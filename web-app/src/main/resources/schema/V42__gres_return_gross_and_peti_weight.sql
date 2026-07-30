-- Gres Filling Return: bring the return form up to the same Excel shape as the
-- forward flow. The user enters Peti + gross Kgs + 1-Peti tare; the server derives
-- Net Kg (stored in the existing return_kg column) and Ghati.
ALTER TABLE sme_erp.gres_filling_returns
    ADD COLUMN gross_kg DOUBLE PRECISION,
    ADD COLUMN peti_weight_kg DOUBLE PRECISION;
