-- A job work now draws its net kg out of the size's stock. What a chitthi took is kept on the
-- chitthi itself, so editing it adjusts the stock by the difference and deleting it puts back
-- exactly that much. NULL = it never drew from stock: raised before this existed, or the size had
-- no stock entry at the time (stock of such a size is not checked).
ALTER TABLE job_works
    ADD COLUMN IF NOT EXISTS stock_kg   DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS stock_peti INTEGER;

-- A stock entry can now be given by the peti (weighing machine), as loose kg, or as a count of
-- pieces. NULL = PETI, which is what every entry before this was.
ALTER TABLE stock_entry
    ADD COLUMN IF NOT EXISTS entry_unit VARCHAR(10);

-- stock_entry.entry_type gains MINUS (taken off by hand), JW_OUT (sent to job work) and JW_BACK
-- (put back when a job work is edited down or deleted). The column is already VARCHAR(10).
