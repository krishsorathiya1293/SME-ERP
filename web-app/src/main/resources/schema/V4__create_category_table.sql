CREATE TABLE category
(
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL UNIQUE,
    created_at      TIMESTAMP(6),
    last_updated_at TIMESTAMP(6)
);
CREATE TABLE sub_category
(
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL UNIQUE,
    category_id     BIGINT       NOT NULL,
    created_at      TIMESTAMP(6),
    last_updated_at TIMESTAMP(6),

    CONSTRAINT fk_sub_category_category
        FOREIGN KEY (category_id)
            REFERENCES category (id)
            ON DELETE CASCADE
);
CREATE INDEX idx_sub_category_category_id
    ON sub_category (category_id);
