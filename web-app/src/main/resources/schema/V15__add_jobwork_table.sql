-- ============================================================
-- job_works
-- ============================================================

CREATE TABLE IF NOT EXISTS job_works
(
    id              BIGSERIAL PRIMARY KEY,

    order_item_id   BIGINT NOT NULL UNIQUE,
    party_id        BIGINT NOT NULL,
    size_id         BIGINT NOT NULL,

    job_date        DATE,
    qty_pc          DOUBLE PRECISION,
    qty_kg          DOUBLE PRECISION,
    finish          VARCHAR(255),
    element_count   DOUBLE PRECISION,
    element_type    VARCHAR(50),
    status          VARCHAR(50),
    job_work_type   VARCHAR(50),

    created_at      TIMESTAMP(6),
    last_updated_at TIMESTAMP(6),

    CONSTRAINT fk_job_works_order_item
        FOREIGN KEY (order_item_id)
            REFERENCES order_items (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_job_works_party
        FOREIGN KEY (party_id)
            REFERENCES party (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_job_works_size
        FOREIGN KEY (size_id)
            REFERENCES size_inventory (id)
            ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS ix_job_works_order_item_id
    ON job_works (order_item_id);

CREATE INDEX IF NOT EXISTS ix_job_works_party_id
    ON job_works (party_id);

CREATE INDEX IF NOT EXISTS ix_job_works_size_id
    ON job_works (size_id);

CREATE INDEX IF NOT EXISTS ix_job_works_job_date
    ON job_works (job_date);

CREATE TABLE IF NOT EXISTS job_work_returns
(
    id                   BIGSERIAL PRIMARY KEY,

    job_work_id          BIGINT NOT NULL UNIQUE,

    return_kg            DOUBLE PRECISION,
    ghati                DOUBLE PRECISION,
    return_element_count DOUBLE PRECISION,
    element_type         VARCHAR(50),

    created_at           TIMESTAMP(6),
    last_updated_at      TIMESTAMP(6),

    CONSTRAINT fk_job_work_returns_job_work
        FOREIGN KEY (job_work_id)
            REFERENCES job_works (id)
            ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_job_work_returns_job_work_id
    ON job_work_returns (job_work_id);