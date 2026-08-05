-- ============================================================
-- job_works.job_work_no
-- Per-party, per-month sequential number shown on the app list
-- and the printed chitthi as "<PARTY_CODE>-<N>" (e.g. AZ-1, ZP-2).
-- Lives on the job work itself so manual (order-less) job works
-- are numbered too. Replaces the old order_items.job_work_no,
-- which only covered order-based job works.
-- ============================================================

ALTER TABLE job_works
    ADD COLUMN IF NOT EXISTS job_work_no INTEGER;

-- Backfill existing rows so their labels render too: a per-party, per-month sequence
-- ordered by creation, matching how new job works are numbered from now on.
WITH numbered AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY party_id, date_trunc('month', created_at)
               ORDER BY created_at, id
           ) AS rn
    FROM job_works
)
UPDATE job_works jw
SET job_work_no = n.rn
FROM numbered n
WHERE jw.id = n.id
  AND jw.job_work_no IS NULL;
