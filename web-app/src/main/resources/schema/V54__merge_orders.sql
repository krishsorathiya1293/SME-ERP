-- ============================================================
-- Merging orders, not chitthis.
--
-- Two orders from the same party for the same item, size and
-- finish are one job on the floor -- 100 Kg and 200 Kg go into
-- the same drum. Until now that could only be said at the
-- moment a chitthi was raised, and had to be said again for
-- every chitthi. It is a fact about the orders, so it is
-- recorded on them once and everything downstream follows.
--
-- Nothing is summed in place and nothing is deleted. A merge
-- creates a NEW order carrying the combined lines, and points
-- the sources at it. The sources keep every row exactly as the
-- party placed it, which is what lets the client portal still
-- report each request against what that request actually
-- ordered -- and what makes un-merging a matter of dropping the
-- merged order rather than reconstructing anything.
-- ============================================================

-- Set on a source order once it has been folded into another. Listings hide these; the merged
-- order stands in for them.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS merged_into_id BIGINT;

-- Set on a source line, pointing at the merged line that now carries its quantity. The line's own
-- qty_pc / qty_kg are left untouched -- that is the party's order, and the share this line holds
-- of the merged line is computed from it.
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS merged_into_item_id BIGINT;

-- ON DELETE SET NULL, deliberately: deleting the merged order un-merges its sources rather than
-- taking them down with it. A merge is never allowed to destroy the orders it was made from.
ALTER TABLE orders
    DROP CONSTRAINT IF EXISTS fk_orders_merged_into;
ALTER TABLE orders
    ADD CONSTRAINT fk_orders_merged_into
        FOREIGN KEY (merged_into_id) REFERENCES orders (id) ON DELETE SET NULL;

ALTER TABLE order_items
    DROP CONSTRAINT IF EXISTS fk_order_items_merged_into_item;
ALTER TABLE order_items
    ADD CONSTRAINT fk_order_items_merged_into_item
        FOREIGN KEY (merged_into_item_id) REFERENCES order_items (id) ON DELETE SET NULL;

-- Every listing filters on "not merged away", and the merged order has to find its sources.
CREATE INDEX IF NOT EXISTS ix_orders_merged_into ON orders (merged_into_id);
CREATE INDEX IF NOT EXISTS ix_order_items_merged_into_item ON order_items (merged_into_item_id);
