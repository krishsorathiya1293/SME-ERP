-- ============================================================
-- Staff & payroll, built on top of the face-device attendance (V63).
-- Mirrors the client's "Attendence Software.xlsx": Add Emp., Profile
-- Design, Salary Paid, Payment History / Upad. All of this lives in the
-- ERP only; the device sync never writes these columns.
-- ============================================================

-- Departments with their shift timings (sheet "Add Emp.").
-- Break = out_time_1 -> in_time_2. Standard hours = both halves added.
CREATE TABLE IF NOT EXISTS staff_department
(
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    in_time_1       TIME         NOT NULL,
    out_time_1      TIME         NOT NULL,
    in_time_2       TIME,
    out_time_2      TIME,
    created_at      TIMESTAMP(6),
    last_updated_at TIMESTAMP(6),
    CONSTRAINT uq_staff_department_name UNIQUE (name)
);

INSERT INTO staff_department (name, in_time_1, out_time_1, in_time_2, out_time_2, created_at, last_updated_at)
VALUES ('Extrusion',    '08:00', '12:30', '13:30', '19:20', now(), now()),
       ('Production',   '08:30', '12:30', '13:30', '18:50', now(), now()),
       ('Emrey',        '08:00', '12:30', '13:30', '20:00', now(), now()),
       ('Finish Plant', '08:00', '12:30', '13:30', '19:00', now(), now())
ON CONFLICT ON CONSTRAINT uq_staff_department_name DO NOTHING;

-- The ERP-side profile, on the same row as the synced device employee.
-- first_name / last_name / department stay device-owned; `name` is the
-- ERP's own (falls back to the device name when empty).
ALTER TABLE attendance_employee
    ADD COLUMN IF NOT EXISTS name                VARCHAR(255),
    ADD COLUMN IF NOT EXISTS gender              VARCHAR(10),
    ADD COLUMN IF NOT EXISTS phone               VARCHAR(20),
    ADD COLUMN IF NOT EXISTS aadhar_no           VARCHAR(20),
    ADD COLUMN IF NOT EXISTS birth_date          DATE,
    ADD COLUMN IF NOT EXISTS reference           VARCHAR(255),
    ADD COLUMN IF NOT EXISTS joining_date        DATE,
    ADD COLUMN IF NOT EXISTS staff_department_id BIGINT REFERENCES staff_department (id),
    ADD COLUMN IF NOT EXISTS per_day_salary      DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS ot_rate_per_hour    DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS pay_mode            VARCHAR(20),
    -- Balances carried over from the previous payroll software, as of balance_from.
    -- Salary accrues into the balance from balance_from onwards.
    ADD COLUMN IF NOT EXISTS opening_balance     DOUBLE PRECISION NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS opening_loan        DOUBLE PRECISION NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS balance_from        DATE,
    ADD COLUMN IF NOT EXISTS active              BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    -- Images as data URLs, shrunk in the browser before upload.
    ADD COLUMN IF NOT EXISTS photo               TEXT,
    ADD COLUMN IF NOT EXISTS photo_thumb         TEXT,
    ADD COLUMN IF NOT EXISTS aadhar_photo        TEXT;

-- "Edit Attendence - manually": a hand-set day that replaces the punches.
CREATE TABLE IF NOT EXISTS attendance_adjustment
(
    id              BIGSERIAL PRIMARY KEY,
    emp_code        VARCHAR(50) NOT NULL,
    work_date       DATE        NOT NULL,
    in_time         TIME,
    out_time        TIME,
    break_minutes   INTEGER,
    absent          BOOLEAN     NOT NULL DEFAULT FALSE,
    note            VARCHAR(255),
    created_at      TIMESTAMP(6),
    last_updated_at TIMESTAMP(6),
    CONSTRAINT uq_attendance_adjustment UNIQUE (emp_code, work_date)
);

-- Upad (advance / loan) given to a worker.
CREATE TABLE IF NOT EXISTS staff_upad
(
    id              BIGSERIAL PRIMARY KEY,
    emp_code        VARCHAR(50)      NOT NULL,
    entry_date      DATE             NOT NULL,
    amount          DOUBLE PRECISION NOT NULL,
    payment_mode    VARCHAR(20)      NOT NULL,
    note            VARCHAR(255),
    created_at      TIMESTAMP(6),
    last_updated_at TIMESTAMP(6)
);
CREATE INDEX IF NOT EXISTS ix_staff_upad_emp ON staff_upad (emp_code, entry_date);

-- "Pagar Didho": one settled pay period. Snapshots keep history stable
-- even if the worker's salary changes later.
CREATE TABLE IF NOT EXISTS staff_salary_payment
(
    id              BIGSERIAL PRIMARY KEY,
    emp_code        VARCHAR(50)      NOT NULL,
    period_from     DATE             NOT NULL,
    period_to       DATE             NOT NULL,
    opening_balance DOUBLE PRECISION NOT NULL DEFAULT 0,
    total_salary    DOUBLE PRECISION NOT NULL DEFAULT 0,
    bonus           DOUBLE PRECISION NOT NULL DEFAULT 0,
    loan_deduction  DOUBLE PRECISION NOT NULL DEFAULT 0,
    payable         DOUBLE PRECISION NOT NULL DEFAULT 0,
    paid_amount     DOUBLE PRECISION NOT NULL DEFAULT 0,
    payment_mode    VARCHAR(20)      NOT NULL,
    paid_on         DATE             NOT NULL,
    note            VARCHAR(255),
    created_at      TIMESTAMP(6),
    last_updated_at TIMESTAMP(6)
);
CREATE INDEX IF NOT EXISTS ix_staff_salary_payment_emp ON staff_salary_payment (emp_code, period_from);

-- Payroll-wide settings: which day a weekly pay week starts, and the weekly off.
CREATE TABLE IF NOT EXISTS staff_setting
(
    setting_key     VARCHAR(50) PRIMARY KEY,
    setting_value   VARCHAR(100),
    created_at      TIMESTAMP(6),
    last_updated_at TIMESTAMP(6)
);
INSERT INTO staff_setting (setting_key, setting_value, created_at, last_updated_at)
VALUES ('WEEK_START', 'SATURDAY', now(), now()),
       ('WEEKLY_OFF', 'NONE', now(), now())
ON CONFLICT (setting_key) DO NOTHING;
