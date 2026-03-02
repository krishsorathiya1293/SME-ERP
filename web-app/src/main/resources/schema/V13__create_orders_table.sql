ALTER TABLE inventory
    DROP CONSTRAINT IF EXISTS inventory_size_id_key;

DROP INDEX IF EXISTS inventory_size_id_key;
CREATE TABLE IF NOT EXISTS orders
(
    id              BIGSERIAL PRIMARY KEY,
    party_id        BIGINT NOT NULL,
    order_date      DATE,

    created_at      TIMESTAMP(6),
    last_updated_at TIMESTAMP(6),

    CONSTRAINT fk_orders_party
        FOREIGN KEY (party_id)
            REFERENCES party (id)
            ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS ix_orders_party_id
    ON orders (party_id);

CREATE INDEX IF NOT EXISTS ix_orders_order_date
    ON orders (order_date);

-- ---------- order_items ----------
CREATE TABLE IF NOT EXISTS order_items
(
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT NOT NULL,
    size_id         BIGINT NOT NULL UNIQUE,
    plating         VARCHAR(255),
    qty_pc          DOUBLE PRECISION,
    qty_kg          DOUBLE PRECISION,
    pc_per_box      DOUBLE PRECISION,
    box_per_cartoon DOUBLE PRECISION,
    pc_per_cartoon  DOUBLE PRECISION,
    sticker_qty     DOUBLE PRECISION,
    pending_pc      DOUBLE PRECISION,
    job_action_done BOOLEAN,
    plating_type    VARCHAR(50),
    job_work_no     DOUBLE PRECISION,
    created_at      TIMESTAMP(6),
    last_updated_at TIMESTAMP(6),
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id)
            REFERENCES orders (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_order_items_size
        FOREIGN KEY (size_id)
            REFERENCES size_inventory (id)
            ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS ix_order_items_order_id
    ON order_items (order_id);

CREATE INDEX IF NOT EXISTS ix_order_items_size_id
    ON order_items (size_id);

-- ---------- order_dispatch ----------
CREATE TABLE IF NOT EXISTS order_dispatch
(
    id              BIGSERIAL PRIMARY KEY,
    order_item_id   BIGINT NOT NULL UNIQUE,
    dispatch_date   DATE,
    dispatch_pcs    DOUBLE PRECISION,

    created_at      TIMESTAMP(6),
    last_updated_at TIMESTAMP(6),

    CONSTRAINT fk_order_dispatch_order_item
        FOREIGN KEY (order_item_id)
            REFERENCES order_items (id)
            ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_order_dispatch_order_item_id
    ON order_dispatch (order_item_id);