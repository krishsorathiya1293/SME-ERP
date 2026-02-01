CREATE TABLE party
(
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255),
    gst             VARCHAR(50),
    contact_no      VARCHAR(20),
    email           VARCHAR(255),
    party_type      VARCHAR(50),
    created_at      TIMESTAMP(6),
    last_updated_at TIMESTAMP(6)
);
CREATE INDEX idx_party_party_type
    ON party (party_type);
