-- ============================================================
-- Link an approved client order request to the order it created
-- in Order Management, so the Client Portal can surface the live
-- job-work / dispatch status of that order.
-- ============================================================

ALTER TABLE client_order_requests
    ADD COLUMN IF NOT EXISTS order_id BIGINT;

ALTER TABLE client_order_requests
    ADD CONSTRAINT fk_client_order_requests_order
        FOREIGN KEY (order_id)
            REFERENCES orders (id)
            ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS ix_client_order_requests_order_id
    ON client_order_requests (order_id);
