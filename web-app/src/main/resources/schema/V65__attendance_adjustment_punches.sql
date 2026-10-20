-- A hand-edited day can hold every in/out, not just the first in and last out:
-- "08:33,12:32,13:30,20:16". in_time / out_time stay as its first and last.
ALTER TABLE attendance_adjustment
    ADD COLUMN IF NOT EXISTS punch_times VARCHAR(500);
