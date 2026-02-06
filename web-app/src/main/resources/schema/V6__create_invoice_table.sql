-- =========================
-- TABLE: invoices
-- =========================
CREATE TABLE if not exists invoices
(
    id                           BIGSERIAL PRIMARY KEY,

    -- Exporter
    exporter_company_name        VARCHAR(255),
    exporter_contact_no          VARCHAR(50),
    exporter_address             TEXT,

    -- Importer (Bill To)
    bill_to_country              VARCHAR(100),
    bill_to_name                 VARCHAR(255),
    bill_to_contact_no           VARCHAR(50),
    bill_to_address              TEXT,
    currency                     VARCHAR(255),

    -- Importer (Ship To)
    ship_to_country              VARCHAR(100),
    ship_to_name                 VARCHAR(255),
    ship_to_contact_no           VARCHAR(50),
    ship_to_address              TEXT,

    -- Invoice Details
    invoice_no                   VARCHAR(100),
    invoice_date                 DATE,
    gst_no                       VARCHAR(100),
    iec_code                     VARCHAR(100),
    po_no                        VARCHAR(100),
    incoterms                    VARCHAR(50),
    payment_terms                VARCHAR(255),
    pre_carriage                 VARCHAR(255),
    country_of_origin            VARCHAR(100),
    country_of_final_destination VARCHAR(100),
    port_of_loading              VARCHAR(100),
    port_of_discharge            VARCHAR(100),
    invoice_type                 VARCHAR(50),

    -- Bank Details
    beneficiary_name             VARCHAR(255),
    bank_name                    VARCHAR(255),
    branch                       VARCHAR(255),
    account_no                   VARCHAR(100),
    swift_code                   VARCHAR(100),

    -- Cost Details
    freight_cost                 DOUBLE PRECISION,
    insurance_cost               DOUBLE PRECISION,
    other_cost                   DOUBLE PRECISION,

    -- Export Declarations
    arn_no                       VARCHAR(100),
    rod_tep                      VARCHAR(100),
    rex_no                       VARCHAR(100),
    created_at                   TIMESTAMP(6),
    last_updated_at              TIMESTAMP(6)
);

-- =========================
-- TABLE: invoice_items
-- =========================
CREATE TABLE if not exists invoice_items
(
    id                     BIGSERIAL PRIMARY KEY,

    item_no                VARCHAR(100),
    description            TEXT,
    hs_code                VARCHAR(100),
    quantity               INTEGER,
    unit_price_usd         DOUBLE PRECISION,
    currency_current_price DOUBLE PRECISION,

    invoice_id             BIGINT NOT NULL,

    CONSTRAINT fk_invoice_items_invoice
        FOREIGN KEY (invoice_id)
            REFERENCES invoices (id)
            ON DELETE CASCADE
);

-- =========================
-- TABLE: packing_details
-- =========================
CREATE TABLE if not exists invoice_packing_details
(
    id              BIGSERIAL PRIMARY KEY,

    item_no         VARCHAR(100),
    description     TEXT,
    total_qty       INTEGER,
    qty_per_carton  INTEGER,
    no_of_cartons   INTEGER,
    gross_weight_kg DOUBLE PRECISION,
    net_weight_kg   DOUBLE PRECISION,
    wooden_pallets  INTEGER,

    invoice_id      BIGINT NOT NULL,

    CONSTRAINT fk_packing_details_invoice
        FOREIGN KEY (invoice_id)
            REFERENCES invoices (id)
            ON DELETE CASCADE
);

-- =========================
-- INDEXES (recommended)
-- =========================
CREATE INDEX if not exists idx_invoices_invoice_no ON invoices (invoice_no);
CREATE INDEX if not exists idx_invoice_items_invoice_id ON invoice_items (invoice_id);
CREATE INDEX if not exists idx_packing_details_invoice_id ON invoice_packing_details (invoice_id);
