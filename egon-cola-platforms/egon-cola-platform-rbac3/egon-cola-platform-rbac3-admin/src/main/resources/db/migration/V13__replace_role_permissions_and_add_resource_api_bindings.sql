-- RBAC3 V13 is the destructive cutover from permission-character grants to
-- resource grants.  V1-V12 remain immutable; no old authorization rows are
-- copied into either of the new tables.

DROP TABLE IF EXISTS rbac3_role_permission CASCADE;
DROP TABLE IF EXISTS rbac3_permission_resource CASCADE;

ALTER TABLE rbac3_resource
    ADD COLUMN IF NOT EXISTS suggested_permission_code VARCHAR(256);

ALTER TABLE rbac3_resource
    ADD CONSTRAINT uq_rbac3_resource_application_id_v13
        UNIQUE (application_id, id);

ALTER TABLE rbac3_resource
    DROP CONSTRAINT IF EXISTS ck_rbac3_resource_source;
ALTER TABLE rbac3_resource
    ADD CONSTRAINT ck_rbac3_resource_source_v13
        CHECK (source_type IN ('MANUAL', 'CI_REGISTRATION'));

ALTER TABLE rbac3_permission
    DROP CONSTRAINT IF EXISTS ck_rbac3_permission_source;
ALTER TABLE rbac3_permission
    ADD CONSTRAINT ck_rbac3_permission_source_v13
        CHECK (source_type IN ('MANUAL', 'CI_REGISTRATION'));

ALTER TABLE rbac3_field_definition
    DROP CONSTRAINT IF EXISTS ck_rbac3_field_definition_source;
ALTER TABLE rbac3_field_definition
    ADD CONSTRAINT ck_rbac3_field_definition_source_v13
        CHECK (source_type IN ('MANUAL', 'CI_REGISTRATION'));

UPDATE rbac3_resource
   SET status = 'PENDING_VALIDATION',
       updated_at = CURRENT_TIMESTAMP,
       updated_by = 'flyway-v13'
 WHERE resource_type IN ('ROUTE', 'ACTION', 'API')
   AND status = 'ACTIVE'
   AND required_permission_id IS NULL;

CREATE TABLE rbac3_role_resource_grant (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    resource_id BIGINT NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_to TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_role_resource_grant_fact
        UNIQUE (tenant_id, role_id, resource_id, valid_from),
    CONSTRAINT fk_rbac3_role_resource_grant_role
        FOREIGN KEY (tenant_id, application_id, role_id)
        REFERENCES rbac3_role(tenant_id, application_id, id),
    CONSTRAINT fk_rbac3_role_resource_grant_resource
        FOREIGN KEY (application_id, resource_id)
        REFERENCES rbac3_resource(application_id, id),
    CONSTRAINT ck_rbac3_role_resource_grant_status
        CHECK (status IN ('ACTIVE', 'DISABLED', 'EXPIRED')),
    CONSTRAINT ck_rbac3_role_resource_grant_window
        CHECK (valid_to IS NULL OR valid_to > valid_from),
    CONSTRAINT ck_rbac3_role_resource_grant_version
        CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_role_resource_grant_role_status_window
    ON rbac3_role_resource_grant
        (tenant_id, role_id, status, valid_from, valid_to);
CREATE INDEX idx_rbac3_role_resource_grant_resource_status
    ON rbac3_role_resource_grant
        (application_id, resource_id, status);

CREATE TABLE rbac3_resource_api_binding (
    id BIGINT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    source_resource_id BIGINT NOT NULL,
    api_resource_id BIGINT NOT NULL,
    source_build_id VARCHAR(256) NOT NULL,
    source_checksum VARCHAR(256) NOT NULL,
    status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_resource_api_binding_pair
        UNIQUE (source_resource_id, api_resource_id),
    CONSTRAINT fk_rbac3_resource_api_binding_source
        FOREIGN KEY (application_id, source_resource_id)
        REFERENCES rbac3_resource(application_id, id),
    CONSTRAINT fk_rbac3_resource_api_binding_api
        FOREIGN KEY (application_id, api_resource_id)
        REFERENCES rbac3_resource(application_id, id),
    CONSTRAINT ck_rbac3_resource_api_binding_distinct
        CHECK (source_resource_id <> api_resource_id),
    CONSTRAINT ck_rbac3_resource_api_binding_status
        CHECK (status IN ('ACTIVE', 'STALE', 'DISABLED')),
    CONSTRAINT ck_rbac3_resource_api_binding_version
        CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_resource_api_binding_application_source_status
    ON rbac3_resource_api_binding (application_id, source_resource_id, status);
CREATE INDEX idx_rbac3_resource_api_binding_application_api_status
    ON rbac3_resource_api_binding (application_id, api_resource_id, status);
