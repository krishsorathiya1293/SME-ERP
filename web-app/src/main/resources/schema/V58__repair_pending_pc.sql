-- ============================================================
-- Repair order_items.pending_pc corrupted by failed dispatches.
--
-- Dispatch wrote pending_pc BEFORE the dispatch row itself was saved, and
-- the write survived when the call went on to fail (the unboxing NPE in
-- ClientOrderFulfillmentServiceImpl.describe, fixed alongside this). So
-- every failed attempt walked pending_pc down while nothing was actually
-- dispatched -- leaving lines reading "Qty 200, Pending 0, Dispatched 0",
-- which the sheet shows as fully delivered when nothing has shipped.
--
-- pending_pc is a cache of qty_pc - sum(dispatches), floored at zero, which
-- is exactly what OrderDispatchServiceImpl.pendingAfter computes. Rows are
-- recomputed from the dispatch rows: those are the record of what actually
-- left the building, and the bug never touched them.
--
-- Deliberately narrow -- it cannot alter a line that is already correct:
--   * only rows that actually disagree, so this is a NO-OP on a healthy
--     database and a working dispatch is left exactly as it is
--   * only rows with a qty_pc to compute from
--   * pending_pc IS NULL is left alone: that is the "never dispatched"
--     state the code relies on, not corruption
--   * MERGE TARGETS are skipped. A merged line's pending is seeded from the
--     sum of its source lines' pending, which already accounts for what
--     those lines shipped before the merge, so qty - own dispatches is the
--     wrong formula for them and would erase that history.
--
-- Every value it changes is snapshotted first, so this is reversible:
--
--   UPDATE order_items oi
--   SET pending_pc = b.old_pending_pc
--   FROM pending_pc_repair_backup b
--   WHERE b.order_item_id = oi.id;
--
-- The backup table is left behind on purpose. It is small (one row per line
-- actually repaired, none on a healthy database) and it is the only record
-- of what the corrupted values were.
-- ============================================================

CREATE TABLE IF NOT EXISTS pending_pc_repair_backup
(
    order_item_id  BIGINT PRIMARY KEY,
    old_pending_pc DOUBLE PRECISION,
    new_pending_pc DOUBLE PRECISION,
    qty_pc         DOUBLE PRECISION,
    dispatched_pc  DOUBLE PRECISION,
    repaired_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Snapshot first. Whatever lands here is exactly the set of rows the UPDATE
-- below touches -- nothing else can be affected, because the UPDATE drives
-- off this table rather than re-evaluating the predicate.
INSERT INTO pending_pc_repair_backup
    (order_item_id, old_pending_pc, new_pending_pc, qty_pc, dispatched_pc)
SELECT oi.id,
       oi.pending_pc,
       GREATEST(0, oi.qty_pc - s.sent),
       oi.qty_pc,
       s.sent
FROM order_items oi
         CROSS JOIN LATERAL (
    SELECT COALESCE((SELECT SUM(d.dispatch_pcs)
                     FROM order_dispatch d
                     WHERE d.order_item_id = oi.id), 0) AS sent
    ) s
WHERE oi.pending_pc IS NOT NULL
  AND oi.qty_pc IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM order_items src WHERE src.merged_into_item_id = oi.id)
  AND ABS(oi.pending_pc - GREATEST(0, oi.qty_pc - s.sent)) > 0.001
ON CONFLICT (order_item_id) DO NOTHING;

UPDATE order_items oi
SET pending_pc = b.new_pending_pc
FROM pending_pc_repair_backup b
WHERE b.order_item_id = oi.id
  AND oi.pending_pc IS DISTINCT FROM b.new_pending_pc;
