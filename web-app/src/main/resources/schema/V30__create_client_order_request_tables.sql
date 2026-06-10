-- ============================================================
-- client_order_requests (header)
--
-- Order requests submitted by CLIENT users from the Client Portal
-- "Shop" page. These are reviewed/approved by an admin before they
-- become real orders.
-- ============================================================

CREATE TABLE IF NOT EXISTS client_order_requests
(
    id              BIGSERIAL PRIMARY KEY,
    party_id        BIGINT       NOT NULL,
    status          VARCHAR(30)  NOT NULL DEFAULT 'PENDING_APPROVAL',
    order_date      DATE         NOT NULL,
    created_at      TIMESTAMP(6),
    last_updated_at TIMESTAMP(6),

    CONSTRAINT fk_client_order_requests_party
        FOREIGN KEY (party_id)
            REFERENCES party (id)
            ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_client_order_requests_party_id
    ON client_order_requests (party_id);

CREATE INDEX IF NOT EXISTS ix_client_order_requests_status
    ON client_order_requests (status);

-- ============================================================
-- client_order_request_items (line items)
-- ============================================================

CREATE TABLE IF NOT EXISTS client_order_request_items
(
    id              BIGSERIAL PRIMARY KEY,
    request_id      BIGINT NOT NULL,
    item_id         BIGINT,
    item_name       VARCHAR(255),
    category        VARCHAR(255),
    size_id         BIGINT,
    size_in_inch    VARCHAR(50),
    size_in_mm      VARCHAR(50),
    plating         VARCHAR(100),
    qty_pc          DOUBLE PRECISION,
    qty_kg          DOUBLE PRECISION,
    pending_pc      DOUBLE PRECISION,
    created_at      TIMESTAMP(6),
    last_updated_at TIMESTAMP(6),

    CONSTRAINT fk_client_order_request_items_request
        FOREIGN KEY (request_id)
            REFERENCES client_order_requests (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_client_order_request_items_item
        FOREIGN KEY (item_id)
            REFERENCES item_inventory (id)
            ON DELETE SET NULL,

    CONSTRAINT fk_client_order_request_items_size
        FOREIGN KEY (size_id)
            REFERENCES size_inventory (id)
            ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS ix_client_order_request_items_request_id
    ON client_order_request_items (request_id);
