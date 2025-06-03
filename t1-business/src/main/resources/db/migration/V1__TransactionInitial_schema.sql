CREATE TYPE client_status AS ENUM (
    'ACTIVE',
    'BLOCKED'
);

CREATE TYPE account_status AS ENUM (
    'ARRESTED',
    'BLOCKED',
    'CLOSED',
    'OPEN'
);
CREATE TYPE account_balance_type AS ENUM (
    'DEBIT',
    'CREDIT'
);
CREATE TYPE transaction_status AS ENUM (
    'ACCEPTED',
    'REJECTED',
    'BLOCKED',
    'CANCELLED',
    'REQUESTED'
);

CREATE TABLE client (
    id BIGSERIAL PRIMARY KEY,
    last_name VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    middle_name VARCHAR(255) NOT NULL,
    client_id BIGINT,
    status client_status NOT NULL
);

CREATE TABLE account (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    status account_status NOT NULL,
    account_id BIGINT,
    balance_type account_balance_type,
    balance NUMERIC(19, 2),
    frozen_amount NUMERIC(19, 2),
    CONSTRAINT fk_account_client FOREIGN KEY (client_id) REFERENCES client(id)
);

CREATE TABLE transaction (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL DEFAULT nextval('transaction_id_seq'),
    account_id BIGINT NOT NULL,
    status transaction_status NOT NULL,
    sum NUMERIC(19, 2),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_transaction_account FOREIGN KEY (account_id) REFERENCES account(id)
);

CREATE TABLE data_source_error_log (
    id BIGSERIAL PRIMARY KEY,
    stack_trace TEXT,
    signature_method VARCHAR(255),
    message VARCHAR(255),
    timestamp TIMESTAMP
);

CREATE TABLE time_limit_log (
    id BIGSERIAL PRIMARY KEY,
    method_name VARCHAR(255),
    duration BIGINT,
    timestamp TIMESTAMP
);

CREATE INDEX idx_account_client_id ON account(client_id);
CREATE INDEX idx_transaction_account_id ON transaction(account_id);
CREATE INDEX idx_transaction_status ON transaction(status);
CREATE INDEX idx_transaction_created_at ON transaction(created_at);

UPDATE client SET client_id = id;
UPDATE account SET account_id = id;