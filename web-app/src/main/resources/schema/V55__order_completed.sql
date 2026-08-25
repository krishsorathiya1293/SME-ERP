-- ============================================================
-- Marking an order done by hand.
--
-- The sheet gets congested. An order can be finished in the
-- works' eyes -- goods gone, nothing owed -- while the figures
-- still read as open: a line dispatched short by agreement, a
-- balance written off, scrap settled against the next order.
-- The derived status cannot know any of that, because none of
-- it is a movement anybody records.
--
-- So this is a plain, manual tick. It says "I am done with
-- this one, take it off my sheet" and nothing more. It is
-- deliberately NOT part of OrderStatus: the derived status
-- must keep reporting what the works actually did, and a
-- supervisor tidying their screen must never be able to
-- rewrite that -- the client portal reads it.
-- ============================================================

ALTER TABLE orders ADD COLUMN IF NOT EXISTS completed BOOLEAN NOT NULL DEFAULT FALSE;

-- The sheet asks for "not completed" on every load, and a party's completed orders only when the
-- user goes looking for them.
CREATE INDEX IF NOT EXISTS ix_orders_completed ON orders (completed);
