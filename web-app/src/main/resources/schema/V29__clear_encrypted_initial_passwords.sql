-- initial_password is no longer stored encrypted (it is now a plain
-- VARCHAR so admins can view/export it directly). Any pre-existing values
-- were written under the old encrypted envelope format and are unusable;
-- clear them so those accounts show as "Pending" again and can be
-- regenerated via the "Reset Credentials" action.
UPDATE users
SET initial_password = NULL
WHERE user_group = 'CLIENT'
  AND initial_password IS NOT NULL;
