CREATE TABLE blacklist (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL UNIQUE,
    blacklisted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    reason TEXT
);


INSERT INTO blacklist(
	 client_id, reason)
	VALUES ( 1, 'Плохо говорил про T1');