CREATE TABLE vouchers (
    id                   BIGSERIAL     PRIMARY KEY,
    voucher_no           VARCHAR(20)   NOT NULL,
    voucher_date         DATE          NOT NULL,
    description          VARCHAR(200),
    reverses_voucher_id  BIGINT        REFERENCES vouchers(id),
    idempotency_key      VARCHAR(64),
    created_by           VARCHAR(50)   NOT NULL,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_vouchers_no           UNIQUE (voucher_no),
    CONSTRAINT uq_vouchers_reverses     UNIQUE (reverses_voucher_id),
    CONSTRAINT uq_vouchers_idempotency  UNIQUE (idempotency_key)
);

CREATE TABLE entries (
    id          BIGSERIAL    PRIMARY KEY,
    voucher_id  BIGINT       NOT NULL REFERENCES vouchers(id),
    account_id  BIGINT       NOT NULL REFERENCES accounts(id),
    side        CHAR(6)      NOT NULL CHECK (side IN ('DEBIT','CREDIT')),
    amount      BIGINT       NOT NULL CHECK (amount > 0),
    line_no     SMALLINT     NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_entries_line UNIQUE (voucher_id, line_no)
);

CREATE INDEX idx_entries_account_voucher ON entries (account_id, voucher_id);
