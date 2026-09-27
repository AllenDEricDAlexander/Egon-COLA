ALTER TABLE orders ADD COLUMN note VARCHAR(160);
ALTER TABLE orders RENAME COLUMN code TO order_code;
ALTER TABLE orders ALTER COLUMN note SET NOT NULL;
-- Regression input: structural changes must be rejected by the CREATE-only generator.
