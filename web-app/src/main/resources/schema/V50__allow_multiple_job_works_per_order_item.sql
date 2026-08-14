-- ============================================================
-- Allow multiple job works per order item (partial / batched plating).
--
-- An order line of 100 Kg is not necessarily sent to the plater in one go --
-- 50 Kg may go now and the rest next week. order_item_id was UNIQUE, so a line
-- could only ever hold one job work and whatever was not sent the first time
-- could never leave the "Approved" bucket.
--
-- Same treatment order_dispatch got in V26 for partial dispatch.
-- ============================================================

ALTER TABLE job_works
    DROP CONSTRAINT IF EXISTS job_works_order_item_id_key;

DROP INDEX IF EXISTS job_works_order_item_id_key;
