DROP TABLE IF EXISTS invoice_packing_details;
ALTER TABLE invoices
    DROP COLUMN IF EXISTS currency;
DO
$$
    BEGIN
        IF EXISTS (SELECT 1
                   FROM information_schema.columns
                   WHERE table_name = 'invoice_items'
                     AND column_name = 'quantity')
            AND NOT EXISTS (SELECT 1
                            FROM information_schema.columns
                            WHERE table_name = 'invoice_items'
                              AND column_name = 'total_quantity') THEN
            ALTER TABLE invoice_items
                RENAME COLUMN quantity TO total_quantity;
        END IF;
    END
$$;
ALTER TABLE invoice_items
    ADD COLUMN IF NOT EXISTS part_no         VARCHAR(100),
    ADD COLUMN IF NOT EXISTS inr_price       DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS currency        VARCHAR(50),
    ADD COLUMN IF NOT EXISTS qty_per_carton  INTEGER,
    ADD COLUMN IF NOT EXISTS no_of_cartons   INTEGER,
    ADD COLUMN IF NOT EXISTS gross_weight_kg DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS net_weight_kg   DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS wooden_pallets  INTEGER;
DO
$$
    BEGIN
        IF EXISTS (SELECT 1
                   FROM information_schema.columns
                   WHERE table_name = 'invoices'
                     AND column_name = 'arn_no'
                     AND data_type <> 'text') THEN
            ALTER TABLE invoices
                ALTER COLUMN arn_no TYPE TEXT;
        END IF;

        IF EXISTS (SELECT 1
                   FROM information_schema.columns
                   WHERE table_name = 'invoices'
                     AND column_name = 'rod_tep'
                     AND data_type <> 'text') THEN
            ALTER TABLE invoices
                ALTER COLUMN rod_tep TYPE TEXT;
        END IF;

        IF EXISTS (SELECT 1
                   FROM information_schema.columns
                   WHERE table_name = 'invoices'
                     AND column_name = 'rex_no'
                     AND data_type <> 'text') THEN
            ALTER TABLE invoices
                ALTER COLUMN rex_no TYPE TEXT;
        END IF;
    END
$$;


