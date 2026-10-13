-- Editing an invoice used to copy the request's (always empty) invoice_type onto the row, leaving
-- it NULL. One save writes its rows in order EXPORT, COMMERCIAL, PACKAGING_LIST, so a row's place
-- within its group gives its type back.
WITH ranked AS (SELECT id,
                       ROW_NUMBER() OVER (PARTITION BY group_id ORDER BY id) AS pos,
                       COUNT(*) OVER (PARTITION BY group_id)                 AS n
                FROM invoices
                WHERE group_id IS NOT NULL)
UPDATE invoices i
SET invoice_type = CASE r.pos WHEN 1 THEN 'EXPORT' WHEN 2 THEN 'COMMERCIAL' ELSE 'PACKAGING_LIST' END
FROM ranked r
WHERE i.id = r.id
  AND r.n = 3
  AND i.invoice_type IS NULL;
