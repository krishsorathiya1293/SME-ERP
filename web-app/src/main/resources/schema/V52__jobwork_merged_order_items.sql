-- ============================================================
-- Merged job work: one chitthi covering several order lines.
--
-- Two orders for the same party, item, size and finish go to the
-- plater as one physical batch, so they should be one chitthi of
-- 300 Kg rather than two of 200 and 100. The job work keeps its
-- primary order_item_id (unchanged for every existing row), and
-- this table records how that single chitthi's weight is split
-- back across the lines it covers.
--
-- The split matters: without it the primary line would look like
-- it sent 300 Kg it never ordered, and the other line would look
-- untouched -- which is exactly what the client portal reports on.
-- ============================================================

CREATE TABLE IF NOT EXISTS job_work_order_items
(
    id              BIGSERIAL PRIMARY KEY,

    job_work_id     BIGINT NOT NULL,
    order_item_id   BIGINT NOT NULL,

    -- This line's share of the chitthi. Sums to the job work's qty_kg / qty_pc.
    qty_kg          DOUBLE PRECISION,
    qty_pc          DOUBLE PRECISION,

    created_at      TIMESTAMP(6),
    last_updated_at TIMESTAMP(6),

    CONSTRAINT fk_jwoi_job_work
        FOREIGN KEY (job_work_id) REFERENCES job_works (id) ON DELETE CASCADE,
    CONSTRAINT fk_jwoi_order_item
        FOREIGN KEY (order_item_id) REFERENCES order_items (id) ON DELETE CASCADE,
    CONSTRAINT uq_jwoi_job_work_order_item UNIQUE (job_work_id, order_item_id)
);

CREATE INDEX IF NOT EXISTS ix_jwoi_order_item ON job_work_order_items (order_item_id);
