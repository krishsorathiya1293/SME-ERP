-- Connect category to item_inventory (blueprint)
ALTER TABLE item_inventory ADD COLUMN category_id BIGINT;
ALTER TABLE item_inventory ADD CONSTRAINT fk_item_inventory_category
    FOREIGN KEY (category_id) REFERENCES category (id);

-- Remodel item table: remove old size/category fields, add size_id FK to size_inventory
ALTER TABLE item DROP COLUMN IF EXISTS size_inch;
ALTER TABLE item DROP COLUMN IF EXISTS size_mm;
ALTER TABLE item DROP COLUMN IF EXISTS dozen_weight;
ALTER TABLE item DROP COLUMN IF EXISTS category_id;
ALTER TABLE item DROP COLUMN IF EXISTS sub_category_id;
ALTER TABLE item ADD COLUMN size_id BIGINT UNIQUE;
ALTER TABLE item ADD CONSTRAINT fk_item_size
    FOREIGN KEY (size_id) REFERENCES size_inventory (id) ON DELETE RESTRICT;

-- Drop sub_category table (no longer used)
DROP TABLE IF EXISTS sub_category;
