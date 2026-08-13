ALTER TABLE job_works ADD COLUMN IF NOT EXISTS job_work_label VARCHAR(50);

-- Backfill existing rows: partyCode(partyName) + '-' + jobWorkNo.
-- partyCode for multi-word names: first letter of each of the first two words.
-- partyCode for single-word names: first two letters.
UPDATE job_works jw
SET job_work_label = UPPER(
    CASE
        WHEN p.name ~ '\S+\s+\S+' THEN
            LEFT(TRIM(p.name), 1) ||
            LEFT(TRIM(SUBSTRING(p.name FROM POSITION(' ' IN TRIM(p.name)) + 1)), 1)
        ELSE
            LEFT(TRIM(p.name), 2)
    END
) || '-' || COALESCE(jw.job_work_no::TEXT, jw.id::TEXT)
FROM party p
WHERE p.id = jw.party_id
  AND jw.job_work_label IS NULL;
