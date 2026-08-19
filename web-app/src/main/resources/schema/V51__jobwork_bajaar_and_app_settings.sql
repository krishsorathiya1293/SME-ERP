-- ============================================================
-- Bajaar (market rate) on a job work, plus the console settings
-- table the "fixed" rate lives in.
--
-- Two kinds of bajaar. FIXED is one house rate that applies to
-- every chitthi and is maintained from Settings, so the job work
-- stores only the choice and reads the amount from app_settings --
-- change the setting and every fixed job work follows. ROJNU
-- (daily) is negotiated per chitthi, so its amount is stored on
-- the row and starts empty until someone types it.
-- ============================================================

ALTER TABLE job_works ADD COLUMN IF NOT EXISTS bajaar_type  VARCHAR(16);
ALTER TABLE job_works ADD COLUMN IF NOT EXISTS bajaar_value DOUBLE PRECISION;

-- Key/value rather than a column per setting: these are single scalars
-- edited by hand from the Settings screen, never queried or joined on.
CREATE TABLE IF NOT EXISTS app_settings
(
    id              BIGSERIAL PRIMARY KEY,

    setting_key     VARCHAR(64) NOT NULL,
    setting_value   VARCHAR(512),

    created_at      TIMESTAMP(6),
    last_updated_at TIMESTAMP(6),

    CONSTRAINT uq_app_settings_key UNIQUE (setting_key)
);
