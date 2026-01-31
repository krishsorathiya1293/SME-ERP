CREATE TABLE IF NOT EXISTS users
(
    id              BIGSERIAL PRIMARY KEY,
    user_email      VARCHAR(100) UNIQUE NOT NULL,
    password        VARCHAR(255)        NOT NULL,
    user_group      VARCHAR(50)         NOT NULL,
    enabled         BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP(6),
    last_updated_at TIMESTAMP(6)
);
