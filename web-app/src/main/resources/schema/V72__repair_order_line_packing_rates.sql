-- ============================================================
-- Repair order lines that hold box COUNTS where packing RATES belong.
--
-- order_items.pc_per_box and box_per_cartoon are rates: pieces per box and
-- boxes per carton. Job work reads them that way (Sticker Qty = pieces /
-- pc_per_box, Total Carton = stickers / box_per_cartoon), so do the order
-- sheet, the merge check and the client-portal approval, which writes rates.
--
-- The order form did not. It worked out how many boxes and cartons the
-- quantity needs and saved THOSE under the same two names. A 600-piece line
-- packed 10 to a box, 10 boxes to a carton was stored as 60 / 6 instead of
-- 10 / 10, and a job work for 300 of those pieces then came out as 5
-- stickers and 0.83 carton where it should be 30 and 3. The form is fixed
-- alongside this; this corrects the lines it had already written, and the
-- job works that were worked out from them.
--
-- Deliberately narrow -- it cannot alter a line that already holds rates:
--   * only lines whose sticker_qty equals pc_per_box. The order form set one
--     sticker per box, so on its lines the two are the same number; on a
--     line that holds a rate they are not (stickers = pieces / rate).
--   * the rate must be recoverable from the line itself: pc_per_cartoon was
--     always saved as a rate, so the pieces-per-box rate has to divide it,
--     has to give back exactly the stored box count for the line's quantity,
--     and the boxes-per-carton it implies has to give back exactly the
--     stored carton count. Exactly ONE such rate must exist, or the line is
--     left alone.
--   * a line with no pc_per_cartoon falls back to the rate on file today
--     (the party's own packing, else the item master), under the same
--     "gives back exactly the stored counts" test.
--   * a merged line is corrected only when it still carries, unchanged, the
--     figures copied from a source line that is itself corrected here.
--   * a job work is recomputed only when its stored Sticker Qty is exactly
--     pieces / the old (count) figure -- i.e. it was provably worked out
--     from the wrong number.
--   A line that does not pass is NOT guessed at. On a database with no such
--   lines this whole script is a no-op.
--
-- Every value it changes is snapshotted first, so this is reversible:
--
--   UPDATE order_items oi
--   SET pc_per_box = b.old_pc_per_box, box_per_cartoon = b.old_box_per_cartoon
--   FROM order_line_packing_repair_backup b
--   WHERE b.order_item_id = oi.id;
--
--   UPDATE job_works jw
--   SET sticker_qty = b.old_sticker_qty, total_carton = b.old_total_carton
--   FROM job_work_packing_repair_backup b
--   WHERE b.job_work_id = jw.id;
--
-- The backup tables are left behind on purpose. They are small (one row per
-- line or job work actually repaired, none on a healthy database) and they
-- are the only record of what the old figures were.
-- ============================================================

CREATE TABLE IF NOT EXISTS order_line_packing_repair_backup
(
    order_item_id       BIGINT PRIMARY KEY,
    -- OWN_FIGURES: rate recovered from the line's own pc_per_cartoon.
    -- MASTER_RATE: recovered from the packing on file. MERGE_TARGET: copied
    -- from a corrected source line.
    how                 VARCHAR(20)      NOT NULL,
    qty_pc              DOUBLE PRECISION,
    old_pc_per_box      DOUBLE PRECISION,
    old_box_per_cartoon DOUBLE PRECISION,
    new_pc_per_box      DOUBLE PRECISION,
    new_box_per_cartoon DOUBLE PRECISION,
    repaired_at         TIMESTAMP        NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS job_work_packing_repair_backup
(
    job_work_id      BIGINT PRIMARY KEY,
    order_item_id    BIGINT,
    qty_pc           DOUBLE PRECISION,
    old_sticker_qty  DOUBLE PRECISION,
    old_total_carton DOUBLE PRECISION,
    new_sticker_qty  DOUBLE PRECISION,
    new_total_carton DOUBLE PRECISION,
    repaired_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Snapshot first. Whatever lands in the backup tables is exactly the set of
-- rows the UPDATEs below touch -- they drive off these tables rather than
-- re-evaluating the tests.

-- 1. Lines whose rate can be recovered from their own figures.
INSERT INTO order_line_packing_repair_backup
    (order_item_id, how, qty_pc, old_pc_per_box, old_box_per_cartoon, new_pc_per_box, new_box_per_cartoon)
SELECT oi.id,
       'OWN_FIGURES',
       oi.qty_pc,
       oi.pc_per_box,
       oi.box_per_cartoon,
       c.rate,
       -- A line saved with no carton figure keeps none; the job work falls
       -- back to the item master for it, as it always did.
       CASE WHEN oi.box_per_cartoon IS NULL THEN NULL ELSE oi.pc_per_cartoon / c.rate END
FROM order_items oi
         CROSS JOIN LATERAL (
    SELECT MIN(r) AS rate, COUNT(*) AS candidates
    -- The bound is repeated here so a stray huge or fractional value can
    -- never be expanded into a series, whatever order the planner picks.
    FROM generate_series(
                 1,
                 CASE
                     WHEN oi.pc_per_cartoon BETWEEN 1 AND 100000
                         AND oi.pc_per_cartoon = FLOOR(oi.pc_per_cartoon)
                         THEN oi.pc_per_cartoon::int
                     ELSE 0
                     END) AS r
    WHERE oi.pc_per_cartoon::int % r = 0
      AND CEIL(oi.qty_pc / r) = oi.pc_per_box
      AND (oi.box_per_cartoon IS NULL
        OR CEIL(oi.pc_per_box / (oi.pc_per_cartoon / r)) = oi.box_per_cartoon)
    ) c
WHERE oi.qty_pc > 0
  AND oi.pc_per_box > 0
  AND oi.pc_per_cartoon BETWEEN 1 AND 100000
  AND oi.pc_per_cartoon = FLOOR(oi.pc_per_cartoon)
  AND oi.sticker_qty = oi.pc_per_box
  AND c.candidates = 1
  -- Something to correct: a 100-piece line at 10 to the box reads "10"
  -- either way, and is only touched if its carton figure is a count.
  AND (c.rate <> oi.pc_per_box
    OR (oi.box_per_cartoon IS NOT NULL AND oi.pc_per_cartoon / c.rate <> oi.box_per_cartoon))
ON CONFLICT (order_item_id) DO NOTHING;

-- 2. Lines with no pc_per_cartoon to recover from: the packing on file.
INSERT INTO order_line_packing_repair_backup
    (order_item_id, how, qty_pc, old_pc_per_box, old_box_per_cartoon, new_pc_per_box, new_box_per_cartoon)
SELECT oi.id,
       'MASTER_RATE',
       oi.qty_pc,
       oi.pc_per_box,
       oi.box_per_cartoon,
       m.rate,
       CASE WHEN oi.box_per_cartoon IS NULL THEN NULL ELSE m.box_rate END
FROM order_items oi
         JOIN orders o ON o.id = oi.order_id
         LEFT JOIN client_inventory ci ON ci.client_id = o.party_id AND ci.size_id = oi.size_id
         LEFT JOIN inventory inv ON inv.size_id = oi.size_id
         CROSS JOIN LATERAL (
    -- The order form took each figure from the party's packing when it had
    -- one, and from the item master otherwise.
    SELECT COALESCE(ci.pcs_per_box, inv.pcs_per_box)::double precision       AS rate,
           COALESCE(ci.box_per_carton, inv.box_per_carton)::double precision AS box_rate
    ) m
WHERE oi.qty_pc > 0
  AND oi.pc_per_box > 0
  AND (oi.pc_per_cartoon IS NULL OR oi.pc_per_cartoon <= 0)
  AND oi.sticker_qty = oi.pc_per_box
  AND m.rate > 0
  AND oi.pc_per_box = CEIL(oi.qty_pc / m.rate)
  AND oi.pc_per_box <> m.rate
  AND (oi.box_per_cartoon IS NULL
    OR (m.box_rate > 0 AND oi.box_per_cartoon = CEIL(oi.pc_per_box / m.box_rate)))
ON CONFLICT (order_item_id) DO NOTHING;

-- 3. Merged lines still carrying the figures copied from a corrected line.
INSERT INTO order_line_packing_repair_backup
    (order_item_id, how, qty_pc, old_pc_per_box, old_box_per_cartoon, new_pc_per_box, new_box_per_cartoon)
SELECT DISTINCT ON (t.id) t.id,
                          'MERGE_TARGET',
                          t.qty_pc,
                          t.pc_per_box,
                          t.box_per_cartoon,
                          b.new_pc_per_box,
                          b.new_box_per_cartoon
FROM order_items t
         JOIN order_items s ON s.merged_into_item_id = t.id
         JOIN order_line_packing_repair_backup b ON b.order_item_id = s.id
WHERE t.pc_per_box IS NOT DISTINCT FROM b.old_pc_per_box
  AND t.box_per_cartoon IS NOT DISTINCT FROM b.old_box_per_cartoon
ORDER BY t.id, s.id
ON CONFLICT (order_item_id) DO NOTHING;

-- 4. Job works on those lines whose stickers were worked out from the count.
INSERT INTO job_work_packing_repair_backup
    (job_work_id, order_item_id, qty_pc, old_sticker_qty, old_total_carton, new_sticker_qty, new_total_carton)
SELECT jw.id,
       jw.order_item_id,
       jw.qty_pc,
       jw.sticker_qty,
       jw.total_carton,
       jw.qty_pc / b.new_pc_per_box,
       CASE WHEN x.box_rate > 0 THEN (jw.qty_pc / b.new_pc_per_box) / x.box_rate END
FROM job_works jw
         JOIN order_line_packing_repair_backup b ON b.order_item_id = jw.order_item_id
         LEFT JOIN inventory inv ON inv.size_id = jw.size_id
         CROSS JOIN LATERAL (
    -- Same precedence the server uses: the line's own rate, else the master's.
    SELECT COALESCE(b.new_box_per_cartoon, inv.box_per_carton::double precision) AS box_rate
    ) x
WHERE jw.qty_pc > 0
  AND b.old_pc_per_box > 0
  AND b.new_pc_per_box > 0
  AND jw.sticker_qty IS NOT NULL
  AND ABS(jw.sticker_qty - jw.qty_pc / b.old_pc_per_box) <= 0.000001 * GREATEST(1, ABS(jw.sticker_qty))
ON CONFLICT (job_work_id) DO NOTHING;

-- Apply. Each UPDATE re-checks that the row still holds the snapshotted old
-- value, so running this twice changes nothing the second time.
UPDATE order_items oi
SET pc_per_box      = b.new_pc_per_box,
    box_per_cartoon = b.new_box_per_cartoon
FROM order_line_packing_repair_backup b
WHERE b.order_item_id = oi.id
  AND oi.pc_per_box IS NOT DISTINCT FROM b.old_pc_per_box
  AND oi.box_per_cartoon IS NOT DISTINCT FROM b.old_box_per_cartoon;

UPDATE job_works jw
SET sticker_qty  = b.new_sticker_qty,
    total_carton = b.new_total_carton
FROM job_work_packing_repair_backup b
WHERE b.job_work_id = jw.id
  AND jw.sticker_qty IS NOT DISTINCT FROM b.old_sticker_qty
  AND jw.total_carton IS NOT DISTINCT FROM b.old_total_carton;
