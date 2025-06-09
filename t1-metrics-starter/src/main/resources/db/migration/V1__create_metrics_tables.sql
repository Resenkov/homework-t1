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