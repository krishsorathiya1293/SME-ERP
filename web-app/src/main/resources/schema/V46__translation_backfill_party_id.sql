-- ============================================================
-- translation.party_id — robust backfill
-- V45 added party_id and backfilled it by an EXACT name match
-- (source_text = party.name), which silently skipped any row
-- whose name differed by a stray space or letter case. Those
-- rows kept party_id = NULL, so the Job Work / Gres print (which
-- matches a party to its translation by party_id) found nothing
-- and rendered the party line blank. This re-runs the backfill
-- space/case-insensitively so every party links to its row.
--
-- Collision-proof so it can never fail the boot-time migration:
--   * only rows still unlinked (party_id IS NULL) are touched,
--   * a party that already has a linked row is left alone,
--   * at most ONE row is linked per party (DISTINCT ON),
-- so the unique index on party_id can never be violated.
-- Idempotent: re-running changes nothing once everything is linked.
-- ============================================================

WITH candidate AS (
    SELECT DISTINCT ON (p.id) t.id AS translation_id, p.id AS party_id
    FROM translation t
    JOIN party p
      ON lower(btrim(t.source_text)) = lower(btrim(p.name))
    WHERE t.type = 'PARTY'
      AND t.party_id IS NULL
      AND NOT EXISTS (
          SELECT 1 FROM translation x WHERE x.party_id = p.id
      )
    ORDER BY p.id, t.id
)
UPDATE translation t
SET party_id = c.party_id
FROM candidate c
WHERE t.id = c.translation_id;
