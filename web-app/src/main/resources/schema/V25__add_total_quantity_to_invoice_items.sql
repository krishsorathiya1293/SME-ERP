ALTER TABLE invoice_items
    ADD COLUMN IF NOT EXISTS total_quantity INTEGER;
