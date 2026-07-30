CREATE TABLE rbac3_tenant (
    id BIGINT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(200) NOT NULL,
    status VARCHAR(32) NOT NULL,
    policy_version BIGINT NOT NULL DEFAULT 0,
    settings JSONB NOT NULL DEFAULT '{}'::jsonb,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_tenant_id UNIQUE (id),
    CONSTRAINT ck_rbac3_tenant_status
        CHECK (status IN ('INITIALIZING', 'ACTIVE', 'SUSPENDED', 'CLOSED')),
    CONSTRAINT ck_rbac3_tenant_policy_version CHECK (policy_version >= 0),
    CONSTRAINT ck_rbac3_tenant_version CHECK (version >= 0)
);

CREATE UNIQUE INDEX uk_rbac3_tenant_code_lower
    ON rbac3_tenant (lower(code));
CREATE INDEX idx_rbac3_tenant_status ON rbac3_tenant (status);
CREATE INDEX idx_rbac3_tenant_updated ON rbac3_tenant (updated_at);

CREATE TABLE rbac3_user (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    username VARCHAR(128) NOT NULL,
    normalized_username VARCHAR(128) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    status VARCHAR(32) NOT NULL,
    auth_version BIGINT NOT NULL DEFAULT 0,
    primary_org_unit_id BIGINT,
    primary_position_id BIGINT,
    directory_snapshot_version BIGINT NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    archived_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_user_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_user_username UNIQUE (tenant_id, normalized_username),
    CONSTRAINT fk_rbac3_user_tenant
        FOREIGN KEY (tenant_id) REFERENCES rbac3_tenant(id),
    CONSTRAINT ck_rbac3_user_status
        CHECK (status IN ('INVITED', 'ACTIVE', 'LOCKED', 'DISABLED', 'ARCHIVED')),
    CONSTRAINT ck_rbac3_user_auth_version CHECK (auth_version >= 0),
    CONSTRAINT ck_rbac3_user_directory_version
        CHECK (directory_snapshot_version >= 0),
    CONSTRAINT ck_rbac3_user_version CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_user_tenant_status
    ON rbac3_user (tenant_id, status);
CREATE INDEX idx_rbac3_user_primary_org
    ON rbac3_user (tenant_id, primary_org_unit_id);

CREATE TABLE rbac3_user_credential (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    credential_type VARCHAR(32) NOT NULL,
    password_hash VARCHAR(512),
    credential_version BIGINT NOT NULL DEFAULT 0,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    password_changed_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_user_credential_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_user_credential_type
        UNIQUE (tenant_id, user_id, credential_type),
    CONSTRAINT fk_rbac3_user_credential_user
        FOREIGN KEY (tenant_id, user_id)
        REFERENCES rbac3_user(tenant_id, id),
    CONSTRAINT ck_rbac3_user_credential_type
        CHECK (credential_type IN ('PASSWORD')),
    CONSTRAINT ck_rbac3_user_credential_password
        CHECK (credential_type <> 'PASSWORD' OR password_hash IS NOT NULL),
    CONSTRAINT ck_rbac3_user_credential_status
        CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED', 'EXPIRED')),
    CONSTRAINT ck_rbac3_user_credential_version
        CHECK (credential_version >= 0 AND version >= 0),
    CONSTRAINT ck_rbac3_user_credential_attempts CHECK (failed_attempts >= 0)
);

CREATE INDEX idx_rbac3_user_credential_user_status
    ON rbac3_user_credential (tenant_id, user_id, status);

CREATE TABLE rbac3_external_identity (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    provider_code VARCHAR(128) NOT NULL,
    external_subject_id VARCHAR(256) NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    sync_version BIGINT NOT NULL DEFAULT 0,
    last_synced_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_external_identity_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_external_identity_subject
        UNIQUE (tenant_id, provider_code, external_subject_id),
    CONSTRAINT fk_rbac3_external_identity_user
        FOREIGN KEY (tenant_id, user_id)
        REFERENCES rbac3_user(tenant_id, id),
    CONSTRAINT ck_rbac3_external_identity_status
        CHECK (status IN ('ACTIVE', 'DISABLED', 'STALE')),
    CONSTRAINT ck_rbac3_external_identity_version
        CHECK (sync_version >= 0 AND version >= 0)
);

CREATE INDEX idx_rbac3_external_identity_user
    ON rbac3_external_identity (tenant_id, user_id);

CREATE TABLE rbac3_directory_snapshot (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    provider_code VARCHAR(128) NOT NULL,
    snapshot_version BIGINT NOT NULL,
    checksum VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    activated_at TIMESTAMPTZ,
    payload JSONB NOT NULL,
    counts JSONB NOT NULL DEFAULT '{}'::jsonb,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_directory_snapshot_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_directory_snapshot_version
        UNIQUE (tenant_id, provider_code, snapshot_version),
    CONSTRAINT fk_rbac3_directory_snapshot_tenant
        FOREIGN KEY (tenant_id) REFERENCES rbac3_tenant(id),
    CONSTRAINT ck_rbac3_directory_snapshot_status
        CHECK (status IN ('RECEIVED', 'VALIDATED', 'ACTIVE', 'REJECTED', 'ARCHIVED')),
    CONSTRAINT ck_rbac3_directory_snapshot_version
        CHECK (snapshot_version >= 0 AND version >= 0)
);

CREATE INDEX idx_rbac3_directory_snapshot_active
    ON rbac3_directory_snapshot
        (tenant_id, provider_code, status, activated_at DESC);

CREATE TABLE rbac3_org_unit (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    snapshot_id BIGINT NOT NULL,
    unit_type VARCHAR(32) NOT NULL,
    code VARCHAR(128) NOT NULL,
    name VARCHAR(200) NOT NULL,
    parent_id BIGINT,
    path TEXT NOT NULL,
    depth INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    external_id VARCHAR(256),
    valid_from TIMESTAMPTZ NOT NULL,
    valid_to TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_org_unit_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_org_unit_code UNIQUE (tenant_id, code),
    CONSTRAINT fk_rbac3_org_unit_snapshot
        FOREIGN KEY (tenant_id, snapshot_id)
        REFERENCES rbac3_directory_snapshot(tenant_id, id),
    CONSTRAINT fk_rbac3_org_unit_parent
        FOREIGN KEY (tenant_id, parent_id)
        REFERENCES rbac3_org_unit(tenant_id, id),
    CONSTRAINT ck_rbac3_org_unit_type
        CHECK (unit_type IN ('ORG', 'DEPT')),
    CONSTRAINT ck_rbac3_org_unit_depth CHECK (depth BETWEEN 0 AND 20),
    CONSTRAINT ck_rbac3_org_unit_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_rbac3_org_unit_window
        CHECK (valid_to IS NULL OR valid_to > valid_from),
    CONSTRAINT ck_rbac3_org_unit_version CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_org_unit_parent_status
    ON rbac3_org_unit (tenant_id, parent_id, status);
CREATE INDEX idx_rbac3_org_unit_path ON rbac3_org_unit (tenant_id, path);

CREATE TABLE rbac3_position (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    snapshot_id BIGINT NOT NULL,
    code VARCHAR(128) NOT NULL,
    name VARCHAR(200) NOT NULL,
    org_unit_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    external_id VARCHAR(256),
    valid_from TIMESTAMPTZ NOT NULL,
    valid_to TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_position_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_position_code UNIQUE (tenant_id, code),
    CONSTRAINT fk_rbac3_position_snapshot
        FOREIGN KEY (tenant_id, snapshot_id)
        REFERENCES rbac3_directory_snapshot(tenant_id, id),
    CONSTRAINT fk_rbac3_position_org_unit
        FOREIGN KEY (tenant_id, org_unit_id)
        REFERENCES rbac3_org_unit(tenant_id, id),
    CONSTRAINT ck_rbac3_position_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_rbac3_position_window
        CHECK (valid_to IS NULL OR valid_to > valid_from),
    CONSTRAINT ck_rbac3_position_version CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_position_org_status
    ON rbac3_position (tenant_id, org_unit_id, status);

CREATE TABLE rbac3_user_position_snapshot (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    snapshot_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    position_id BIGINT NOT NULL,
    org_unit_id BIGINT NOT NULL,
    primary_flag BOOLEAN NOT NULL DEFAULT FALSE,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_to TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL,
    external_assignment_id VARCHAR(256),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_user_position_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_user_position_snapshot
        UNIQUE (tenant_id, snapshot_id, user_id, position_id, valid_from),
    CONSTRAINT fk_rbac3_user_position_snapshot
        FOREIGN KEY (tenant_id, snapshot_id)
        REFERENCES rbac3_directory_snapshot(tenant_id, id),
    CONSTRAINT fk_rbac3_user_position_user
        FOREIGN KEY (tenant_id, user_id)
        REFERENCES rbac3_user(tenant_id, id),
    CONSTRAINT fk_rbac3_user_position_position
        FOREIGN KEY (tenant_id, position_id)
        REFERENCES rbac3_position(tenant_id, id),
    CONSTRAINT fk_rbac3_user_position_org
        FOREIGN KEY (tenant_id, org_unit_id)
        REFERENCES rbac3_org_unit(tenant_id, id),
    CONSTRAINT ck_rbac3_user_position_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_rbac3_user_position_window
        CHECK (valid_to IS NULL OR valid_to > valid_from),
    CONSTRAINT ck_rbac3_user_position_version CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_user_position_user_window
    ON rbac3_user_position_snapshot
        (tenant_id, user_id, status, valid_from, valid_to);
CREATE INDEX idx_rbac3_user_position_position
    ON rbac3_user_position_snapshot (tenant_id, position_id, status);

CREATE TABLE rbac3_service_principal (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    service_code VARCHAR(128) NOT NULL,
    application_code VARCHAR(128) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    status VARCHAR(32) NOT NULL,
    allowed_envs JSONB NOT NULL DEFAULT '[]'::jsonb,
    allowed_namespaces JSONB NOT NULL DEFAULT '[]'::jsonb,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_service_principal_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_service_principal_application_id
        UNIQUE (tenant_id, application_code, id),
    CONSTRAINT uq_rbac3_service_principal_code
        UNIQUE (tenant_id, service_code),
    CONSTRAINT fk_rbac3_service_principal_tenant
        FOREIGN KEY (tenant_id) REFERENCES rbac3_tenant(id),
    CONSTRAINT ck_rbac3_service_principal_status
        CHECK (status IN ('ACTIVE', 'DISABLED', 'ARCHIVED')),
    CONSTRAINT ck_rbac3_service_principal_version CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_service_principal_application
    ON rbac3_service_principal (tenant_id, application_code, status);

CREATE TABLE rbac3_service_credential (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    principal_id BIGINT NOT NULL,
    credential_id VARCHAR(128) NOT NULL,
    credential_type VARCHAR(32) NOT NULL,
    secret_hash VARCHAR(512),
    public_key TEXT,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_to TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL,
    last_used_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_service_credential_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_service_credential_key
        UNIQUE (tenant_id, credential_id),
    CONSTRAINT fk_rbac3_service_credential_principal
        FOREIGN KEY (tenant_id, principal_id)
        REFERENCES rbac3_service_principal(tenant_id, id),
    CONSTRAINT ck_rbac3_service_credential_material CHECK (
        (credential_type = 'CLIENT_SECRET'
            AND secret_hash IS NOT NULL AND public_key IS NULL)
        OR (credential_type = 'PUBLIC_KEY'
            AND secret_hash IS NULL AND public_key IS NOT NULL)
    ),
    CONSTRAINT ck_rbac3_service_credential_status
        CHECK (status IN ('ACTIVE', 'DISABLED', 'EXPIRED', 'REVOKED')),
    CONSTRAINT ck_rbac3_service_credential_window
        CHECK (valid_to IS NULL OR valid_to > valid_from),
    CONSTRAINT ck_rbac3_service_credential_version CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_service_credential_principal
    ON rbac3_service_credential (tenant_id, principal_id, status);

CREATE TABLE rbac3_service_permission (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    principal_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    application_code VARCHAR(128) NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_to TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_service_permission_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_service_permission_fact
        UNIQUE (tenant_id, principal_id, permission_id, application_code),
    CONSTRAINT fk_rbac3_service_permission_principal
        FOREIGN KEY (tenant_id, application_code, principal_id)
        REFERENCES rbac3_service_principal(tenant_id, application_code, id),
    CONSTRAINT ck_rbac3_service_permission_window
        CHECK (valid_to IS NULL OR valid_to > valid_from),
    CONSTRAINT ck_rbac3_service_permission_version CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_service_permission_principal_window
    ON rbac3_service_permission
        (tenant_id, principal_id, valid_from, valid_to);

CREATE TABLE rbac3_application (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    application_code VARCHAR(128) NOT NULL,
    application_name VARCHAR(200) NOT NULL,
    display_priority INTEGER NOT NULL DEFAULT 1000,
    status VARCHAR(32) NOT NULL,
    current_manifest_id BIGINT,
    current_manifest_version BIGINT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_application_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_application_identity
        UNIQUE (tenant_id, id, application_code),
    CONSTRAINT uq_rbac3_application_code
        UNIQUE (tenant_id, application_code),
    CONSTRAINT fk_rbac3_application_tenant
        FOREIGN KEY (tenant_id) REFERENCES rbac3_tenant(id),
    CONSTRAINT ck_rbac3_application_status
        CHECK (status IN ('ACTIVE', 'DISABLED', 'ARCHIVED')),
    CONSTRAINT ck_rbac3_application_priority CHECK (display_priority >= 0),
    CONSTRAINT ck_rbac3_application_manifest_version
        CHECK (current_manifest_version IS NULL OR current_manifest_version >= 0),
    CONSTRAINT ck_rbac3_application_version CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_application_status_priority
    ON rbac3_application (tenant_id, status, display_priority);

CREATE TABLE rbac3_resource_manifest (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    schema_version INTEGER NOT NULL,
    artifact_version VARCHAR(128) NOT NULL,
    build_id VARCHAR(256) NOT NULL,
    manifest_version BIGINT NOT NULL,
    checksum VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    definition_set_id VARCHAR(64),
    payload JSONB NOT NULL,
    validation_result JSONB NOT NULL DEFAULT '{}'::jsonb,
    received_at TIMESTAMPTZ NOT NULL,
    activated_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_manifest_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_manifest_application_id
        UNIQUE (tenant_id, application_id, id),
    CONSTRAINT uq_rbac3_manifest_build
        UNIQUE (tenant_id, application_id, artifact_version, build_id),
    CONSTRAINT uq_rbac3_manifest_version
        UNIQUE (tenant_id, application_id, manifest_version),
    CONSTRAINT fk_rbac3_manifest_application
        FOREIGN KEY (tenant_id, application_id)
        REFERENCES rbac3_application(tenant_id, id),
    CONSTRAINT ck_rbac3_manifest_status
        CHECK (status IN ('PENDING_VALIDATION', 'ACTIVE', 'SUPERSEDED')),
    CONSTRAINT ck_rbac3_manifest_versions CHECK (
        schema_version > 0 AND manifest_version >= 0 AND version >= 0
    )
);

CREATE INDEX idx_rbac3_manifest_status_received
    ON rbac3_resource_manifest
        (tenant_id, application_id, status, received_at DESC);

CREATE TABLE rbac3_resource (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    resource_code VARCHAR(128) NOT NULL,
    resource_name VARCHAR(200) NOT NULL,
    parent_resource_id BIGINT,
    required_permission_id BIGINT,
    status VARCHAR(32) NOT NULL,
    source_manifest_id BIGINT,
    source_build_id VARCHAR(256),
    mechanical_facts JSONB NOT NULL DEFAULT '{}'::jsonb,
    display_metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    stale_since TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_resource_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_resource_application_id
        UNIQUE (tenant_id, application_id, id),
    CONSTRAINT uq_rbac3_resource_application_type_id
        UNIQUE (tenant_id, application_id, id, resource_type),
    CONSTRAINT uq_rbac3_resource_code
        UNIQUE (tenant_id, application_id, resource_type, resource_code),
    CONSTRAINT fk_rbac3_resource_application
        FOREIGN KEY (tenant_id, application_id)
        REFERENCES rbac3_application(tenant_id, id),
    CONSTRAINT fk_rbac3_resource_parent
        FOREIGN KEY (tenant_id, application_id, parent_resource_id)
        REFERENCES rbac3_resource(tenant_id, application_id, id),
    CONSTRAINT fk_rbac3_resource_manifest
        FOREIGN KEY (tenant_id, application_id, source_manifest_id)
        REFERENCES rbac3_resource_manifest(tenant_id, application_id, id),
    CONSTRAINT ck_rbac3_resource_type
        CHECK (resource_type IN ('APP', 'MENU', 'ROUTE', 'ACTION', 'API')),
    CONSTRAINT ck_rbac3_resource_status
        CHECK (status IN ('PENDING_VALIDATION', 'ACTIVE', 'STALE', 'ARCHIVED')),
    CONSTRAINT ck_rbac3_resource_version CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_resource_application_status
    ON rbac3_resource (tenant_id, application_id, status, resource_type);
CREATE INDEX idx_rbac3_resource_required_permission
    ON rbac3_resource (tenant_id, required_permission_id);

CREATE TABLE rbac3_permission (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    permission_code VARCHAR(128) NOT NULL,
    permission_name VARCHAR(200) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    description TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_permission_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_permission_application_id
        UNIQUE (tenant_id, application_id, id),
    CONSTRAINT uq_rbac3_permission_code UNIQUE (tenant_id, permission_code),
    CONSTRAINT fk_rbac3_permission_application
        FOREIGN KEY (tenant_id, application_id)
        REFERENCES rbac3_application(tenant_id, id),
    CONSTRAINT ck_rbac3_permission_risk
        CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_rbac3_permission_status
        CHECK (status IN ('ACTIVE', 'DEPRECATED', 'ARCHIVED')),
    CONSTRAINT ck_rbac3_permission_version CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_permission_application_status
    ON rbac3_permission (tenant_id, application_id, status);

CREATE TABLE rbac3_permission_resource (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    resource_id BIGINT NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    definition_set_id VARCHAR(64),
    gateway_operation_id VARCHAR(64),
    security_policy_id VARCHAR(128),
    mapping_version BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_permission_resource_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_permission_resource_mapping
        UNIQUE (tenant_id, resource_id, mapping_version),
    CONSTRAINT fk_rbac3_permission_resource_permission
        FOREIGN KEY (tenant_id, application_id, permission_id)
        REFERENCES rbac3_permission(tenant_id, application_id, id),
    CONSTRAINT fk_rbac3_permission_resource_resource
        FOREIGN KEY (tenant_id, application_id, resource_id, resource_type)
        REFERENCES rbac3_resource(tenant_id, application_id, id, resource_type),
    CONSTRAINT ck_rbac3_permission_resource_api_identity CHECK (
        (resource_type = 'API'
            AND definition_set_id IS NOT NULL
            AND gateway_operation_id IS NOT NULL)
        OR (resource_type <> 'API'
            AND definition_set_id IS NULL
            AND gateway_operation_id IS NULL)
    ),
    CONSTRAINT ck_rbac3_permission_resource_status
        CHECK (status IN ('ACTIVE', 'STALE', 'DISABLED')),
    CONSTRAINT ck_rbac3_permission_resource_version
        CHECK (mapping_version >= 0 AND version >= 0)
);

CREATE UNIQUE INDEX uk_rbac3_permission_resource_api_operation
    ON rbac3_permission_resource (
        tenant_id, definition_set_id, gateway_operation_id, mapping_version
    )
    WHERE definition_set_id IS NOT NULL AND gateway_operation_id IS NOT NULL;
CREATE INDEX idx_rbac3_permission_resource_operation
    ON rbac3_permission_resource
        (tenant_id, gateway_operation_id, definition_set_id, status);

CREATE TABLE rbac3_role (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    role_code VARCHAR(128) NOT NULL,
    role_name VARCHAR(200) NOT NULL,
    role_type VARCHAR(32) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    privileged BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL,
    landing_route_id BIGINT,
    landing_priority INTEGER NOT NULL DEFAULT 1000,
    max_assignment_days INTEGER,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_role_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_role_application_id
        UNIQUE (tenant_id, application_id, id),
    CONSTRAINT uq_rbac3_role_code
        UNIQUE (tenant_id, application_id, role_code),
    CONSTRAINT fk_rbac3_role_application
        FOREIGN KEY (tenant_id, application_id)
        REFERENCES rbac3_application(tenant_id, id),
    CONSTRAINT fk_rbac3_role_landing_route
        FOREIGN KEY (tenant_id, application_id, landing_route_id)
        REFERENCES rbac3_resource(tenant_id, application_id, id),
    CONSTRAINT ck_rbac3_role_code
        CHECK (role_code ~ '^[A-Z][A-Z0-9_]{2,63}$'),
    CONSTRAINT ck_rbac3_role_type
        CHECK (role_type IN ('PUBLIC', 'POSITION', 'MANAGEMENT', 'TEMPORARY', 'EMERGENCY')),
    CONSTRAINT ck_rbac3_role_risk
        CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_rbac3_role_status
        CHECK (status IN ('ACTIVE', 'DISABLED', 'ARCHIVED')),
    CONSTRAINT ck_rbac3_role_landing_priority CHECK (landing_priority >= 0),
    CONSTRAINT ck_rbac3_role_max_days
        CHECK (max_assignment_days IS NULL OR max_assignment_days > 0),
    CONSTRAINT ck_rbac3_role_version CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_role_application_type_status
    ON rbac3_role (tenant_id, application_id, role_type, status);
CREATE INDEX idx_rbac3_role_privileged_status
    ON rbac3_role (tenant_id, privileged, status);

CREATE TABLE rbac3_role_inheritance (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    senior_role_id BIGINT NOT NULL,
    junior_role_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_role_inheritance_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_role_inheritance_edge
        UNIQUE (tenant_id, application_id, senior_role_id, junior_role_id),
    CONSTRAINT fk_rbac3_role_inheritance_senior
        FOREIGN KEY (tenant_id, application_id, senior_role_id)
        REFERENCES rbac3_role(tenant_id, application_id, id),
    CONSTRAINT fk_rbac3_role_inheritance_junior
        FOREIGN KEY (tenant_id, application_id, junior_role_id)
        REFERENCES rbac3_role(tenant_id, application_id, id),
    CONSTRAINT ck_rbac3_role_inheritance_distinct
        CHECK (senior_role_id <> junior_role_id),
    CONSTRAINT ck_rbac3_role_inheritance_version CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_role_inheritance_junior
    ON rbac3_role_inheritance
        (tenant_id, application_id, junior_role_id);

CREATE TABLE rbac3_role_closure (
    tenant_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    ancestor_role_id BIGINT NOT NULL,
    descendant_role_id BIGINT NOT NULL,
    depth INTEGER NOT NULL,
    CONSTRAINT pk_rbac3_role_closure PRIMARY KEY (
        tenant_id, application_id, ancestor_role_id, descendant_role_id
    ),
    CONSTRAINT fk_rbac3_role_closure_ancestor
        FOREIGN KEY (tenant_id, application_id, ancestor_role_id)
        REFERENCES rbac3_role(tenant_id, application_id, id),
    CONSTRAINT fk_rbac3_role_closure_descendant
        FOREIGN KEY (tenant_id, application_id, descendant_role_id)
        REFERENCES rbac3_role(tenant_id, application_id, id),
    CONSTRAINT ck_rbac3_role_closure_depth CHECK (depth BETWEEN 0 AND 10),
    CONSTRAINT ck_rbac3_role_closure_self
        CHECK ((ancestor_role_id = descendant_role_id) = (depth = 0))
);

CREATE INDEX idx_rbac3_closure_descendant_depth
    ON rbac3_role_closure
        (tenant_id, application_id, descendant_role_id, depth);
CREATE INDEX idx_rbac3_closure_ancestor_depth
    ON rbac3_role_closure
        (tenant_id, application_id, ancestor_role_id, depth);

CREATE TABLE rbac3_role_permission (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_to TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_role_permission_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_role_permission_fact
        UNIQUE (tenant_id, role_id, permission_id, valid_from),
    CONSTRAINT fk_rbac3_role_permission_role
        FOREIGN KEY (tenant_id, application_id, role_id)
        REFERENCES rbac3_role(tenant_id, application_id, id),
    CONSTRAINT fk_rbac3_role_permission_permission
        FOREIGN KEY (tenant_id, application_id, permission_id)
        REFERENCES rbac3_permission(tenant_id, application_id, id),
    CONSTRAINT ck_rbac3_role_permission_status
        CHECK (status IN ('ACTIVE', 'DISABLED', 'EXPIRED')),
    CONSTRAINT ck_rbac3_role_permission_window
        CHECK (valid_to IS NULL OR valid_to > valid_from),
    CONSTRAINT ck_rbac3_role_permission_version CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_role_permission_role_status
    ON rbac3_role_permission (tenant_id, role_id, status);
CREATE INDEX idx_rbac3_role_permission_permission_status
    ON rbac3_role_permission (tenant_id, permission_id, status);

CREATE TABLE rbac3_data_rule (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    scope_type VARCHAR(32) NOT NULL,
    directory_snapshot_version BIGINT,
    status VARCHAR(32) NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_to TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_data_rule_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_rbac3_data_rule_role
        FOREIGN KEY (tenant_id, application_id, role_id)
        REFERENCES rbac3_role(tenant_id, application_id, id),
    CONSTRAINT fk_rbac3_data_rule_permission
        FOREIGN KEY (tenant_id, application_id, permission_id)
        REFERENCES rbac3_permission(tenant_id, application_id, id),
    CONSTRAINT ck_rbac3_data_rule_scope
        CHECK (scope_type IN ('ALL', 'SELF', 'DEPT', 'DEPT_TREE', 'ORG', 'ORG_TREE', 'CUSTOM')),
    CONSTRAINT ck_rbac3_data_rule_status
        CHECK (status IN ('ACTIVE', 'DISABLED', 'EXPIRED')),
    CONSTRAINT ck_rbac3_data_rule_window
        CHECK (valid_to IS NULL OR valid_to > valid_from),
    CONSTRAINT ck_rbac3_data_rule_directory_version CHECK (
        directory_snapshot_version IS NULL OR directory_snapshot_version >= 0
    ),
    CONSTRAINT ck_rbac3_data_rule_version CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_data_rule_role_permission
    ON rbac3_data_rule (tenant_id, role_id, permission_id, status);

CREATE TABLE rbac3_data_rule_ref (
    tenant_id BIGINT NOT NULL,
    data_rule_id BIGINT NOT NULL,
    ref_type VARCHAR(32) NOT NULL,
    ref_id BIGINT NOT NULL,
    CONSTRAINT pk_rbac3_data_rule_ref PRIMARY KEY (
        tenant_id, data_rule_id, ref_type, ref_id
    ),
    CONSTRAINT fk_rbac3_data_rule_ref_rule
        FOREIGN KEY (tenant_id, data_rule_id)
        REFERENCES rbac3_data_rule(tenant_id, id),
    CONSTRAINT ck_rbac3_data_rule_ref_type
        CHECK (ref_type IN ('USER', 'DEPT', 'ORG', 'POSITION'))
);

CREATE INDEX idx_rbac3_data_rule_ref_lookup
    ON rbac3_data_rule_ref (tenant_id, ref_type, ref_id);

CREATE TABLE rbac3_field_definition (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    resource_id BIGINT NOT NULL,
    field_code VARCHAR(128) NOT NULL,
    json_path VARCHAR(512) NOT NULL,
    data_type VARCHAR(32) NOT NULL,
    sensitivity VARCHAR(32) NOT NULL,
    default_access VARCHAR(32) NOT NULL,
    masking_strategy VARCHAR(32),
    writable BOOLEAN NOT NULL DEFAULT FALSE,
    exportable BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL,
    source_manifest_id BIGINT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_field_definition_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_field_definition_application_id
        UNIQUE (tenant_id, application_id, id),
    CONSTRAINT uq_rbac3_field_definition_code
        UNIQUE (tenant_id, application_id, resource_id, field_code),
    CONSTRAINT fk_rbac3_field_definition_resource
        FOREIGN KEY (tenant_id, application_id, resource_id)
        REFERENCES rbac3_resource(tenant_id, application_id, id),
    CONSTRAINT fk_rbac3_field_definition_manifest
        FOREIGN KEY (tenant_id, application_id, source_manifest_id)
        REFERENCES rbac3_resource_manifest(tenant_id, application_id, id),
    CONSTRAINT ck_rbac3_field_definition_type
        CHECK (data_type IN ('STRING', 'NUMBER', 'BOOLEAN', 'DATE', 'DATETIME', 'OBJECT', 'ARRAY')),
    CONSTRAINT ck_rbac3_field_definition_sensitivity
        CHECK (sensitivity IN ('NORMAL', 'INTERNAL', 'CONFIDENTIAL', 'HIGH')),
    CONSTRAINT ck_rbac3_field_definition_access
        CHECK (default_access IN ('NONE', 'MASKED_READ', 'READ')),
    CONSTRAINT ck_rbac3_field_definition_status
        CHECK (status IN ('ACTIVE', 'STALE', 'DISABLED', 'ARCHIVED')),
    CONSTRAINT ck_rbac3_field_definition_version CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_field_definition_resource_status
    ON rbac3_field_definition (tenant_id, resource_id, status);

CREATE TABLE rbac3_field_rule (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    field_definition_id BIGINT NOT NULL,
    access_level VARCHAR(32) NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_to TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_field_rule_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_field_rule_fact UNIQUE (
        tenant_id, role_id, permission_id, field_definition_id, valid_from
    ),
    CONSTRAINT fk_rbac3_field_rule_role
        FOREIGN KEY (tenant_id, application_id, role_id)
        REFERENCES rbac3_role(tenant_id, application_id, id),
    CONSTRAINT fk_rbac3_field_rule_permission
        FOREIGN KEY (tenant_id, application_id, permission_id)
        REFERENCES rbac3_permission(tenant_id, application_id, id),
    CONSTRAINT fk_rbac3_field_rule_definition
        FOREIGN KEY (tenant_id, application_id, field_definition_id)
        REFERENCES rbac3_field_definition(tenant_id, application_id, id),
    CONSTRAINT ck_rbac3_field_rule_access
        CHECK (access_level IN ('NONE', 'MASKED_READ', 'READ', 'WRITE')),
    CONSTRAINT ck_rbac3_field_rule_status
        CHECK (status IN ('ACTIVE', 'DISABLED', 'EXPIRED')),
    CONSTRAINT ck_rbac3_field_rule_window
        CHECK (valid_to IS NULL OR valid_to > valid_from),
    CONSTRAINT ck_rbac3_field_rule_version CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_field_rule_role_permission
    ON rbac3_field_rule (tenant_id, role_id, permission_id, status);

CREATE TABLE rbac3_user_role_assignment (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assignment_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_to TIMESTAMPTZ,
    source_type VARCHAR(32) NOT NULL,
    source_id VARCHAR(128) NOT NULL,
    reason VARCHAR(500),
    ticket_no VARCHAR(128),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_assignment_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_rbac3_assignment_user
        FOREIGN KEY (tenant_id, user_id)
        REFERENCES rbac3_user(tenant_id, id),
    CONSTRAINT fk_rbac3_assignment_role
        FOREIGN KEY (tenant_id, role_id)
        REFERENCES rbac3_role(tenant_id, id),
    CONSTRAINT ck_rbac3_assignment_type
        CHECK (assignment_type IN ('AUTO', 'DIRECT', 'TEMPORARY', 'EMERGENCY')),
    CONSTRAINT ck_rbac3_assignment_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'EXPIRED', 'REVOKED')),
    CONSTRAINT ck_rbac3_assignment_window
        CHECK (valid_to IS NULL OR valid_to > valid_from),
    CONSTRAINT ck_rbac3_assignment_bounded
        CHECK (assignment_type NOT IN ('TEMPORARY', 'EMERGENCY')
            OR valid_to IS NOT NULL),
    CONSTRAINT ck_rbac3_assignment_version CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_assignment_user_active_window
    ON rbac3_user_role_assignment
        (tenant_id, user_id, status, valid_from, valid_to);
CREATE INDEX idx_rbac3_assignment_role_active_window
    ON rbac3_user_role_assignment
        (tenant_id, role_id, status, valid_from, valid_to);

CREATE TABLE rbac3_auto_assignment_rule (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    rule_code VARCHAR(128) NOT NULL,
    match_type VARCHAR(32) NOT NULL,
    match_ref_id BIGINT,
    role_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_to TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_auto_assignment_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_auto_assignment_code UNIQUE (tenant_id, rule_code),
    CONSTRAINT fk_rbac3_auto_assignment_role
        FOREIGN KEY (tenant_id, role_id)
        REFERENCES rbac3_role(tenant_id, id),
    CONSTRAINT fk_rbac3_auto_assignment_position
        FOREIGN KEY (tenant_id, match_ref_id)
        REFERENCES rbac3_position(tenant_id, id),
    CONSTRAINT ck_rbac3_auto_assignment_match CHECK (
        (match_type = 'ALL_ACTIVE_USERS' AND match_ref_id IS NULL)
        OR (match_type = 'POSITION' AND match_ref_id IS NOT NULL)
    ),
    CONSTRAINT ck_rbac3_auto_assignment_status
        CHECK (status IN ('ACTIVE', 'DISABLED', 'EXPIRED')),
    CONSTRAINT ck_rbac3_auto_assignment_window
        CHECK (valid_to IS NULL OR valid_to > valid_from),
    CONSTRAINT ck_rbac3_auto_assignment_version CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_auto_assignment_match
    ON rbac3_auto_assignment_rule
        (tenant_id, status, match_type, match_ref_id);

CREATE TABLE rbac3_role_prerequisite (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    target_role_id BIGINT NOT NULL,
    group_code VARCHAR(128) NOT NULL,
    match_mode VARCHAR(32) NOT NULL,
    prerequisite_role_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_role_prerequisite_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_role_prerequisite_fact UNIQUE (
        tenant_id, target_role_id, group_code, prerequisite_role_id
    ),
    CONSTRAINT fk_rbac3_role_prerequisite_target
        FOREIGN KEY (tenant_id, target_role_id)
        REFERENCES rbac3_role(tenant_id, id),
    CONSTRAINT fk_rbac3_role_prerequisite_required
        FOREIGN KEY (tenant_id, prerequisite_role_id)
        REFERENCES rbac3_role(tenant_id, id),
    CONSTRAINT ck_rbac3_role_prerequisite_distinct
        CHECK (target_role_id <> prerequisite_role_id),
    CONSTRAINT ck_rbac3_role_prerequisite_mode
        CHECK (match_mode IN ('ALL_OF', 'ANY_OF')),
    CONSTRAINT ck_rbac3_role_prerequisite_status
        CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_rbac3_role_prerequisite_version CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_role_prerequisite_target
    ON rbac3_role_prerequisite (tenant_id, target_role_id, status);

CREATE TABLE rbac3_role_cardinality (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    scope_type VARCHAR(32) NOT NULL,
    max_active INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_to TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_role_cardinality_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_role_cardinality_fact
        UNIQUE (tenant_id, role_id, scope_type, valid_from),
    CONSTRAINT fk_rbac3_role_cardinality_role
        FOREIGN KEY (tenant_id, role_id)
        REFERENCES rbac3_role(tenant_id, id),
    CONSTRAINT ck_rbac3_role_cardinality_scope
        CHECK (scope_type IN ('TENANT', 'ORG', 'DEPT')),
    CONSTRAINT ck_rbac3_role_cardinality_max CHECK (max_active > 0),
    CONSTRAINT ck_rbac3_role_cardinality_status
        CHECK (status IN ('ACTIVE', 'DISABLED', 'EXPIRED')),
    CONSTRAINT ck_rbac3_role_cardinality_window
        CHECK (valid_to IS NULL OR valid_to > valid_from),
    CONSTRAINT ck_rbac3_role_cardinality_version CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_role_cardinality_role
    ON rbac3_role_cardinality (tenant_id, role_id, status);

CREATE TABLE rbac3_sod_set (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    application_id BIGINT,
    set_code VARCHAR(128) NOT NULL,
    constraint_type VARCHAR(32) NOT NULL,
    max_active_roles INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_to TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_sod_set_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_sod_set_code UNIQUE (tenant_id, set_code),
    CONSTRAINT fk_rbac3_sod_set_application
        FOREIGN KEY (tenant_id, application_id)
        REFERENCES rbac3_application(tenant_id, id),
    CONSTRAINT ck_rbac3_sod_set_type
        CHECK (constraint_type IN ('SSD', 'DSD')),
    CONSTRAINT ck_rbac3_sod_set_application
        CHECK (constraint_type <> 'DSD' OR application_id IS NOT NULL),
    CONSTRAINT ck_rbac3_sod_set_max CHECK (max_active_roles >= 1),
    CONSTRAINT ck_rbac3_sod_set_status
        CHECK (status IN ('ACTIVE', 'DISABLED', 'EXPIRED')),
    CONSTRAINT ck_rbac3_sod_set_window
        CHECK (valid_to IS NULL OR valid_to > valid_from),
    CONSTRAINT ck_rbac3_sod_set_version CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_sod_set_lookup
    ON rbac3_sod_set
        (tenant_id, constraint_type, application_id, status);

CREATE TABLE rbac3_sod_member (
    tenant_id BIGINT NOT NULL,
    sod_set_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT pk_rbac3_sod_member PRIMARY KEY (tenant_id, sod_set_id, role_id),
    CONSTRAINT fk_rbac3_sod_member_set
        FOREIGN KEY (tenant_id, sod_set_id)
        REFERENCES rbac3_sod_set(tenant_id, id),
    CONSTRAINT fk_rbac3_sod_member_role
        FOREIGN KEY (tenant_id, role_id)
        REFERENCES rbac3_role(tenant_id, id)
);

CREATE INDEX idx_rbac3_sod_member_role
    ON rbac3_sod_member (tenant_id, role_id);

CREATE TABLE rbac3_operation_sod_rule (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    application_code VARCHAR(128) NOT NULL,
    business_resource VARCHAR(128) NOT NULL,
    prior_action_code VARCHAR(128) NOT NULL,
    forbidden_later_action_code VARCHAR(128) NOT NULL,
    lookback_from TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_to TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_operation_sod_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_operation_sod_fact UNIQUE (
        tenant_id, application_code, business_resource,
        prior_action_code, forbidden_later_action_code, valid_from
    ),
    CONSTRAINT fk_rbac3_operation_sod_application
        FOREIGN KEY (tenant_id, application_code)
        REFERENCES rbac3_application(tenant_id, application_code),
    CONSTRAINT ck_rbac3_operation_sod_status
        CHECK (status IN ('ACTIVE', 'DISABLED', 'EXPIRED')),
    CONSTRAINT ck_rbac3_operation_sod_window
        CHECK (valid_to IS NULL OR valid_to > valid_from),
    CONSTRAINT ck_rbac3_operation_sod_version CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_operation_sod_lookup
    ON rbac3_operation_sod_rule
        (tenant_id, application_code, business_resource, status);

CREATE TABLE rbac3_business_participation (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    application_code VARCHAR(128) NOT NULL,
    business_resource VARCHAR(128) NOT NULL,
    business_id VARCHAR(256) NOT NULL,
    actor_user_id BIGINT NOT NULL,
    action_code VARCHAR(128) NOT NULL,
    business_event_id VARCHAR(128) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    trace_id VARCHAR(128) NOT NULL,
    payload_digest VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_business_participation_event
        UNIQUE (tenant_id, application_code, business_event_id),
    CONSTRAINT fk_rbac3_business_participation_application
        FOREIGN KEY (tenant_id, application_code)
        REFERENCES rbac3_application(tenant_id, application_code),
    CONSTRAINT fk_rbac3_business_participation_actor
        FOREIGN KEY (tenant_id, actor_user_id)
        REFERENCES rbac3_user(tenant_id, id)
);

CREATE INDEX idx_rbac3_participation_conflict
    ON rbac3_business_participation (
        tenant_id, application_code, business_resource, business_id,
        actor_user_id, action_code
    );
CREATE INDEX idx_rbac3_participation_occurred
    ON rbac3_business_participation (occurred_at);

CREATE TABLE rbac3_management_policy (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    policy_code VARCHAR(128) NOT NULL,
    name VARCHAR(200) NOT NULL,
    status VARCHAR(32) NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_to TIMESTAMPTZ,
    max_assignment_days INTEGER,
    max_risk_level VARCHAR(32) NOT NULL,
    required_auth_strength VARCHAR(32) NOT NULL,
    require_reason BOOLEAN NOT NULL DEFAULT FALSE,
    require_ticket BOOLEAN NOT NULL DEFAULT FALSE,
    include_inherited_subject_roles BOOLEAN NOT NULL DEFAULT FALSE,
    require_all_affiliations_in_scope BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_management_policy_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_management_policy_code UNIQUE (tenant_id, policy_code),
    CONSTRAINT fk_rbac3_management_policy_tenant
        FOREIGN KEY (tenant_id) REFERENCES rbac3_tenant(id),
    CONSTRAINT ck_rbac3_management_policy_status
        CHECK (status IN ('ACTIVE', 'DISABLED', 'EXPIRED', 'ARCHIVED')),
    CONSTRAINT ck_rbac3_management_policy_window
        CHECK (valid_to IS NULL OR valid_to > valid_from),
    CONSTRAINT ck_rbac3_management_policy_days
        CHECK (max_assignment_days IS NULL OR max_assignment_days > 0),
    CONSTRAINT ck_rbac3_management_policy_risk
        CHECK (max_risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_rbac3_management_policy_auth
        CHECK (required_auth_strength IN ('PASSWORD', 'MFA', 'STRONG')),
    CONSTRAINT ck_rbac3_management_policy_version CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_management_policy_active
    ON rbac3_management_policy (tenant_id, status, valid_from, valid_to);

CREATE TABLE rbac3_management_subject (
    tenant_id BIGINT NOT NULL,
    policy_id BIGINT NOT NULL,
    subject_type VARCHAR(32) NOT NULL,
    subject_id BIGINT NOT NULL,
    CONSTRAINT pk_rbac3_management_subject PRIMARY KEY (
        tenant_id, policy_id, subject_type, subject_id
    ),
    CONSTRAINT fk_rbac3_management_subject_policy
        FOREIGN KEY (tenant_id, policy_id)
        REFERENCES rbac3_management_policy(tenant_id, id),
    CONSTRAINT ck_rbac3_management_subject_type
        CHECK (subject_type IN ('USER', 'ROLE', 'POSITION'))
);

CREATE INDEX idx_rbac3_management_subject_lookup
    ON rbac3_management_subject (tenant_id, subject_type, subject_id);

CREATE TABLE rbac3_management_scope (
    tenant_id BIGINT NOT NULL,
    policy_id BIGINT NOT NULL,
    scope_type VARCHAR(32) NOT NULL,
    scope_ref_id BIGINT,
    CONSTRAINT uq_rbac3_management_scope
        UNIQUE (tenant_id, policy_id, scope_type, scope_ref_id),
    CONSTRAINT fk_rbac3_management_scope_policy
        FOREIGN KEY (tenant_id, policy_id)
        REFERENCES rbac3_management_policy(tenant_id, id),
    CONSTRAINT ck_rbac3_management_scope_type CHECK (
        scope_type IN (
            'SELF_DEPT', 'DEPT', 'DEPT_TREE', 'ORG', 'ORG_TREE',
            'CUSTOM_DEPT', 'CUSTOM_USER'
        )
    ),
    CONSTRAINT ck_rbac3_management_scope_ref
        CHECK (scope_type = 'SELF_DEPT' OR scope_ref_id IS NOT NULL)
);

CREATE UNIQUE INDEX uk_rbac3_management_scope_self_dept
    ON rbac3_management_scope (tenant_id, policy_id, scope_type)
    WHERE scope_ref_id IS NULL;
CREATE INDEX idx_rbac3_management_scope_lookup
    ON rbac3_management_scope (tenant_id, scope_type, scope_ref_id);

CREATE TABLE rbac3_management_role (
    tenant_id BIGINT NOT NULL,
    policy_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT pk_rbac3_management_role PRIMARY KEY (
        tenant_id, policy_id, role_id
    ),
    CONSTRAINT fk_rbac3_management_role_policy
        FOREIGN KEY (tenant_id, policy_id)
        REFERENCES rbac3_management_policy(tenant_id, id),
    CONSTRAINT fk_rbac3_management_role_role
        FOREIGN KEY (tenant_id, role_id)
        REFERENCES rbac3_role(tenant_id, id)
);

CREATE INDEX idx_rbac3_management_role_lookup
    ON rbac3_management_role (tenant_id, role_id);

CREATE TABLE rbac3_management_operation (
    tenant_id BIGINT NOT NULL,
    policy_id BIGINT NOT NULL,
    operation_code VARCHAR(32) NOT NULL,
    CONSTRAINT pk_rbac3_management_operation PRIMARY KEY (
        tenant_id, policy_id, operation_code
    ),
    CONSTRAINT fk_rbac3_management_operation_policy
        FOREIGN KEY (tenant_id, policy_id)
        REFERENCES rbac3_management_policy(tenant_id, id),
    CONSTRAINT ck_rbac3_management_operation_code CHECK (
        operation_code IN (
            'VIEW_ASSIGNMENT', 'ASSIGN_ROLE', 'REVOKE_ROLE', 'SUSPEND_ROLE',
            'RESUME_ROLE', 'TEMPORARY_ASSIGN', 'VIEW_AUDIT', 'VIEW_IMPACT',
            'SELF_REVOKE_LOW_RISK'
        )
    )
);

CREATE INDEX idx_rbac3_management_operation_lookup
    ON rbac3_management_operation (tenant_id, operation_code);

CREATE TABLE rbac3_session (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    session_version BIGINT NOT NULL DEFAULT 0,
    auth_version_at_issue BIGINT NOT NULL,
    policy_version_at_issue BIGINT NOT NULL,
    active_root_checksum VARCHAR(128),
    activation_required BOOLEAN NOT NULL DEFAULT TRUE,
    token_family_id VARCHAR(128) NOT NULL,
    device_id_hash VARCHAR(128),
    auth_strength VARCHAR(32) NOT NULL,
    authenticated_at TIMESTAMPTZ NOT NULL,
    last_seen_at TIMESTAMPTZ NOT NULL,
    idle_expires_at TIMESTAMPTZ NOT NULL,
    absolute_expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    revoke_reason VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_session_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_session_tenant_session UNIQUE (tenant_id, session_id),
    CONSTRAINT uq_rbac3_session_id UNIQUE (session_id),
    CONSTRAINT uq_rbac3_session_token_family UNIQUE (token_family_id),
    CONSTRAINT fk_rbac3_session_user
        FOREIGN KEY (tenant_id, user_id)
        REFERENCES rbac3_user(tenant_id, id),
    CONSTRAINT ck_rbac3_session_status
        CHECK (status IN ('ACTIVE', 'LOGGED_OUT', 'REVOKED', 'EXPIRED', 'COMPROMISED')),
    CONSTRAINT ck_rbac3_session_auth_strength
        CHECK (auth_strength IN ('PASSWORD', 'MFA', 'STRONG')),
    CONSTRAINT ck_rbac3_session_idle_expiry
        CHECK (idle_expires_at > authenticated_at),
    CONSTRAINT ck_rbac3_session_absolute_expiry
        CHECK (absolute_expires_at > authenticated_at),
    CONSTRAINT ck_rbac3_session_expiry_order
        CHECK (idle_expires_at <= absolute_expires_at),
    CONSTRAINT ck_rbac3_session_versions CHECK (
        session_version >= 0 AND auth_version_at_issue >= 0
        AND policy_version_at_issue >= 0 AND version >= 0
    ),
    CONSTRAINT ck_rbac3_session_revocation CHECK (
        (status = 'REVOKED' AND revoked_at IS NOT NULL)
        OR status <> 'REVOKED'
    )
);

CREATE INDEX idx_rbac3_session_user_status
    ON rbac3_session (tenant_id, user_id, status);
CREATE INDEX idx_rbac3_session_absolute_expiry
    ON rbac3_session (status, absolute_expires_at);

CREATE TABLE rbac3_session_active_role (
    tenant_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    root_role_id BIGINT NOT NULL,
    session_version BIGINT NOT NULL,
    eligible_assignment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    activated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_rbac3_session_active_root
        UNIQUE (tenant_id, session_id, root_role_id),
    CONSTRAINT fk_rbac3_session_active_role_session
        FOREIGN KEY (tenant_id, session_id)
        REFERENCES rbac3_session(tenant_id, session_id),
    CONSTRAINT fk_rbac3_session_active_role_root
        FOREIGN KEY (tenant_id, application_id, root_role_id)
        REFERENCES rbac3_role(tenant_id, application_id, id),
    CONSTRAINT ck_rbac3_session_active_role_version
        CHECK (session_version >= 0),
    CONSTRAINT ck_rbac3_session_active_role_evidence
        CHECK (jsonb_typeof(eligible_assignment_ids) = 'array')
);

CREATE INDEX idx_rbac3_session_active_role_application
    ON rbac3_session_active_role (tenant_id, session_id, application_id);
CREATE INDEX idx_rbac3_session_active_role_root
    ON rbac3_session_active_role (tenant_id, root_role_id);

CREATE TABLE rbac3_refresh_token (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    family_id VARCHAR(128) NOT NULL,
    generation BIGINT NOT NULL,
    token_hash VARCHAR(256) NOT NULL,
    status VARCHAR(32) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    rotated_at TIMESTAMPTZ,
    replaced_by_id BIGINT,
    reuse_detected_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_refresh_token_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT uq_rbac3_refresh_token_generation UNIQUE (family_id, generation),
    CONSTRAINT fk_rbac3_refresh_token_session
        FOREIGN KEY (tenant_id, session_id)
        REFERENCES rbac3_session(tenant_id, session_id),
    CONSTRAINT fk_rbac3_refresh_token_replacement
        FOREIGN KEY (tenant_id, replaced_by_id)
        REFERENCES rbac3_refresh_token(tenant_id, id),
    CONSTRAINT ck_rbac3_refresh_token_status
        CHECK (status IN ('ACTIVE', 'ROTATED', 'REUSED_DETECTED', 'REVOKED', 'EXPIRED')),
    CONSTRAINT ck_rbac3_refresh_token_expiry CHECK (expires_at > issued_at),
    CONSTRAINT ck_rbac3_refresh_token_generation CHECK (generation >= 0),
    CONSTRAINT ck_rbac3_refresh_token_version CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_refresh_token_session_status
    ON rbac3_refresh_token (tenant_id, session_id, status);
CREATE INDEX idx_rbac3_refresh_token_expiry
    ON rbac3_refresh_token (expires_at);

CREATE TABLE rbac3_idempotency_record (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    actor_type VARCHAR(32) NOT NULL,
    actor_id VARCHAR(128) NOT NULL,
    operation_code VARCHAR(128) NOT NULL,
    key_hash VARCHAR(256) NOT NULL,
    request_hash VARCHAR(256) NOT NULL,
    resource_type VARCHAR(128),
    resource_id VARCHAR(128),
    response_status INTEGER,
    response_digest VARCHAR(256),
    status VARCHAR(32) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_idempotency_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_idempotency_key UNIQUE (
        tenant_id, actor_type, actor_id, operation_code, key_hash
    ),
    CONSTRAINT fk_rbac3_idempotency_tenant
        FOREIGN KEY (tenant_id) REFERENCES rbac3_tenant(id),
    CONSTRAINT ck_rbac3_idempotency_actor
        CHECK (actor_type IN ('USER', 'SERVICE', 'SYSTEM')),
    CONSTRAINT ck_rbac3_idempotency_status
        CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_rbac3_idempotency_response_status CHECK (
        response_status IS NULL OR response_status BETWEEN 100 AND 599
    ),
    CONSTRAINT ck_rbac3_idempotency_version CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_idempotency_expiry
    ON rbac3_idempotency_record (expires_at);
CREATE INDEX idx_rbac3_idempotency_resource
    ON rbac3_idempotency_record (tenant_id, resource_type, resource_id);

CREATE TABLE rbac3_authorization_mutation (
    mutation_id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT,
    session_id BIGINT,
    scope_type VARCHAR(32) NOT NULL,
    command_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    old_session_version BIGINT,
    new_session_version BIGINT,
    old_auth_version BIGINT,
    new_auth_version BIGINT,
    old_policy_version BIGINT,
    new_policy_version BIGINT,
    fence_created_at TIMESTAMPTZ,
    committed_at TIMESTAMPTZ,
    projected_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    last_error_code VARCHAR(128),
    attempt INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uq_rbac3_authorization_mutation_id UNIQUE (mutation_id),
    CONSTRAINT uq_rbac3_authorization_mutation_command UNIQUE (command_id),
    CONSTRAINT fk_rbac3_authorization_mutation_tenant
        FOREIGN KEY (tenant_id) REFERENCES rbac3_tenant(id),
    CONSTRAINT fk_rbac3_authorization_mutation_user
        FOREIGN KEY (tenant_id, user_id)
        REFERENCES rbac3_user(tenant_id, id),
    CONSTRAINT fk_rbac3_authorization_mutation_session
        FOREIGN KEY (tenant_id, session_id)
        REFERENCES rbac3_session(tenant_id, session_id),
    CONSTRAINT ck_rbac3_authorization_mutation_scope
        CHECK (scope_type IN ('SESSION', 'USER', 'TENANT')),
    CONSTRAINT ck_rbac3_authorization_mutation_scope_target
        CHECK (scope_type <> 'SESSION' OR session_id IS NOT NULL),
    CONSTRAINT ck_rbac3_authorization_mutation_user_target
        CHECK (scope_type <> 'USER' OR user_id IS NOT NULL),
    CONSTRAINT ck_rbac3_authorization_mutation_status
        CHECK (status IN ('PREPARING', 'COMMITTED', 'PROJECTED', 'COMPLETED',
            'ABORTED', 'RECOVERY_REQUIRED')),
    CONSTRAINT ck_rbac3_authorization_mutation_versions CHECK (
        (old_session_version IS NULL OR old_session_version >= 0)
        AND (new_session_version IS NULL OR new_session_version >= 0)
        AND (old_auth_version IS NULL OR old_auth_version >= 0)
        AND (new_auth_version IS NULL OR new_auth_version >= 0)
        AND (old_policy_version IS NULL OR old_policy_version >= 0)
        AND (new_policy_version IS NULL OR new_policy_version >= 0)
        AND version >= 0
    ),
    CONSTRAINT ck_rbac3_authorization_mutation_attempt CHECK (attempt >= 0)
);

CREATE INDEX idx_rbac3_authorization_mutation_recovery
    ON rbac3_authorization_mutation (status, updated_at);
CREATE INDEX idx_rbac3_authorization_mutation_scope
    ON rbac3_authorization_mutation
        (tenant_id, user_id, session_id, status);

CREATE TABLE rbac3_audit_log (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    actor_type VARCHAR(32) NOT NULL,
    actor_id VARCHAR(128) NOT NULL,
    target_type VARCHAR(128),
    target_id VARCHAR(128),
    management_policy_id BIGINT,
    reason_code VARCHAR(128),
    request_id VARCHAR(128) NOT NULL,
    trace_id VARCHAR(128) NOT NULL,
    client_ip INET,
    user_agent TEXT,
    before_snapshot JSONB,
    after_snapshot JSONB,
    payload_checksum VARCHAR(256) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_rbac3_audit_tenant
        FOREIGN KEY (tenant_id) REFERENCES rbac3_tenant(id),
    CONSTRAINT fk_rbac3_audit_management_policy
        FOREIGN KEY (tenant_id, management_policy_id)
        REFERENCES rbac3_management_policy(tenant_id, id),
    CONSTRAINT ck_rbac3_audit_outcome
        CHECK (outcome IN ('SUCCESS', 'DENIED', 'FAILED', 'PENDING_PROPAGATION')),
    CONSTRAINT ck_rbac3_audit_severity
        CHECK (severity IN ('INFO', 'WARN', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_rbac3_audit_actor
        CHECK (actor_type IN ('USER', 'SERVICE', 'SYSTEM')),
    CONSTRAINT ck_rbac3_audit_checksum CHECK (length(payload_checksum) > 0)
);

CREATE INDEX idx_rbac3_audit_tenant_created
    ON rbac3_audit_log (tenant_id, created_at DESC);
CREATE INDEX idx_rbac3_audit_trace
    ON rbac3_audit_log (tenant_id, trace_id);
CREATE INDEX idx_rbac3_audit_target_created
    ON rbac3_audit_log
        (tenant_id, target_type, target_id, created_at DESC);

ALTER TABLE rbac3_user
    ADD CONSTRAINT fk_rbac3_user_primary_org
    FOREIGN KEY (tenant_id, primary_org_unit_id)
    REFERENCES rbac3_org_unit(tenant_id, id),
    ADD CONSTRAINT fk_rbac3_user_primary_position
    FOREIGN KEY (tenant_id, primary_position_id)
    REFERENCES rbac3_position(tenant_id, id);

ALTER TABLE rbac3_service_principal
    ADD CONSTRAINT fk_rbac3_service_principal_application
    FOREIGN KEY (tenant_id, application_code)
    REFERENCES rbac3_application(tenant_id, application_code);

ALTER TABLE rbac3_service_permission
    ADD CONSTRAINT fk_rbac3_service_permission_application
    FOREIGN KEY (tenant_id, application_id, application_code)
    REFERENCES rbac3_application(tenant_id, id, application_code),
    ADD CONSTRAINT fk_rbac3_service_permission_permission
    FOREIGN KEY (tenant_id, application_id, permission_id)
    REFERENCES rbac3_permission(tenant_id, application_id, id);

ALTER TABLE rbac3_application
    ADD CONSTRAINT fk_rbac3_application_current_manifest
    FOREIGN KEY (tenant_id, id, current_manifest_id)
    REFERENCES rbac3_resource_manifest(tenant_id, application_id, id);

ALTER TABLE rbac3_resource
    ADD CONSTRAINT fk_rbac3_resource_required_permission
    FOREIGN KEY (tenant_id, application_id, required_permission_id)
    REFERENCES rbac3_permission(tenant_id, application_id, id);

CREATE FUNCTION rbac3_reject_immutable_column_change()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    column_name TEXT;
BEGIN
    FOREACH column_name IN ARRAY TG_ARGV LOOP
        IF (to_jsonb(NEW) -> column_name)
                IS DISTINCT FROM (to_jsonb(OLD) -> column_name) THEN
            RAISE EXCEPTION '% column %.% is immutable',
                TG_OP, TG_TABLE_NAME, column_name
                USING ERRCODE = '55000';
        END IF;
    END LOOP;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_rbac3_directory_snapshot_immutable
    BEFORE UPDATE ON rbac3_directory_snapshot
    FOR EACH ROW EXECUTE FUNCTION rbac3_reject_immutable_column_change(
        'tenant_id', 'provider_code', 'snapshot_version', 'checksum',
        'generated_at', 'payload'
    );

CREATE TRIGGER trg_rbac3_resource_manifest_immutable
    BEFORE UPDATE ON rbac3_resource_manifest
    FOR EACH ROW EXECUTE FUNCTION rbac3_reject_immutable_column_change(
        'tenant_id', 'application_id', 'schema_version', 'artifact_version',
        'build_id', 'manifest_version', 'checksum', 'payload'
    );

CREATE TRIGGER trg_rbac3_permission_code_immutable
    BEFORE UPDATE ON rbac3_permission
    FOR EACH ROW EXECUTE FUNCTION rbac3_reject_immutable_column_change(
        'tenant_id', 'application_id', 'permission_code'
    );

CREATE TRIGGER trg_rbac3_resource_identity_immutable
    BEFORE UPDATE ON rbac3_resource
    FOR EACH ROW EXECUTE FUNCTION rbac3_reject_immutable_column_change(
        'tenant_id', 'application_id', 'resource_type', 'resource_code',
        'source_manifest_id', 'source_build_id', 'mechanical_facts'
    );

CREATE TRIGGER trg_rbac3_role_identity_immutable
    BEFORE UPDATE ON rbac3_role
    FOR EACH ROW EXECUTE FUNCTION rbac3_reject_immutable_column_change(
        'tenant_id', 'application_id', 'role_code', 'role_type', 'privileged'
    );

CREATE TRIGGER trg_rbac3_permission_resource_mapping_immutable
    BEFORE UPDATE ON rbac3_permission_resource
    FOR EACH ROW EXECUTE FUNCTION rbac3_reject_immutable_column_change(
        'tenant_id', 'application_id', 'permission_id', 'resource_id',
        'resource_type', 'definition_set_id', 'gateway_operation_id',
        'security_policy_id', 'mapping_version'
    );

CREATE FUNCTION rbac3_reject_append_only_change()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION '% is append-only', TG_TABLE_NAME
        USING ERRCODE = '55000';
    RETURN OLD;
END;
$$;

CREATE TRIGGER trg_rbac3_audit_log_append_only
    BEFORE UPDATE OR DELETE ON rbac3_audit_log
    FOR EACH ROW EXECUTE FUNCTION rbac3_reject_append_only_change();

CREATE TRIGGER trg_rbac3_business_participation_append_only
    BEFORE UPDATE OR DELETE ON rbac3_business_participation
    FOR EACH ROW EXECUTE FUNCTION rbac3_reject_append_only_change();
