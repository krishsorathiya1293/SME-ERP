CREATE TABLE IF NOT EXISTS item_inventory
(
    id              BIGSERIAL PRIMARY KEY,
    item_name       VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP(6),
    last_updated_at TIMESTAMP(6)

);

CREATE UNIQUE INDEX IF NOT EXISTS ux_item_inventory_item_name
    ON item_inventory (LOWER(item_name));

CREATE TABLE IF NOT EXISTS size_inventory
(
    id                     BIGSERIAL PRIMARY KEY,
    item_name_inventory_id BIGINT NOT NULL,
    size_in_inch           VARCHAR(100),
    size_in_mm             VARCHAR(100),
    dozen_weight           DOUBLE PRECISION,
    created_at             TIMESTAMP(6),
    last_updated_at        TIMESTAMP(6),

    CONSTRAINT fk_size_inventory_item
        FOREIGN KEY (item_name_inventory_id)
            REFERENCES item_inventory (id)
            ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_size_inventory_item_size
    ON size_inventory (item_name_inventory_id, COALESCE(size_in_inch, ''), COALESCE(size_in_mm, ''));

CREATE INDEX IF NOT EXISTS ix_size_inventory_item_id
    ON size_inventory (item_name_inventory_id);

-- -------------------------------
CREATE TABLE IF NOT EXISTS inventory
(
    id              BIGSERIAL PRIMARY KEY,
    size_id         BIGINT NOT NULL UNIQUE,
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
    CONSTRAINT fk_inventory_size
        FOREIGN KEY (size_id)
            REFERENCES size_inventory (id)
            ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS ix_inventory_size_id
    ON inventory (size_id);