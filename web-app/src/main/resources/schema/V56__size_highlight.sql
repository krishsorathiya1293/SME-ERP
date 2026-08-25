-- ============================================================
-- Highlighting a row of the stock master.
--
-- The grid is read the way a spreadsheet is read, so it is
-- marked up the way one is: colour a row, and write down why.
-- The note is the half that matters -- a yellow row nobody can
-- explain is worse than no colour at all -- so the two are
-- stored together and set together.
--
-- Held on the size rather than on the stock entry because a
-- row of that grid IS a size: the stock entry is optional
-- (610 of 611 rows had none when this was written) and a row
-- with nothing in stock is exactly the kind a supervisor
-- wants to flag.
-- ============================================================

-- One of a small fixed set of names (amber/green/rose/blue), not a hex value: the palette belongs
-- to the console's tokens, so a row keeps reading correctly if the theme changes.
ALTER TABLE size_inventory ADD COLUMN IF NOT EXISTS highlight_color VARCHAR(16);

-- Why the row is marked. Free text, shown on hover and in the highlight popover.
ALTER TABLE size_inventory ADD COLUMN IF NOT EXISTS highlight_note TEXT;
