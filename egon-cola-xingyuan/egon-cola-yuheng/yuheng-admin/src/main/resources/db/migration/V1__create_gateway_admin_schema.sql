CREATE TABLE gateway_group (
    id VARCHAR(64) PRIMARY KEY,
    gateway_group_code VARCHAR(128) NOT NULL,
    display_name VARCHAR(256) NOT NULL,
    env VARCHAR(64) NOT NULL,
    namespace VARCHAR(128) NOT NULL,
    description VARCHAR(1024),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    revision BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL
);

CREATE UNIQUE INDEX uk_gateway_group_scope_active
    ON gateway_group (gateway_group_code, env, namespace)
    WHERE deleted = FALSE;

CREATE TABLE gateway_application (
    id VARCHAR(64) PRIMARY KEY,
    application_code VARCHAR(128) NOT NULL,
    display_name VARCHAR(256) NOT NULL,
    env VARCHAR(64) NOT NULL,
    namespace VARCHAR(128) NOT NULL,
    description VARCHAR(1024),
    revision BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL
);

CREATE UNIQUE INDEX uk_gateway_application_scope_active
    ON gateway_application (application_code, env, namespace)
    WHERE deleted = FALSE;

CREATE TABLE gateway_application_credential (
    id VARCHAR(64) PRIMARY KEY,
    application_id VARCHAR(64) NOT NULL REFERENCES gateway_application(id),
    access_key VARCHAR(128) NOT NULL UNIQUE,
    secret_ciphertext TEXT,
    secret_reference VARCHAR(512),
    key_version VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_gateway_credential_secret
        CHECK ((secret_ciphertext IS NOT NULL) <> (secret_reference IS NOT NULL))
);

CREATE TABLE gateway_hmac_nonce (
    access_key VARCHAR(128) NOT NULL,
    nonce VARCHAR(256) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (access_key, nonce)
);

CREATE INDEX idx_gateway_hmac_nonce_expiry
    ON gateway_hmac_nonce (expires_at);

