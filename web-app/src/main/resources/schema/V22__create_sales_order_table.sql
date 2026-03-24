-- ============================================================
-- sales_orders (header)
-- ============================================================

CREATE TABLE IF NOT EXISTS sales_orders
(
    id                      BIGSERIAL PRIMARY KEY,
    party_id                BIGINT       NOT NULL,
    customer_chitthi_no     VARCHAR(255),
    customer_chitthi_date   DATE,
    sales_no                VARCHAR(50),
    order_date              DATE         NOT NULL,
    order_time              VARCHAR(10),
    created_at              TIMESTAMP(6),
    last_updated_at         TIMESTAMP(6),

    CONSTRAINT fk_sales_orders_party
        FOREIGN KEY (party_id)
            REFERENCES party (id)
            ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS ix_sales_orders_party_id
    ON sales_orders (party_id);

CREATE INDEX IF NOT EXISTS ix_sales_orders_order_date
    ON sales_orders (order_date);

-- ============================================================
-- sales_order_items (line items)
-- ============================================================

CREATE TABLE IF NOT EXISTS sales_order_items
(
    id                  BIGSERIAL PRIMARY KEY,
    sales_order_id      BIGINT NOT NULL,
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

    CONSTRAINT fk_sales_order_items_order
        FOREIGN KEY (sales_order_id)
            REFERENCES sales_orders (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_sales_order_items_size
        FOREIGN KEY (size_id)
            REFERENCES size_inventory (id)
            ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS ix_sales_order_items_order_id
    ON sales_order_items (sales_order_id);

CREATE INDEX IF NOT EXISTS ix_sales_order_items_size_id
    ON sales_order_items (size_id);
