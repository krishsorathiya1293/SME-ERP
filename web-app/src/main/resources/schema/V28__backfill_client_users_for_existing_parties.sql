-- Backfill CLIENT user accounts for parties that were created before the
-- client-portal feature existed (and therefore have no linked user yet).
--
-- A placeholder bcrypt hash is used as the password. These accounts are
-- non-functional until an admin uses the "Reset Credentials" action in the
-- Client Portal to generate a real password.
INSERT INTO users (username, password, user_group, enabled, party_id, created_at, last_updated_at)
SELECT
    'CLI' || LPAD(p.id::text, 4, '0'),
    '$2a$10$placeholder00000000000uGHEgNSPVNWnMHqjPOB0e6bPsMN3/S2',
    'CLIENT',
    TRUE,
    p.id,
    now(),
    now()
FROM party p
WHERE NOT EXISTS (
    SELECT 1 FROM users u WHERE u.party_id = p.id
);
