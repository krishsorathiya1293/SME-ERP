-- Gres Filling: monthly-reset Ch. No. (client spec: "001", resets each month)
ALTER TABLE sme_erp.gres_fillings
    ADD COLUMN ch_no_serial INT,
    ADD COLUMN ch_no_year_month VARCHAR(7);

-- Backfill existing rows so they don't collide with the new counter. The old
-- chitthi_no format was "NN/Gres.Fill/YYYY"; parse the leading NN and stamp
-- the year-month from chitthi_date. Rows without a date get NULL.
UPDATE sme_erp.gres_fillings
SET ch_no_serial      = CAST(SPLIT_PART(chitthi_no, '/', 1) AS INT),
    ch_no_year_month  = TO_CHAR(chitthi_date, 'YYYY-MM')
WHERE chitthi_no ~ '^[0-9]+/'
  AND chitthi_date IS NOT NULL;

CREATE UNIQUE INDEX uk_gres_ch_no_month
    ON sme_erp.gres_fillings (ch_no_year_month, ch_no_serial)
    WHERE ch_no_serial IS NOT NULL;

-- Tare weight per Peti (Kg) — client's Excel makes this a per-record manual
-- field. The old element_weight_gm concept moves out of the frontend.
ALTER TABLE sme_erp.gres_filling_items
    ADD COLUMN peti_weight_kg DOUBLE PRECISION;
