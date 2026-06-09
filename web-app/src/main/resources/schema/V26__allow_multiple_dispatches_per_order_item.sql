-- Allow multiple dispatches per order item (partial dispatch support)
ALTER TABLE order_dispatch
    DROP CONSTRAINT IF EXISTS order_dispatch_order_item_id_key;

DROP INDEX IF EXISTS order_dispatch_order_item_id_key;
