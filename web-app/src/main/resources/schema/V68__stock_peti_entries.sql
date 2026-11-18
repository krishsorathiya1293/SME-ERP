-- Stock by peti (crate). The client weighs full petis on their machine: the kg typed in
-- includes the petis, so petis x empty peti weight is taken off to get the net kg, and
-- pcs = net kg / weight per pc, as before.
ALTER TABLE item
    ADD COLUMN IF NOT EXISTS peti_count        INTEGER,
    ADD COLUMN IF NOT EXISTS empty_peti_weight DOUBLE PRECISION;

-- Every stock change, dated. ADD puts more on top of what is there ("+ 4 peti"), SET
-- replaces it (a recount). The *_after columns are the stock right after the entry.
CREATE TABLE IF NOT EXISTS stock_entry
(
    id                BIGSERIAL PRIMARY KEY,
    item_id           BIGINT           NOT NULL REFERENCES item (id) ON DELETE CASCADE,
    entry_type        VARCHAR(10)      NOT NULL,
    entry_date        DATE             NOT NULL,
    peti_count        INTEGER,
    gross_kg          DOUBLE PRECISION,
    empty_peti_weight DOUBLE PRECISION,
    net_kg            DOUBLE PRECISION NOT NULL,
    weight_per_pc     DOUBLE PRECISION,
    pcs               DOUBLE PRECISION NOT NULL,
    total_peti_after  INTEGER,
    total_kg_after    DOUBLE PRECISION NOT NULL,
    total_pc_after    DOUBLE PRECISION NOT NULL,
    note              VARCHAR(255),
    created_at        TIMESTAMP(6),
    last_updated_at   TIMESTAMP(6)
);
CREATE INDEX IF NOT EXISTS ix_stock_entry_item ON stock_entry (item_id, entry_date);
