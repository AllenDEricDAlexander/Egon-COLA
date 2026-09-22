CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    deleted_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    code VARCHAR(64)
);
CREATE UNIQUE INDEX orders_lifecycle ON orders(tenant_id, code, deleted_at);
CREATE UNIQUE INDEX orders_active ON orders(tenant_id, code) WHERE deleted_at IS NULL;
COMMENT ON COLUMN orders.code IS '业务代码与 Unicode 转义样本';
