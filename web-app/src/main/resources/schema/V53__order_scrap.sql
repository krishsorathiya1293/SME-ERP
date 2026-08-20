-- ============================================================
-- Scrap moves from the chitthi to the order.
--
-- The rate was put on the job work as a "bajaar", in two kinds:
-- FIXED (one house rate held in app_settings) and ROJNU (agreed
-- per chitthi). The works settle it once, with the party, when
-- the order is taken -- not with the plater, and not per batch --
-- so it belongs to the order, and the two names for one number
-- were only ever a source of confusion. One nullable amount on
-- the order replaces both.
-- ============================================================

ALTER TABLE orders ADD COLUMN IF NOT EXISTS scrap DOUBLE PRECISION;

-- Carry the rates already entered onto the orders they were entered against, so nothing typed on
-- the floor is lost.
--
-- A chitthi reaches an order two ways: through the line it was raised against, and -- when it is a
-- merged one -- through every other line it covers, which may sit on a different order. Both are
-- followed, since the rate applied to the whole batch either way. Where several chitthis point at
-- one order the rate was the same number on all of them, so the latest simply settles the tie.
-- FIXED chitthis resolved their amount from the setting at read time, so that is what they are
-- worth here.
UPDATE orders o
SET scrap = latest.amount
FROM (
    SELECT DISTINCT ON (link.order_id) link.order_id, link.amount
    FROM (
        SELECT oi.order_id,
               jw.id AS job_work_id,
               CASE
                   WHEN jw.bajaar_type = 'ROJNU' THEN jw.bajaar_value
                   WHEN jw.bajaar_type = 'FIXED' THEN (
                       SELECT NULLIF(TRIM(s.setting_value), '')::DOUBLE PRECISION
                       FROM app_settings s
                       WHERE s.setting_key = 'jobwork.fixed.bajaar'
                         AND TRIM(s.setting_value) ~ '^[0-9]+(\.[0-9]+)?$'
                   )
               END AS amount
        FROM job_works jw
        JOIN order_items oi ON oi.id = jw.order_item_id
        WHERE jw.bajaar_type IS NOT NULL

        UNION ALL

        SELECT oi.order_id,
               jw.id AS job_work_id,
               CASE
                   WHEN jw.bajaar_type = 'ROJNU' THEN jw.bajaar_value
                   WHEN jw.bajaar_type = 'FIXED' THEN (
                       SELECT NULLIF(TRIM(s.setting_value), '')::DOUBLE PRECISION
                       FROM app_settings s
                       WHERE s.setting_key = 'jobwork.fixed.bajaar'
                         AND TRIM(s.setting_value) ~ '^[0-9]+(\.[0-9]+)?$'
                   )
               END AS amount
        FROM job_works jw
        JOIN job_work_order_items jwoi ON jwoi.job_work_id = jw.id
        JOIN order_items oi ON oi.id = jwoi.order_item_id
        WHERE jw.bajaar_type IS NOT NULL
    ) AS link
    WHERE link.amount IS NOT NULL
    ORDER BY link.order_id, link.job_work_id DESC
) AS latest
WHERE o.id = latest.order_id;

ALTER TABLE job_works DROP COLUMN IF EXISTS bajaar_type;
ALTER TABLE job_works DROP COLUMN IF EXISTS bajaar_value;

-- app_settings existed only to hold the fixed bajaar -- V51 created it for that and nothing else
-- ever wrote to it -- and there is no house-wide rate any more.
DROP TABLE IF EXISTS app_settings;
