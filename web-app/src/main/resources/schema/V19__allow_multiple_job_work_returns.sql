-- Allow multiple returns per job work (drop unique constraint on job_work_id)
ALTER TABLE job_work_returns
    DROP CONSTRAINT IF EXISTS job_work_returns_job_work_id_key;
