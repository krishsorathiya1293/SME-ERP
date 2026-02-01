CREATE TABLE item
(
    id                BIGSERIAL PRIMARY KEY,
    size_inch         VARCHAR(50),
    size_mm           VARCHAR(50),
    category_id       BIGINT,
    sub_category_id   BIGINT,
    item_kg           DOUBLE PRECISION,
    weight_per_pc     DOUBLE PRECISION,
    total_pc          DOUBLE PRECISION,
    low_stock_warning DOUBLE PRECISION,
    dozen_weight      DOUBLE PRECISION,
    stock_status      VARCHAR(50),
    created_at        TIMESTAMP(6),
    last_updated_at   TIMESTAMP(6),

    CONSTRAINT fk_item_category
        FOREIGN KEY (category_id)
            REFERENCES category (id),

    CONSTRAINT fk_item_sub_category
        FOREIGN KEY (sub_category_id)
            REFERENCES sub_category (id)
);
