CREATE TABLE accounts (
    id              BIGSERIAL       PRIMARY KEY,
    account_number  VARCHAR(50)     NOT NULL UNIQUE,
    owner_name      VARCHAR(255)    NOT NULL,
    balance         NUMERIC(19, 4)  NOT NULL DEFAULT 0,
    currency        VARCHAR(3)      NOT NULL DEFAULT 'SGD',
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE TABLE transactions (
    id                      BIGSERIAL       PRIMARY KEY,
    sender_id               VARCHAR(255)    NOT NULL,
    receiver_id             VARCHAR(255)    NOT NULL,
    amount                  NUMERIC(19, 4)  NOT NULL,
    currency                VARCHAR(3)      NOT NULL DEFAULT 'SGD',
    type                    VARCHAR(20)     NOT NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    risk_level              VARCHAR(20),
    risk_score              DOUBLE PRECISION,
    description             TEXT,
    source_account_id       BIGINT          REFERENCES accounts(id),
    destination_account_id  BIGINT          REFERENCES accounts(id),
    created_at              TIMESTAMP       NOT NULL DEFAULT now(),
    processed_at            TIMESTAMP
);

CREATE INDEX idx_transactions_source_account ON transactions(source_account_id);
CREATE INDEX idx_transactions_dest_account   ON transactions(destination_account_id);
CREATE INDEX idx_transactions_status         ON transactions(status);
CREATE INDEX idx_transactions_created_at     ON transactions(created_at);
CREATE INDEX idx_transactions_sender_id      ON transactions(sender_id);
