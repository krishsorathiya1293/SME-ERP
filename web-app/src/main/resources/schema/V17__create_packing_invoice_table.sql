-- Packing Invoice header
CREATE TABLE IF NOT EXISTS packing_invoice
(
    id              BIGSERIAL PRIMARY KEY,
    invoice_date    DATE          NOT NULL,
    invoice_no      VARCHAR(10)   NOT NULL,
    party_id        BIGINT        NOT NULL,
    cartoon_no      VARCHAR(10)   NOT NULL,
    created_at      TIMESTAMP(6),
    last_updated_at TIMESTAMP(6),

    CONSTRAINT fk_packing_invoice_party
        FOREIGN KEY (party_id)
            REFERENCES party (id)
            ON DELETE RESTRICT,

    CONSTRAINT uq_packing_invoice_party_date_no
        UNIQUE (party_id, invoice_date, invoice_no)
);

CREATE INDEX IF NOT EXISTS ix_packing_invoice_date
    ON packing_invoice (invoice_date);

CREATE INDEX IF NOT EXISTS ix_packing_invoice_party
    ON packing_invoice (party_id);

-- Packing Invoice line items
CREATE TABLE IF NOT EXISTS packing_invoice_items
(
    id                       BIGSERIAL PRIMARY KEY,
    packing_invoice_id       BIGINT NOT NULL,
    size_id                  BIGINT NOT NULL,
    finish                   VARCHAR(255),
    box                      DOUBLE PRECISION,
    pc                       DOUBLE PRECISION,
    total_pc                 DOUBLE PRECISION,
    scrap                    DOUBLE PRECISION,
    laboure                  DOUBLE PRECISION,
    rs_kg                    DOUBLE PRECISION,
    box_weight               DOUBLE PRECISION,
    box_weight_ac_doc_weight DOUBLE PRECISION,
    bill_cal_doc_weight      DOUBLE PRECISION,
    rate_pc                  DOUBLE PRECISION,
    total_rs                 DOUBLE PRECISION,
    total_kg                 DOUBLE PRECISION,
    as_per_doc_weight        DOUBLE PRECISION,
    loss                     DOUBLE PRECISION,
    created_at               TIMESTAMP(6),
    last_updated_at          TIMESTAMP(6),

    CONSTRAINT fk_packing_invoice_item_invoice
        FOREIGN KEY (packing_invoice_id)
            REFERENCES packing_invoice (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_packing_invoice_item_size
        FOREIGN KEY (size_id)
            REFERENCES size_inventory (id)
            ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS ix_packing_invoice_items_invoice_id
    ON packing_invoice_items (packing_invoice_id);

CREATE INDEX IF NOT EXISTS ix_packing_invoice_items_size_id
    ON packing_invoice_items (size_id);
