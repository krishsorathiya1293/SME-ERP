-- ============================================================
-- Backfill the pipeline stage on client order requests.
--
-- A client order request used to stop at APPROVED and stay there no matter what
-- happened to the order it produced, so an order already out for plating still
-- showed under "Approved" in Order Approvals. The application now writes the
-- stage back on every job-work / dispatch event; this brings existing rows into
-- line so the tabs are correct from the first load rather than only after the
-- next event touches an order.
--
-- Mirrors ClientOrderFulfillmentServiceImpl: an order takes the FURTHEST stage
-- any of its lines has reached, except DISPATCHED which needs every line.
--   0 APPROVED           - not sent anywhere yet
--   1 IN_PLATING         - out for job work, nothing back
--   2 READY_TO_DISPATCH  - the job worker has returned something
--   3 DISPATCHED         - every piece ordered has gone out
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
                            -- reached, with an already-dispatched line counting as ready (2).
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
  -- Never touch a request the admin still has to decide on, or has rejected.
  AND r.status IN ('APPROVED', 'IN_PROGRESS', 'COMPLETED');
