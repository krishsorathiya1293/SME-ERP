-- -------------------------------
-- CLIENT INVENTORY
-- -------------------------------

CREATE TABLE IF NOT EXISTS client_inventory
(
    id              BIGSERIAL PRIMARY KEY,

    client_id       BIGINT NOT NULL,
    size_id         BIGINT NOT NULL,

    pcs_per_box     INTEGER,
    box_per_carton  INTEGER,
    pcs_per_carton  INTEGER,
    carton_weight   DOUBLE PRECISION,

    sssatinlacq     DOUBLE PRECISION,
    antiq           DOUBLE PRECISION,
    sidegold        DOUBLE PRECISION,
    zblack          DOUBLE PRECISION,
    grblack         DOUBLE PRECISION,
    mattss          DOUBLE PRECISION,
    mattantiq       DOUBLE PRECISION,
    pvdrose         DOUBLE PRECISION,
    pvdgold         DOUBLE PRECISION,
    pvdblack        DOUBLE PRECISION,
    rosegold        DOUBLE PRECISION,
    clearlacq       DOUBLE PRECISION,

    created_at      TIMESTAMP(6),
    last_updated_at TIMESTAMP(6),

    CONSTRAINT fk_client_inventory_client
        FOREIGN KEY (client_id)
            REFERENCES party (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_client_inventory_size
        FOREIGN KEY (size_id)
            REFERENCES size_inventory (id)
            ON DELETE CASCADE
);

-- -------------------------------
-- UNIQUE CONSTRAINT
-- One inventory row per client per size
-- -------------------------------

CREATE UNIQUE INDEX IF NOT EXISTS ux_client_inventory_client_size
    ON client_inventory (client_id, size_id);

-- -------------------------------
-- INDEXES
-- -------------------------------

CREATE INDEX IF NOT EXISTS ix_client_inventory_client_id
    ON client_inventory (client_id);

CREATE INDEX IF NOT EXISTS ix_client_inventory_size_id
    ON client_inventory (size_id);