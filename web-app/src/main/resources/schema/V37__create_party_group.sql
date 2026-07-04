-- Party groups: a "customer group" (e.g. "Mahaveer") that owns several companies/parties
-- (Mahaveer-Eshaan, Mahaveer-Pittal ...). One client login is provisioned at the group level; the
-- client then chooses which member company/party to act as. Standalone parties (group_id NULL)
-- keep their own individual login and behave exactly as before.
CREATE TABLE party_group
(
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255),
    contact_no      VARCHAR(20),
    created_at      TIMESTAMP(6),
    last_updated_at TIMESTAMP(6)
);

-- A party may belong to at most one group (NULL = standalone).
ALTER TABLE party
    ADD COLUMN group_id BIGINT,
    ADD CONSTRAINT fk_party_group FOREIGN KEY (group_id) REFERENCES party_group (id);
CREATE INDEX idx_party_group_id ON party (group_id);

-- A CLIENT user is linked to EITHER a single party (standalone) OR a group (the "party admin"
-- login). group_id is unique so a group has exactly one login; multiple NULLs are allowed for
-- standalone/admin users.
ALTER TABLE users
    ADD COLUMN group_id BIGINT,
    ADD CONSTRAINT uq_users_group_id UNIQUE (group_id),
    ADD CONSTRAINT fk_users_group FOREIGN KEY (group_id) REFERENCES party_group (id);
