CREATE TABLE test_business_record (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    update_user_id VARCHAR(128) NOT NULL,
    update_time TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
    title VARCHAR(255) NOT NULL,
    payload VARCHAR(1024),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_test_business_tenant_deleted
    ON test_business_record (tenant_id, deleted_at);

CREATE TABLE test_global_record (
    id BIGINT PRIMARY KEY,
    payload VARCHAR(255)
);
