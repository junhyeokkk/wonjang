import { MigrationInterface, QueryRunner } from "typeorm";

export class InitLedgerSchema1781231113103 implements MigrationInterface {

    public async up(queryRunner: QueryRunner): Promise<void> {
        await queryRunner.query(`
            CREATE TABLE accounts (
                id          BIGSERIAL PRIMARY KEY,
                name        VARCHAR(100) NOT NULL UNIQUE,
                type        VARCHAR(20)  NOT NULL,
                currency    CHAR(3)      NOT NULL DEFAULT 'KRW',
                created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
            );

            CREATE TABLE transactions (
                id          BIGSERIAL PRIMARY KEY,
                type        VARCHAR(30)  NOT NULL,
                description TEXT,
                occurred_at TIMESTAMPTZ  NOT NULL,
                created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
            );

            CREATE TABLE entries (
                id             BIGSERIAL PRIMARY KEY,
                transaction_id BIGINT NOT NULL REFERENCES transactions(id),
                account_id     BIGINT NOT NULL REFERENCES accounts(id),
                amount         BIGINT NOT NULL CHECK (amount <> 0),
                created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
            );

            CREATE INDEX idx_entries_account ON entries(account_id, id);

            CREATE FUNCTION prevent_entry_mutation() RETURNS trigger AS $$
            BEGIN
                RAISE EXCEPTION 'entries are append-only: use a reversal transaction instead';
            END;
            $$ LANGUAGE plpgsql;

            CREATE TRIGGER trg_entries_append_only
                BEFORE UPDATE OR DELETE ON entries
                FOR EACH ROW EXECUTE FUNCTION prevent_entry_mutation();
         `);
    }

    public async down(queryRunner: QueryRunner): Promise<void> {
        await queryRunner.query(`
            DROP TRIGGER IF EXISTS trg_entries_append_only ON entries;
            DROP FUNCTION IF EXISTS prevent_entry_mutation;
            DROP TABLE IF EXISTS entries;
            DROP TABLE IF EXISTS transactions;
            DROP TABLE IF EXISTS accounts;
        `);
    }
}