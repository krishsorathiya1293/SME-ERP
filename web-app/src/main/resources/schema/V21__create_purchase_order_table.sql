-- ============================================================
-- purchase_orders (header)
-- ============================================================

CREATE TABLE IF NOT EXISTS purchase_orders
(
    id                   BIGSERIAL PRIMARY KEY,
    party_id             BIGINT       NOT NULL,
    seller_chitthi_no    VARCHAR(255),
    seller_chitthi_date  DATE,
    purchase_no          VARCHAR(50),
    order_date           DATE         NOT NULL,
    order_time           VARCHAR(10),
    created_at           TIMESTAMP(6),
    last_updated_at      TIMESTAMP(6),

    CONSTRAINT fk_purchase_orders_party
        FOREIGN KEY (party_id)
            REFERENCES party (id)
            ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_purchase_orders_party_id
    ON purchase_orders (party_id);

CREATE INDEX IF NOT EXISTS ix_purchase_orders_order_date
    ON purchase_orders (order_date);

-- ============================================================
-- purchase_order_items (line items)
-- ============================================================

CREATE TABLE IF NOT EXISTS purchase_order_items
(
    id                  BIGSERIAL PRIMARY KEY,
    purchase_order_id   BIGINT NOT NULL,
    size_id             BIGINT,
    unit_kg             DOUBLE PRECISION,
    unit_type           VARCHAR(50),
    element_count       DOUBLE PRECISION,
    element_type        VARCHAR(50),
    scrap               DOUBLE PRECISION,
    labour              DOUBLE PRECISION,
    price               DOUBLE PRECISION,
    total_price         DOUBLE PRECISION,
    javak_kg_pc         DOUBLE PRECISION,
    javak_rs            DOUBLE PRECISION,
    javak_total_rs      DOUBLE PRECISION,
    created_at          TIMESTAMP(6),
    last_updated_at     TIMESTAMP(6),

    CONSTRAINT fk_purchase_order_items_order
        FOREIGN KEY (purchase_order_id)
            REFERENCES purchase_orders (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_purchase_order_items_size
        FOREIGN KEY (size_id)
            REFERENCES size_inventory (id)
            ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_purchase_order_items_order_id
    ON purchase_order_items (purchase_order_id);

CREATE INDEX IF NOT EXISTS ix_purchase_order_items_size_id
    ON purchase_order_items (size_id);
