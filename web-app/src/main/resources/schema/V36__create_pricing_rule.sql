-- Dynamic finish-price formulas: finish = S.S. * multiplier + offset_value
-- Scope precedence (most specific wins): client+item -> client -> item -> global (both NULL)
CREATE TABLE sme_erp.pricing_rule
(
    id              BIGSERIAL PRIMARY KEY,
    client_id       BIGINT,
    item_id         BIGINT,
    finish_key      VARCHAR(50)      NOT NULL,
    multiplier      DOUBLE PRECISION NOT NULL DEFAULT 1,
    offset_value    DOUBLE PRECISION NOT NULL DEFAULT 0,
    created_at      TIMESTAMP(6),
    last_updated_at TIMESTAMP(6),

    CONSTRAINT fk_pricing_rule_client
        FOREIGN KEY (client_id) REFERENCES sme_erp.party (id),
    CONSTRAINT fk_pricing_rule_item
        FOREIGN KEY (item_id) REFERENCES sme_erp.item_inventory (id)
);

-- One rule per (scope, finish). NULLs are normalized so a single global / per-client row is enforced.
CREATE UNIQUE INDEX ux_pricing_rule_scope
    ON sme_erp.pricing_rule (COALESCE(client_id, -1), COALESCE(item_id, -1), finish_key);

-- Seed global defaults (client_id NULL, item_id NULL) with the previously hardcoded offsets.
INSERT INTO sme_erp.pricing_rule (finish_key, multiplier, offset_value, created_at, last_updated_at)
VALUES ('antiq', 1, 10, now(), now()),
       ('sidegold', 1, 12, now(), now()),
       ('sartinlacq', 1, 0, now(), now()),
       ('zblack', 1, 105, now(), now()),
       ('grblack', 1, 60, now(), now()),
       ('mattss', 1, 30, now(), now()),
       ('mattantiq', 1, 60, now(), now()),
       ('pvdrose', 1, 400, now(), now()),
       ('pvdgold', 1, 400, now(), now()),
       ('pvdblack', 1, 400, now(), now()),
       ('rosegold', 1, 400, now(), now()),
       ('clearlacq', 1, 400, now(), now());
