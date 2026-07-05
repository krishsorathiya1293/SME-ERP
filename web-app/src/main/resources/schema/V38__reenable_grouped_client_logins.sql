-- Party group members now keep their OWN client login working in addition to the shared group
-- login (both work). An earlier version disabled a party's individual login when it joined a
-- group; re-enable any client login that was disabled that way so nobody is locked out.
UPDATE users
SET enabled = true, last_updated_at = now()
WHERE user_group = 'CLIENT'
  AND party_id IS NOT NULL
  AND enabled = false;
