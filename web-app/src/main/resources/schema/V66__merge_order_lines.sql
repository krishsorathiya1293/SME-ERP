-- ============================================================
-- Merging order LINES, including ones already at the plater
-- or partly dispatched.
--
-- Until now only whole orders still at CREATED could merge, so
-- nothing recorded against a line ever had to move. Now a line
-- with chitthis and dispatches behind it can be folded into a
-- merged line, and those records move with it: from then on the
-- merged line is simply one line, and every screen that reads a
-- line's dispatches and chitthis keeps working unchanged.
--
-- This table is the memory of that move. One row per record
-- that moved, saying which line it came from. It is what lets
-- un-merge put every dispatch and chitthi back where it was, and
-- what lets the client portal keep crediting work done before
-- the merge to the order it was done for.
--
--   kind = DISPATCH    record_id = order_dispatch.id
--   kind = JOB_WORK    record_id = job_works.id       (its primary line moved)
--   kind = ALLOCATION  record_id = job_work_order_items.id on the merged line
--
-- An ALLOCATION row keeps the source line's own share (qty_kg /
-- qty_pc). When one chitthi covered two of the lines being
-- merged, their two shares become one allocation on the merged
-- line (the table allows one per chitthi per line), and two rows
-- here point at it -- which is how un-merge splits it back.
--
-- For DISPATCH and JOB_WORK rows the quantity is the record's
-- own figure at the moment of the merge, so un-merge can tell a
-- record that was edited afterwards and refuse rather than put
-- back something that no longer matches.
-- ============================================================

CREATE TABLE IF NOT EXISTS order_item_merge_moves
(
    id              BIGSERIAL PRIMARY KEY,

    merged_item_id  BIGINT      NOT NULL,
    source_item_id  BIGINT      NOT NULL,
    kind            VARCHAR(20) NOT NULL,
    record_id       BIGINT      NOT NULL,

    qty_kg          DOUBLE PRECISION,
    qty_pc          DOUBLE PRECISION,

    created_at      TIMESTAMP(6),
    last_updated_at TIMESTAMP(6),

    CONSTRAINT fk_oimm_merged_item
        FOREIGN KEY (merged_item_id) REFERENCES order_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_oimm_source_item
        FOREIGN KEY (source_item_id) REFERENCES order_items (id) ON DELETE CASCADE,
    CONSTRAINT ck_oimm_kind CHECK (kind IN ('DISPATCH', 'JOB_WORK', 'ALLOCATION'))
);

CREATE INDEX IF NOT EXISTS ix_oimm_merged_item ON order_item_merge_moves (merged_item_id);
CREATE INDEX IF NOT EXISTS ix_oimm_source_item ON order_item_merge_moves (source_item_id);
