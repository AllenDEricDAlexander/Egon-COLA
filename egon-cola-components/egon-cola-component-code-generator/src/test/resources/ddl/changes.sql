ALTER TABLE orders ADD COLUMN note VARCHAR(160);
ALTER TABLE orders RENAME COLUMN code TO order_code;
ALTER TABLE orders ALTER COLUMN note SET NOT NULL;
-- Tests replay supported variants independently and assert destructive impacts instead of writing live schema.
