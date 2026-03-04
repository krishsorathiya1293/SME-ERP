-- size_id should NOT be globally unique; the same size can appear in multiple orders
ALTER TABLE order_items
    DROP CONSTRAINT IF EXISTS order_items_size_id_key;
