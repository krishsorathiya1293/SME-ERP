-- ============================================================
-- Re-backfill the pipeline stage on client order requests.
--
-- V48 rolled an order up to the stage of its LEAST advanced line. Real orders
-- run a dozen lines that move at different speeds, so that kept a request in
-- "Approved" until the very last line had been sent for plating -- in practice,
-- never. ClientOrderFulfillmentServiceImpl now rolls up to the FURTHEST stage
-- any line has reached; this re-runs the backfill under that rule.
--
-- V48 is deliberately left untouched: it has already been applied, and editing
-- an applied migration breaks Flyway's checksum validation on the next start.
--
--   0 APPROVED           - not sent anywhere yet
--   1 IN_PLATING         - out for job work, nothing back
--   2 READY_TO_DISPATCH  - the job worker has returned something
--   3 DISPATCHED         - every piece ordered has gone out
--
-- Stages are derived from the works themselves, not from the status V48 left
-- behind, so this is a straight recompute rather than a patch on top.
-- ============================================================

WITH item_stage AS (SELECT oi.order_id,
                           CASE
                               WHEN COALESCE(oi.qty_pc, 0) > 0
                                   AND COALESCE((SELECT SUM(d.dispatch_pcs)
                                                 FROM order_dispatch d
                                                 WHERE d.order_item_id = oi.id), 0) >= oi.qty_pc
                                   THEN 3
                               WHEN jw.id IS NULL THEN 0
                               WHEN jw.status = 'COMPLETE'
                                   OR COALESCE((SELECT SUM(r.return_kg)
                                                FROM job_work_returns r
                                                WHERE r.job_work_id = jw.id), 0) > 0
                                   THEN 2
                               ELSE 1
                               END AS stage
                    FROM order_items oi
                             LEFT JOIN job_works jw ON jw.order_item_id = oi.id),
     order_stage AS (SELECT order_id,
                            -- All lines dispatched -> DISPATCHED; otherwise the furthest any line
                            -- reached, with an already-dispatched line counting as ready (2) so a
                            -- part-shipped order doesn't read as fully dispatched.
                            CASE
                                WHEN MIN(stage) = 3 THEN 3
                                ELSE LEAST(MAX(stage), 2)
                                END AS stage
                     FROM item_stage
                     GROUP BY order_id)
UPDATE client_order_requests r
SET status = CASE os.stage
                 WHEN 3 THEN 'DISPATCHED'
                 WHEN 2 THEN 'READY_TO_DISPATCH'
                 WHEN 1 THEN 'IN_PLATING'
                 ELSE 'APPROVED'
    END
FROM order_stage os
WHERE r.order_id = os.order_id
  -- Every stage the works may own. Never touch a request the admin still has to
  -- decide on, or has rejected.
  AND r.status NOT IN ('PENDING_APPROVAL', 'REJECTED');
