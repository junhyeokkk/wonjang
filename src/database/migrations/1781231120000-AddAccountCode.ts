import { MigrationInterface, QueryRunner } from 'typeorm';

/**
 * accounts에 기계용 업무 식별자 `code` 컬럼을 추가한다.
 * name(표시용 한글 이름)과 분리한다. (근거: docs/adr/002-account-type-representation.md)
 *
 * InitLedgerSchema 직후 빈 테이블에 적용되므로 NOT NULL + UNIQUE를 바로 건다.
 */
export class AddAccountCode1781231120000 implements MigrationInterface {
  public async up(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`
            ALTER TABLE accounts
                ADD COLUMN code VARCHAR(50) NOT NULL UNIQUE;
        `);
  }

  public async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`
            ALTER TABLE accounts DROP COLUMN code;
        `);
  }
}
