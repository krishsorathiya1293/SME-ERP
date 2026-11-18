-- Bonus and upad jama entered any day from Pagar Didho, without settling the pay week.
--   BONUS: amount is added to what the company owes the worker; paid_now of it may be handed
--          over on the day (the rest is paid with the salary).
--   JAMA:  the worker gives upad back in cash; pending upad goes down, salary is untouched.
CREATE TABLE IF NOT EXISTS staff_bonus_jama
(
    id              BIGSERIAL PRIMARY KEY,
    emp_code        VARCHAR(50)      NOT NULL,
    entry_type      VARCHAR(10)      NOT NULL,
    entry_date      DATE             NOT NULL,
    amount          DOUBLE PRECISION NOT NULL,
    paid_now        DOUBLE PRECISION NOT NULL DEFAULT 0,
    payment_mode    VARCHAR(20),
    note            VARCHAR(255),
    created_at      TIMESTAMP(6),
    last_updated_at TIMESTAMP(6)
);
CREATE INDEX IF NOT EXISTS ix_staff_bonus_jama_emp ON staff_bonus_jama (emp_code, entry_date);
