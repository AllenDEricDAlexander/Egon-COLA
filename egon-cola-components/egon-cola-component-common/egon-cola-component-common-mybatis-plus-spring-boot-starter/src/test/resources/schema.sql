CREATE TABLE test_business_record (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    update_user_id VARCHAR(128) NOT NULL,
    update_time TIMESTAMP NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    title VARCHAR(255) NOT NULL,
    payload VARCHAR(1024),
    version BIGINT
);

CREATE INDEX idx_test_business_tenant_deleted
    ON test_business_record (tenant_id, is_deleted);

CREATE TABLE test_global_record (
    id BIGINT PRIMARY KEY,
    payload VARCHAR(255)
);
