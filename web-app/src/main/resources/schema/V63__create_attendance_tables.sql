-- ============================================================
-- attendance
-- Face-recognition punches pulled from the factory PC's EasyTime
-- database by the Ishita HRMS desktop app (electron/erpSync.js) and
-- pushed to /api/v1/attendance/sync/*. EasyTime stays the master;
-- these tables are a read copy for the ERP.
-- ============================================================

CREATE TABLE IF NOT EXISTS attendance_employee
(
    id              BIGSERIAL PRIMARY KEY,

    emp_code        VARCHAR(50)  NOT NULL,   -- EasyTime personnel_employee.emp_code
    first_name      VARCHAR(255),
    last_name       VARCHAR(255),
    department      VARCHAR(255),

    created_at      TIMESTAMP(6),
    last_updated_at TIMESTAMP(6),

    CONSTRAINT uq_attendance_employee_code UNIQUE (emp_code)
);

CREATE TABLE IF NOT EXISTS attendance_punch
(
    id              BIGSERIAL PRIMARY KEY,

    emp_code        VARCHAR(50)  NOT NULL,
    punch_time      TIMESTAMP(0) NOT NULL,   -- factory local time, as the device recorded it
    punch_state     VARCHAR(10),             -- EasyTime punch_state (0 = in, 1 = out, ...)
    verify_type     INTEGER,                 -- 15 = face, 1 = fingerprint, ...
    terminal_sn     VARCHAR(100) NOT NULL DEFAULT '',
    terminal_alias  VARCHAR(255),
    source_id       BIGINT,                  -- EasyTime iclock_transaction.id (informational)

    created_at      TIMESTAMP(6),
    last_updated_at TIMESTAMP(6),

    -- Natural key, so a re-sent batch or a reinstalled EasyTime (ids restart) never duplicates.
    CONSTRAINT uq_attendance_punch UNIQUE (emp_code, punch_time, terminal_sn)
);

CREATE INDEX IF NOT EXISTS ix_attendance_punch_time
    ON attendance_punch (punch_time);
