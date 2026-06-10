-- Rename existing CLIENT usernames from the old "CLI0001"-style format to a
-- slug derived from the linked party's name, matching the new registration
-- logic in UserService.registerClientUser/generateClientUsername. Usernames
-- that have already been customized away from the CLI#### pattern (e.g. the
-- client changed it, or it was already migrated) are left untouched.
DO $$
DECLARE
    rec RECORD;
    base_slug TEXT;
    candidate TEXT;
    suffix INT;
BEGIN
    FOR rec IN
        SELECT u.id AS user_id, p.name AS party_name
        FROM users u
        JOIN party p ON p.id = u.party_id
        WHERE u.user_group = 'CLIENT'
          AND u.username ~ '^CLI[0-9]+$'
    LOOP
        base_slug := lower(regexp_replace(coalesce(rec.party_name, ''), '[^a-zA-Z0-9]+', '', 'g'));
        IF base_slug = '' THEN
            base_slug := 'client';
        END IF;
        IF length(base_slug) > 20 THEN
            base_slug := substr(base_slug, 1, 20);
        END IF;

        candidate := base_slug;
        suffix := 1;
        WHILE EXISTS (SELECT 1 FROM users WHERE username = candidate AND id <> rec.user_id) LOOP
            suffix := suffix + 1;
            candidate := base_slug || suffix::text;
        END LOOP;

        UPDATE users SET username = candidate, last_updated_at = now() WHERE id = rec.user_id;
    END LOOP;
END $$;
