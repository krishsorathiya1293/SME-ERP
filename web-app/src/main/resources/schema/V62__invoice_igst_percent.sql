-- IGST on the commercial invoice is now a chosen rate (default 18%) instead of a fixed 18%.
ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS igst_percent DOUBLE PRECISION;

UPDATE invoices SET igst_percent = 18 WHERE igst_enabled AND igst_percent IS NULL;
