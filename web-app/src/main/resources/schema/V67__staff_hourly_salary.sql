-- Salary is now purely hourly: pay = worked hours x per-hour salary. No overtime rate, no
-- short-time deduction, no half day. per_day_salary and ot_rate_per_hour are no longer read;
-- they stay on the table so the old figures aren't lost.
ALTER TABLE attendance_employee
    ADD COLUMN IF NOT EXISTS per_hour_salary DOUBLE PRECISION;

-- Carry existing workers over at the per-hour rate they were effectively on:
-- per-day salary / the department's standard hours (both halves, break excluded).
-- Minutes are counted from 7 AM so night shifts measure right, same as WorkDay.between.
WITH m AS (
    SELECT id,
           MOD(MOD(EXTRACT(HOUR FROM in_time_1)::int * 60 + EXTRACT(MINUTE FROM in_time_1)::int - 420, 1440) + 1440, 1440) AS in1,
           MOD(MOD(EXTRACT(HOUR FROM out_time_1)::int * 60 + EXTRACT(MINUTE FROM out_time_1)::int - 420, 1440) + 1440, 1440) AS out1,
           MOD(MOD(EXTRACT(HOUR FROM in_time_2)::int * 60 + EXTRACT(MINUTE FROM in_time_2)::int - 420, 1440) + 1440, 1440) AS in2,
           MOD(MOD(EXTRACT(HOUR FROM out_time_2)::int * 60 + EXTRACT(MINUTE FROM out_time_2)::int - 420, 1440) + 1440, 1440) AS out2
    FROM staff_department
),
d AS (
    SELECT id,
           COALESCE(GREATEST(0, out1 - in1), 0)
               + CASE WHEN in2 IS NOT NULL AND out2 IS NOT NULL THEN GREATEST(0, out2 - in2) ELSE 0 END
               AS standard_minutes
    FROM m
)
UPDATE attendance_employee e
SET per_hour_salary = ROUND((e.per_day_salary / (d.standard_minutes / 60.0))::numeric, 2)
FROM d
WHERE e.staff_department_id = d.id
  AND e.per_hour_salary IS NULL
  AND e.per_day_salary > 0
  AND d.standard_minutes > 0;
