CREATE TABLE accounts (
    id         BIGSERIAL    PRIMARY KEY,
    code       VARCHAR(10)  NOT NULL,
    name       VARCHAR(50)  NOT NULL,
    type       VARCHAR(10)  NOT NULL CHECK (type IN ('ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE')),
    active     BOOLEAN      NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_accounts_code UNIQUE (code)
);
