import { MigrationInterface, QueryRunner } from 'typeorm';

/**
 * 절대 원칙 #1 "합 = 0 강제"를 DB 레벨에서 보증한다.
 *
 * 거래(transaction) 단위로 entries의 amount 합이 0이 아니면 거부한다.
 * - DEFERRABLE INITIALLY DEFERRED: 한 거래의 entries를 여러 INSERT로 나눠 넣어도
 *   개별 INSERT 시점이 아니라 **커밋 시점**에 합을 검증한다. 따라서 정상적인
 *   복식부기 기입(예: 차변 1줄 + 대변 1줄)을 한 트랜잭션 안에서 넣는 동안에는
 *   중간 불균형이 허용되고, 최종 합만 0이면 통과한다.
 * - entries는 append-only이므로 INSERT만 감시한다. (UPDATE/DELETE는
 *   prevent_entry_mutation 트리거가 이미 차단한다.)
 * - amount <> 0 CHECK와 결합하면, 합이 0이 되려면 최소 2줄 이상이 필요하다.
 */
export class EnforceBalancedTransaction1781231130000 implements MigrationInterface {
  public async up(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`
            CREATE FUNCTION assert_transaction_balanced() RETURNS trigger AS $$
            DECLARE
                total BIGINT;
            BEGIN
                SELECT COALESCE(SUM(amount), 0) INTO total
                FROM entries
                WHERE transaction_id = NEW.transaction_id;

                IF total <> 0 THEN
                    RAISE EXCEPTION
                        'transaction % is not balanced: sum of entries = %',
                        NEW.transaction_id, total
                        USING ERRCODE = 'check_violation';
                END IF;

                RETURN NULL;
            END;
            $$ LANGUAGE plpgsql;

            CREATE CONSTRAINT TRIGGER trg_entries_balanced
                AFTER INSERT ON entries
                DEFERRABLE INITIALLY DEFERRED
                FOR EACH ROW EXECUTE FUNCTION assert_transaction_balanced();
        `);
  }

  public async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`
            DROP TRIGGER IF EXISTS trg_entries_balanced ON entries;
            DROP FUNCTION IF EXISTS assert_transaction_balanced;
        `);
  }
}