CREATE TABLE gateway_business_domain (
    id VARCHAR(64) PRIMARY KEY,
    application_id VARCHAR(64) NOT NULL REFERENCES gateway_application(id),
    code VARCHAR(128) NOT NULL,
    display_name VARCHAR(256) NOT NULL,
    description VARCHAR(1024),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uk_gateway_business_domain_active
    ON gateway_business_domain (application_id, code)
    WHERE deleted = FALSE;

CREATE TABLE gateway_entity_domain (
    id VARCHAR(64) PRIMARY KEY,
    business_domain_id VARCHAR(64) NOT NULL
        REFERENCES gateway_business_domain(id),
    code VARCHAR(128) NOT NULL,
    display_name VARCHAR(256) NOT NULL,
    description VARCHAR(1024),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uk_gateway_entity_domain_active
    ON gateway_entity_domain (business_domain_id, code)
    WHERE deleted = FALSE;

CREATE TABLE gateway_interface_group (
    id VARCHAR(64) PRIMARY KEY,
    entity_domain_id VARCHAR(64) NOT NULL REFERENCES gateway_entity_domain(id),
    code VARCHAR(256) NOT NULL,
    display_name VARCHAR(256) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    class_name VARCHAR(512),
    description VARCHAR(1024),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uk_gateway_interface_group_active
    ON gateway_interface_group (entity_domain_id, code)
    WHERE deleted = FALSE;

CREATE TABLE gateway_definition_set (
    id VARCHAR(64) PRIMARY KEY,
    application_id VARCHAR(64) NOT NULL REFERENCES gateway_application(id),
    report_id VARCHAR(128) NOT NULL,
    build_id VARCHAR(256) NOT NULL,
    protocol VARCHAR(32) NOT NULL,
    fingerprint VARCHAR(64) NOT NULL,
    complete_set BOOLEAN NOT NULL,
    status VARCHAR(32) NOT NULL,
    operation_count INTEGER NOT NULL,
    accepted_count INTEGER NOT NULL DEFAULT 0,
    conflict_count INTEGER NOT NULL DEFAULT 0,
    received_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX uk_gateway_definition_set_build
    ON gateway_definition_set
        (application_id, build_id, protocol, fingerprint);
CREATE UNIQUE INDEX uk_gateway_definition_set_report
    ON gateway_definition_set (application_id, report_id);

CREATE TABLE gateway_operation (
    id VARCHAR(64) PRIMARY KEY,
    application_id VARCHAR(64) NOT NULL REFERENCES gateway_application(id),
    interface_group_id VARCHAR(64) NOT NULL
        REFERENCES gateway_interface_group(id),
    operation_key VARCHAR(512) NOT NULL,
    protocol VARCHAR(32) NOT NULL,
    method_identity VARCHAR(1024) NOT NULL,
    external_accessible BOOLEAN NOT NULL DEFAULT FALSE,
    provider_service_identity JSONB NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    lifecycle_status VARCHAR(32) NOT NULL,
    current_definition_id VARCHAR(64),
    deprecated_at TIMESTAMPTZ,
    revision BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uk_gateway_operation_key
    ON gateway_operation (application_id, operation_key);

CREATE TABLE gateway_operation_definition (
    id VARCHAR(64) PRIMARY KEY,
    operation_id VARCHAR(64) NOT NULL REFERENCES gateway_operation(id),
    definition_set_id VARCHAR(64) REFERENCES gateway_definition_set(id),
    definition_version BIGINT NOT NULL,
    definition_sha256 VARCHAR(64) NOT NULL,
    summary VARCHAR(1024),
    tags JSONB NOT NULL DEFAULT '[]'::jsonb,
    request_schema JSONB NOT NULL,
    response_schema JSONB NOT NULL,
    error_schema JSONB NOT NULL DEFAULT '[]'::jsonb,
    descriptor_snapshot JSONB,
    attributes JSONB NOT NULL DEFAULT '{}'::jsonb,
    external_accessible BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    UNIQUE (operation_id, definition_version),
    UNIQUE (operation_id, definition_sha256)
);

ALTER TABLE gateway_operation
    ADD CONSTRAINT fk_gateway_operation_current_definition
    FOREIGN KEY (current_definition_id)
    REFERENCES gateway_operation_definition(id);

CREATE TABLE gateway_draft (
    gateway_group_id VARCHAR(64) PRIMARY KEY REFERENCES gateway_group(id),
    revision BIGINT NOT NULL DEFAULT 0,
    based_on_release_id VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    change_summary VARCHAR(1024),
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL
);

CREATE TABLE gateway_route_draft (
    gateway_group_id VARCHAR(64) NOT NULL REFERENCES gateway_draft(gateway_group_id),
    route_id VARCHAR(128) NOT NULL,
    operation_id VARCHAR(64) NOT NULL REFERENCES gateway_operation(id),
    route_content JSONB NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    PRIMARY KEY (gateway_group_id, route_id)
);

CREATE TABLE gateway_policy_draft (
    gateway_group_id VARCHAR(64) NOT NULL REFERENCES gateway_draft(gateway_group_id),
    policy_id VARCHAR(128) NOT NULL,
    policy_type VARCHAR(64) NOT NULL,
    policy_scope VARCHAR(64) NOT NULL,
    policy_content JSONB NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    PRIMARY KEY (gateway_group_id, policy_id)
);

CREATE TABLE gateway_release (
    id VARCHAR(64) PRIMARY KEY,
    gateway_group_id VARCHAR(64) NOT NULL REFERENCES gateway_group(id),
    draft_revision BIGINT NOT NULL,
    based_on_release_id VARCHAR(64),
    rollback_of_release_id VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    partial_applied BOOLEAN NOT NULL DEFAULT FALSE,
    change_id VARCHAR(128),
    validation_report JSONB NOT NULL,
    structured_diff JSONB NOT NULL,
    change_reason VARCHAR(1024) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_gateway_release_group_created
    ON gateway_release (gateway_group_id, created_at DESC);
CREATE INDEX idx_gateway_release_status
    ON gateway_release (status);

CREATE TABLE gateway_release_content (
    release_id VARCHAR(64) PRIMARY KEY REFERENCES gateway_release(id),
    rule_content_sha256 VARCHAR(64) NOT NULL,
    artifact_sha256 VARCHAR(64) NOT NULL,
    canonical_snapshot JSONB NOT NULL,
    activation_content JSONB NOT NULL,
    chunk_manifest JSONB NOT NULL,
    snapshot_size BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_gateway_release_content_rule_sha
    ON gateway_release_content (rule_content_sha256);
CREATE INDEX idx_gateway_release_content_artifact_sha
    ON gateway_release_content (artifact_sha256);

CREATE TABLE gateway_release_attempt (
    release_id VARCHAR(64) NOT NULL REFERENCES gateway_release(id),
    attempt_no INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    change_id VARCHAR(128),
    lease_owner VARCHAR(128),
    lease_until TIMESTAMPTZ,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    error_code VARCHAR(128),
    error_message VARCHAR(1024),
    PRIMARY KEY (release_id, attempt_no)
);

CREATE TABLE gateway_release_target (
    release_id VARCHAR(64) NOT NULL,
    attempt_no INTEGER NOT NULL,
    instance_id VARCHAR(256) NOT NULL,
    lease_id VARCHAR(256) NOT NULL,
    status VARCHAR(32) NOT NULL,
    applied_version BIGINT,
    applied_artifact_sha256 VARCHAR(64),
    error_code VARCHAR(128),
    observed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (release_id, attempt_no, instance_id, lease_id),
    FOREIGN KEY (release_id, attempt_no)
        REFERENCES gateway_release_attempt(release_id, attempt_no)
);

CREATE TABLE gateway_idempotency_record (
    scope_type VARCHAR(64) NOT NULL,
    scope_id VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(256) NOT NULL,
    payload_sha256 VARCHAR(64) NOT NULL,
    resource_id VARCHAR(128) NOT NULL,
    response_content JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ,
    PRIMARY KEY (scope_type, scope_id, idempotency_key)
);

CREATE TABLE gateway_audit_log (
    id VARCHAR(64) PRIMARY KEY,
    actor_id VARCHAR(128) NOT NULL,
    actor_type VARCHAR(32) NOT NULL,
    source VARCHAR(64) NOT NULL,
    request_id VARCHAR(128),
    trace_id VARCHAR(128),
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(128) NOT NULL,
    action VARCHAR(128) NOT NULL,
    before_summary JSONB,
    after_summary JSONB,
    draft_revision BIGINT,
    release_id VARCHAR(64),
    successful BOOLEAN NOT NULL,
    error_code VARCHAR(128),
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_gateway_audit_resource
    ON gateway_audit_log (resource_type, resource_id, occurred_at DESC);
