ALTER TABLE users
    ADD COLUMN username        VARCHAR(100),
    ADD COLUMN initial_password VARCHAR(500),
    ADD COLUMN party_id        BIGINT;

UPDATE users
SET username = user_email
WHERE username IS NULL;

ALTER TABLE users
    ALTER COLUMN username SET NOT NULL,
    ALTER COLUMN user_email DROP NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT uq_users_username UNIQUE (username),
    ADD CONSTRAINT uq_users_party_id UNIQUE (party_id),
    ADD CONSTRAINT fk_users_party FOREIGN KEY (party_id) REFERENCES party (id);
