-- Manual job work is not tied to an order item, so order_item_id must be nullable.
-- (The UNIQUE constraint stays; Postgres allows multiple NULLs in a unique column.)
ALTER TABLE job_works
    ALTER COLUMN order_item_id DROP NOT NULL;
