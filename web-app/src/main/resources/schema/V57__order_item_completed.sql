-- ============================================================
-- The "done with it" tick moves from the ORDER to the LINE.
--
-- One order routinely carries five items that finish at five different
-- times: two are settled and off the works' mind while the other three
-- are still with the plater. An order-level tick could only say "all of
-- it" or "none of it", so ticking the two finished ones took the other
-- three off the sheet with them -- and the only way to keep the three
-- visible was to leave the two showing as outstanding.
--
-- orders.completed is kept, and keeps meaning "every line of it", so the
-- existing order-level endpoint still works and nothing that already
-- reads it breaks. The sheet reads the line from here on.
--
-- Backfill: a line inherits its order's tick, so an order completed
-- before this migration stays completed line for line.
-- ============================================================

ALTER TABLE order_items ADD COLUMN IF NOT EXISTS completed BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE order_items oi
SET completed = TRUE
FROM orders o
WHERE o.id = oi.order_id
  AND o.completed;

-- The sheet asks for "not completed" on every load.
CREATE INDEX IF NOT EXISTS ix_order_items_completed ON order_items (completed);
