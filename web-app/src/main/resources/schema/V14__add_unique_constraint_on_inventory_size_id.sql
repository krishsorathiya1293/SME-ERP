-- Add unique constraint on size_id to support @OneToOne(unique = true)

ALTER TABLE inventory
    ADD CONSTRAINT uk_inventory_size_id
        UNIQUE (size_id);