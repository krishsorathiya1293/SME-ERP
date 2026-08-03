-- ============================================================
-- translation
-- Global dictionary of party-name / finish transliterations
-- (English source -> Hindi + Gujarati), editable by the user
-- and reused by the Job Work print instead of the live
-- Google transliteration call.
-- ============================================================

CREATE TABLE IF NOT EXISTS translation
(
    id              BIGSERIAL PRIMARY KEY,

    type            VARCHAR(50)  NOT NULL,   -- PARTY | FINISH
    source_text     VARCHAR(255) NOT NULL,   -- English lookup key
    hindi           VARCHAR(255),
    gujarati        VARCHAR(255),

    created_at      TIMESTAMP(6),
    last_updated_at TIMESTAMP(6),

    CONSTRAINT uq_translation_type_source UNIQUE (type, source_text)
);

CREATE INDEX IF NOT EXISTS ix_translation_type
    ON translation (type);
