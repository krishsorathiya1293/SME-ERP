-- A salary payment freezes the salary of the days worked up to the day it was saved. When a
-- payment's period ran past that day (a monthly worker given money on the 22nd for the 17th to the
-- 31st), the later days were never counted. salary_through records the last frozen day; days after
-- it, up to period_to, are worked out live from attendance.
ALTER TABLE staff_salary_payment ADD COLUMN IF NOT EXISTS salary_through DATE;

-- Old payments: frozen up to the working day they were saved on (the working day starts at 7 AM).
UPDATE staff_salary_payment
SET salary_through = LEAST(period_to, CAST(created_at - INTERVAL '7 hours' AS DATE))
WHERE salary_through IS NULL
  AND created_at IS NOT NULL;
