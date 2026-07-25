-- Job Work rework to match the client's Out-Side/In-Side Job-Work excel model:
-- gross weight + peti tare -> auto net kg, auto total pcs / stickers / cartons
-- (using the item's own master rates), plus rate/kg -> total rate.

ALTER TABLE job_works
    ADD COLUMN IF NOT EXISTS peti_weight_kg DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS gross_kg        DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS sticker_qty     DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS total_carton    DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS rate_per_kg     DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS total_rate      DOUBLE PRECISION;

ALTER TABLE job_work_returns
    ADD COLUMN IF NOT EXISTS peti_weight_kg DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS gross_kg       DOUBLE PRECISION;
