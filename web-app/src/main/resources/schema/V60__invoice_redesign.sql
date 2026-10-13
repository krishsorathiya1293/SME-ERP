-- Invoice redesign (client sheet "Requirement of Invoice Design"): LUT toggle, invoice-level
-- currency + exchange rate, IGST, place of pre-carriage, PO date, signature date, a separate
-- Bill To / Ship To for the commercial invoice, and a unit per item line.
ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS lut_enabled                 BOOLEAN,
    ADD COLUMN IF NOT EXISTS place_of_pre_carriage       TEXT,
    ADD COLUMN IF NOT EXISTS po_date                     DATE,
    ADD COLUMN IF NOT EXISTS currency                    VARCHAR(20),
    ADD COLUMN IF NOT EXISTS exchange_rate               DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS igst_enabled                BOOLEAN,
    ADD COLUMN IF NOT EXISTS sign_date                   DATE,
    ADD COLUMN IF NOT EXISTS commercial_bill_to_country  TEXT,
    ADD COLUMN IF NOT EXISTS commercial_bill_to_name     TEXT,
    ADD COLUMN IF NOT EXISTS commercial_bill_to_contact_no TEXT,
    ADD COLUMN IF NOT EXISTS commercial_bill_to_address  TEXT,
    ADD COLUMN IF NOT EXISTS commercial_ship_to_country  TEXT,
    ADD COLUMN IF NOT EXISTS commercial_ship_to_name     TEXT,
    ADD COLUMN IF NOT EXISTS commercial_ship_to_contact_no TEXT,
    ADD COLUMN IF NOT EXISTS commercial_ship_to_address  TEXT,
    ADD COLUMN IF NOT EXISTS group_id                    BIGINT;

-- Free-text fields were sized for short codes; the new defaults (multi-line exporter phone,
-- "To The Order - <country>", port names) can run longer.
ALTER TABLE invoices
    ALTER COLUMN exporter_company_name TYPE TEXT,
    ALTER COLUMN exporter_contact_no TYPE TEXT,
    ALTER COLUMN bill_to_country TYPE TEXT,
    ALTER COLUMN ship_to_country TYPE TEXT,
    ALTER COLUMN incoterms TYPE TEXT,
    ALTER COLUMN payment_terms TYPE TEXT,
    ALTER COLUMN pre_carriage TYPE TEXT,
    ALTER COLUMN country_of_origin TYPE TEXT,
    ALTER COLUMN country_of_final_destination TYPE TEXT,
    ALTER COLUMN port_of_loading TYPE TEXT,
    ALTER COLUMN port_of_discharge TYPE TEXT,
    ALTER COLUMN po_no TYPE TEXT;

ALTER TABLE invoice_items
    ADD COLUMN IF NOT EXISTS unit VARCHAR(30);

-- One save writes three rows (export / commercial / packing list). Tie existing triples together
-- so an edit and a PDF download reach all three: rows with the same invoice number written within
-- a few seconds of each other belong to one save.
UPDATE invoices i
SET group_id = (SELECT MIN(j.id)
                FROM invoices j
                WHERE j.invoice_no IS NOT DISTINCT FROM i.invoice_no
                  AND ABS(EXTRACT(EPOCH FROM (j.created_at - i.created_at))) < 5)
WHERE i.group_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_invoices_group_id ON invoices (group_id);
