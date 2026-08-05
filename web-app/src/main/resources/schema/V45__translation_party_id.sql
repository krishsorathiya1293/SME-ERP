-- ============================================================
-- translation.party_id
-- PARTY translations now key on the party's id instead of the
-- party name, so a rename / trailing space / casing difference
-- can never make the Job Work (or Gres) print miss the saved
-- Hindi/Gujarati and render blank. FINISH rows have no id and
-- keep keying on source_text (a fixed option list).
-- ============================================================

ALTER TABLE translation
    ADD COLUMN IF NOT EXISTS party_id BIGINT;

-- Backfill existing PARTY rows to their party by current name (best effort).
-- Rows that no longer match a party name stay unlinked (party_id NULL) and
-- become invisible to the editor; a later same-name ensure re-adopts them.
UPDATE translation t
SET party_id = p.id
FROM party p
WHERE t.type = 'PARTY'
  AND t.party_id IS NULL
  AND t.source_text = p.name;

-- The old (type, source_text) unique can't hold once parties key on id:
-- two distinct parties may legitimately share a name.
ALTER TABLE translation
    DROP CONSTRAINT IF EXISTS uq_translation_type_source;

-- One dictionary row per party. FINISH rows have party_id NULL, and Postgres
-- treats NULLs as distinct, so they never collide on this index.
CREATE UNIQUE INDEX IF NOT EXISTS uq_translation_party_id
    ON translation (party_id) WHERE party_id IS NOT NULL;

-- FINISH rows keep unique text keying.
CREATE UNIQUE INDEX IF NOT EXISTS uq_translation_finish_source
    ON translation (type, source_text) WHERE type = 'FINISH';
