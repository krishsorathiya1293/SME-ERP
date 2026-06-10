-- Backfill CLIENT user accounts for parties that were created before the
-- client-portal feature existed (and therefore have no linked user yet).
--
-- The generated password is stored only as a bcrypt hash (pgcrypto's
-- gen_salt('bf') produces $2a$ hashes, which Spring's BCryptPasswordEncoder
-- can verify). The plaintext is NOT retained for these backfilled accounts,
-- so initial_password stays NULL -- use the "Reset credentials" action in the
-- Client Portal to (re)generate a downloadable password for these parties.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO users (username, password, user_group, enabled, party_id, created_at, last_updated_at)
SELECT
    'CLI' || LPAD(p.id::text, 4, '0'),
    crypt(substr(md5(random()::text || p.id::text), 1, 12), gen_salt('bf', 10)),
    'CLIENT',
    TRUE,
    p.id,
    now(),
    now()
FROM party p
WHERE NOT EXISTS (
    SELECT 1 FROM users u WHERE u.party_id = p.id
);
