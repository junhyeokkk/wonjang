CREATE TABLE balance_snapshots (
    account_id   BIGINT       NOT NULL REFERENCES accounts(id),
    as_of_date   DATE         NOT NULL,
    debit_total  BIGINT       NOT NULL,
    credit_total BIGINT       NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (account_id, as_of_date)
);
