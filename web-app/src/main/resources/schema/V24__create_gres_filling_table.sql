CREATE TABLE sme_erp.gres_fillings
(
    id              BIGSERIAL PRIMARY KEY,
    party_id        BIGINT      NOT NULL REFERENCES sme_erp.party (id),
    chitthi_no      VARCHAR(100),
    chitthi_date    DATE,
    order_time      VARCHAR(10),
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP,
    last_updated_at TIMESTAMP
);

CREATE TABLE sme_erp.gres_filling_items
(
    id              BIGSERIAL PRIMARY KEY,
    gres_filling_id BIGINT NOT NULL REFERENCES sme_erp.gres_fillings (id) ON DELETE CASCADE,
    size_id         BIGINT REFERENCES sme_erp.size_inventory (id),
    unit_kg         DOUBLE PRECISION,
    unit_type       VARCHAR(20),
    element_count   DOUBLE PRECISION,
    element_type    VARCHAR(20),
    net_weight      DOUBLE PRECISION,
    rate_per_kg     DOUBLE PRECISION,
    total_amount    DOUBLE PRECISION
);

CREATE TABLE sme_erp.gres_filling_returns
(
    id                   BIGSERIAL PRIMARY KEY,
    gres_filling_id      BIGINT NOT NULL REFERENCES sme_erp.gres_fillings (id) ON DELETE CASCADE,
    return_kg            DOUBLE PRECISION,
    ghati                DOUBLE PRECISION,
    return_element_count DOUBLE PRECISION,
    element_type         VARCHAR(20),
    return_date          DATE,
    created_at           TIMESTAMP,
    last_updated_at      TIMESTAMP
);

CREATE INDEX idx_gres_filling_returns_gres_id ON sme_erp.gres_filling_returns (gres_filling_id);
CREATE INDEX idx_gres_filling_items_gres_id ON sme_erp.gres_filling_items (gres_filling_id);
