ALTER TABLE sme_erp.job_works
    ADD COLUMN IF NOT EXISTS chitthi_no   VARCHAR(100),
    ADD COLUMN IF NOT EXISTS chitthi_date DATE,
    ADD COLUMN IF NOT EXISTS order_time   VARCHAR(10);
