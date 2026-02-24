-- Performance index cleanup — 23 Feb 2026

-- Drop unnecessary indexes from earlier migrations
DROP INDEX IF EXISTS idx_party_party_type;   -- V3: low cardinality enum
DROP INDEX IF EXISTS idx_item_stock_status;   -- redundant: low cardinality
DROP INDEX IF EXISTS idx_invoices_invoice_type; -- redundant: covered by idx_invoices_id_type (V9)

-- item: FK join columns
CREATE INDEX IF NOT EXISTS idx_item_category_id     ON item (category_id);
CREATE INDEX IF NOT EXISTS idx_item_sub_category_id ON item (sub_category_id);

-- invoices: date range and pagination sort
CREATE INDEX IF NOT EXISTS idx_invoices_invoice_date ON invoices (invoice_date);
CREATE INDEX IF NOT EXISTS idx_invoices_created_at   ON invoices (created_at DESC);

-- party: name search
CREATE INDEX IF NOT EXISTS idx_party_name ON party (name);
