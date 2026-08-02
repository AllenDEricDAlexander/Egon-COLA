CREATE TABLE gateway_mcp_server (
    id VARCHAR(64) PRIMARY KEY,
    gateway_group_id VARCHAR(64) NOT NULL REFERENCES gateway_group(id),
    server_code VARCHAR(128) NOT NULL,
    display_name VARCHAR(256) NOT NULL,
    description VARCHAR(2048),
    instructions TEXT,
    dialects JSONB NOT NULL DEFAULT '[]'::jsonb,
    oauth_audience VARCHAR(256) NOT NULL,
    list_cache_ttl_seconds BIGINT NOT NULL DEFAULT 30,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    revision BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT ck_gateway_mcp_server_dialects
        CHECK (jsonb_typeof(dialects) = 'array'),
    CONSTRAINT ck_gateway_mcp_server_cache_ttl
        CHECK (list_cache_ttl_seconds >= 0),
    CONSTRAINT ck_gateway_mcp_server_revision
        CHECK (revision >= 0)
);

CREATE UNIQUE INDEX uk_gateway_mcp_server_code_active
    ON gateway_mcp_server (gateway_group_id, server_code)
    WHERE deleted = FALSE;
CREATE INDEX idx_gateway_mcp_server_group
    ON gateway_mcp_server (gateway_group_id, enabled, server_code)
    WHERE deleted = FALSE;

CREATE TABLE gateway_mcp_tool_draft (
    id VARCHAR(64) PRIMARY KEY,
    gateway_group_id VARCHAR(64) NOT NULL REFERENCES gateway_group(id),
    server_id VARCHAR(64) NOT NULL REFERENCES gateway_mcp_server(id),
    tool_name VARCHAR(256) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    operation_id VARCHAR(64),
    remote_mount_id VARCHAR(64),
    content JSONB NOT NULL DEFAULT '{}'::jsonb,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    revision BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT ck_gateway_mcp_tool_source CHECK (
        source_type IN ('LOCAL_OPERATION', 'REMOTE_MCP')
    ),
    CONSTRAINT ck_gateway_mcp_tool_binding CHECK (
        (source_type = 'LOCAL_OPERATION'
            AND operation_id IS NOT NULL
            AND remote_mount_id IS NULL)
        OR
        (source_type = 'REMOTE_MCP'
            AND operation_id IS NULL
            AND remote_mount_id IS NOT NULL)
    ),
    CONSTRAINT ck_gateway_mcp_tool_content
        CHECK (jsonb_typeof(content) = 'object'),
    CONSTRAINT ck_gateway_mcp_tool_revision CHECK (revision >= 0)
);

CREATE UNIQUE INDEX uk_gateway_mcp_tool_name_active
    ON gateway_mcp_tool_draft (server_id, tool_name)
    WHERE deleted = FALSE;
CREATE INDEX idx_gateway_mcp_tool_group_server
    ON gateway_mcp_tool_draft (gateway_group_id, server_id, enabled)
    WHERE deleted = FALSE;
CREATE INDEX idx_gateway_mcp_tool_operation
    ON gateway_mcp_tool_draft (operation_id)
    WHERE deleted = FALSE AND operation_id IS NOT NULL;

CREATE TABLE gateway_mcp_resource_draft (
    id VARCHAR(64) PRIMARY KEY,
    gateway_group_id VARCHAR(64) NOT NULL REFERENCES gateway_group(id),
    server_id VARCHAR(64) NOT NULL REFERENCES gateway_mcp_server(id),
    resource_name VARCHAR(256) NOT NULL,
    resource_uri VARCHAR(1024) NOT NULL,
    driver_type VARCHAR(32) NOT NULL,
    operation_id VARCHAR(64),
    remote_mount_id VARCHAR(64),
    content JSONB NOT NULL DEFAULT '{}'::jsonb,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    revision BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT ck_gateway_mcp_resource_driver CHECK (
        driver_type IN (
            'LOCAL_OPERATION', 'LOCAL_DICTIONARY', 'APP_ARTIFACT',
            'REMOTE_MCP'
        )
    ),
    CONSTRAINT ck_gateway_mcp_resource_content
        CHECK (jsonb_typeof(content) = 'object'),
    CONSTRAINT ck_gateway_mcp_resource_revision CHECK (revision >= 0)
);

CREATE UNIQUE INDEX uk_gateway_mcp_resource_name_active
    ON gateway_mcp_resource_draft (server_id, resource_name)
    WHERE deleted = FALSE;
CREATE UNIQUE INDEX uk_gateway_mcp_resource_uri_active
    ON gateway_mcp_resource_draft (server_id, resource_uri)
    WHERE deleted = FALSE;
CREATE INDEX idx_gateway_mcp_resource_group_server
    ON gateway_mcp_resource_draft (gateway_group_id, server_id, enabled)
    WHERE deleted = FALSE;

CREATE TABLE gateway_mcp_resource_template_draft (
    id VARCHAR(64) PRIMARY KEY,
    gateway_group_id VARCHAR(64) NOT NULL REFERENCES gateway_group(id),
    server_id VARCHAR(64) NOT NULL REFERENCES gateway_mcp_server(id),
    template_name VARCHAR(256) NOT NULL,
    uri_template VARCHAR(2048) NOT NULL,
    driver_type VARCHAR(32) NOT NULL,
    operation_id VARCHAR(64),
    remote_mount_id VARCHAR(64),
    content JSONB NOT NULL DEFAULT '{}'::jsonb,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    revision BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT ck_gateway_mcp_template_driver CHECK (
        driver_type IN (
            'LOCAL_OPERATION', 'LOCAL_DICTIONARY', 'APP_ARTIFACT',
            'REMOTE_MCP'
        )
    ),
    CONSTRAINT ck_gateway_mcp_template_content
        CHECK (jsonb_typeof(content) = 'object'),
    CONSTRAINT ck_gateway_mcp_template_revision CHECK (revision >= 0)
);

CREATE UNIQUE INDEX uk_gateway_mcp_template_name_active
    ON gateway_mcp_resource_template_draft (server_id, template_name)
    WHERE deleted = FALSE;
CREATE UNIQUE INDEX uk_gateway_mcp_template_uri_active
    ON gateway_mcp_resource_template_draft (server_id, uri_template)
    WHERE deleted = FALSE;
CREATE INDEX idx_gateway_mcp_template_group_server
    ON gateway_mcp_resource_template_draft
        (gateway_group_id, server_id, enabled)
    WHERE deleted = FALSE;

CREATE TABLE gateway_mcp_prompt_draft (
    id VARCHAR(64) PRIMARY KEY,
    gateway_group_id VARCHAR(64) NOT NULL REFERENCES gateway_group(id),
    server_id VARCHAR(64) NOT NULL REFERENCES gateway_mcp_server(id),
    prompt_name VARCHAR(256) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    operation_id VARCHAR(64),
    remote_mount_id VARCHAR(64),
    content JSONB NOT NULL DEFAULT '{}'::jsonb,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    revision BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT ck_gateway_mcp_prompt_source CHECK (
        source_type IN ('LOCAL_TEMPLATE', 'LOCAL_OPERATION', 'REMOTE_MCP')
    ),
    CONSTRAINT ck_gateway_mcp_prompt_content
        CHECK (jsonb_typeof(content) = 'object'),
    CONSTRAINT ck_gateway_mcp_prompt_revision CHECK (revision >= 0)
);

CREATE UNIQUE INDEX uk_gateway_mcp_prompt_name_active
    ON gateway_mcp_prompt_draft (server_id, prompt_name)
    WHERE deleted = FALSE;
CREATE INDEX idx_gateway_mcp_prompt_group_server
    ON gateway_mcp_prompt_draft (gateway_group_id, server_id, enabled)
    WHERE deleted = FALSE;

CREATE TABLE gateway_mcp_task_policy_draft (
    id VARCHAR(64) PRIMARY KEY,
    gateway_group_id VARCHAR(64) NOT NULL REFERENCES gateway_group(id),
    server_id VARCHAR(64) NOT NULL REFERENCES gateway_mcp_server(id),
    tool_name VARCHAR(256) NOT NULL,
    content JSONB NOT NULL DEFAULT '{}'::jsonb,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    revision BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT ck_gateway_mcp_task_policy_content
        CHECK (jsonb_typeof(content) = 'object'),
    CONSTRAINT ck_gateway_mcp_task_policy_revision CHECK (revision >= 0)
);

CREATE UNIQUE INDEX uk_gateway_mcp_task_policy_active
    ON gateway_mcp_task_policy_draft (server_id, tool_name)
    WHERE deleted = FALSE;
CREATE INDEX idx_gateway_mcp_task_policy_group
    ON gateway_mcp_task_policy_draft (gateway_group_id, server_id)
    WHERE deleted = FALSE;

CREATE TABLE gateway_mcp_app_artifact (
    id VARCHAR(64) PRIMARY KEY,
    gateway_group_id VARCHAR(64) NOT NULL REFERENCES gateway_group(id),
    app_code VARCHAR(128) NOT NULL,
    app_version VARCHAR(64) NOT NULL,
    display_name VARCHAR(256) NOT NULL,
    resource_uri VARCHAR(1024) NOT NULL,
    artifact_reference VARCHAR(1024) NOT NULL,
    artifact_sha256 VARCHAR(64) NOT NULL,
    size_bytes BIGINT NOT NULL,
    mime_type VARCHAR(128) NOT NULL,
    content_security_policy TEXT NOT NULL,
    permission_manifest JSONB NOT NULL DEFAULT '[]'::jsonb,
    allowed_origins JSONB NOT NULL DEFAULT '[]'::jsonb,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    CONSTRAINT ck_gateway_mcp_app_sha CHECK (
        length(artifact_sha256) = 64
    ),
    CONSTRAINT ck_gateway_mcp_app_size CHECK (
        size_bytes >= 0 AND size_bytes <= 16777216
    ),
    CONSTRAINT ck_gateway_mcp_app_mime CHECK (
        mime_type = 'text/html;profile=mcp-app'
    ),
    CONSTRAINT ck_gateway_mcp_app_status CHECK (
        status IN ('ACTIVE', 'REVOKED')
    ),
    CONSTRAINT ck_gateway_mcp_app_permissions CHECK (
        jsonb_typeof(permission_manifest) = 'array'
    ),
    CONSTRAINT ck_gateway_mcp_app_origins CHECK (
        jsonb_typeof(allowed_origins) = 'array'
    )
);

CREATE UNIQUE INDEX uk_gateway_mcp_app_version
    ON gateway_mcp_app_artifact (app_code, app_version);
CREATE UNIQUE INDEX uk_gateway_mcp_app_resource_uri
    ON gateway_mcp_app_artifact (resource_uri);
CREATE INDEX idx_gateway_mcp_app_group_status
    ON gateway_mcp_app_artifact (gateway_group_id, status, created_at DESC);

CREATE TABLE gateway_mcp_app_binding_draft (
    id VARCHAR(64) PRIMARY KEY,
    gateway_group_id VARCHAR(64) NOT NULL REFERENCES gateway_group(id),
    server_id VARCHAR(64) NOT NULL REFERENCES gateway_mcp_server(id),
    tool_name VARCHAR(256) NOT NULL,
    app_artifact_id VARCHAR(64) NOT NULL
        REFERENCES gateway_mcp_app_artifact(id),
    content JSONB NOT NULL DEFAULT '{}'::jsonb,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    revision BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT ck_gateway_mcp_app_binding_content
        CHECK (jsonb_typeof(content) = 'object'),
    CONSTRAINT ck_gateway_mcp_app_binding_revision CHECK (revision >= 0)
);

CREATE UNIQUE INDEX uk_gateway_mcp_app_binding_active
    ON gateway_mcp_app_binding_draft
        (server_id, tool_name, app_artifact_id)
    WHERE deleted = FALSE;
CREATE INDEX idx_gateway_mcp_app_binding_group
    ON gateway_mcp_app_binding_draft (gateway_group_id, server_id)
    WHERE deleted = FALSE;

CREATE TABLE gateway_mcp_remote_provider (
    id VARCHAR(64) PRIMARY KEY,
    gateway_group_id VARCHAR(64) NOT NULL REFERENCES gateway_group(id),
    provider_code VARCHAR(128) NOT NULL,
    display_name VARCHAR(256) NOT NULL,
    dialect VARCHAR(32) NOT NULL,
    transport_type VARCHAR(32) NOT NULL,
    endpoint_reference VARCHAR(1024) NOT NULL,
    auth_profile_reference VARCHAR(512),
    tls_profile_reference VARCHAR(512),
    capability_fingerprint VARCHAR(128),
    status VARCHAR(32) NOT NULL DEFAULT 'CONFIGURED',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    revision BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT ck_gateway_mcp_provider_dialect CHECK (
        dialect IN (
            'STABLE_2025_11_25', 'RC_2026_07_28', 'LEGACY_2024_SSE'
        )
    ),
    CONSTRAINT ck_gateway_mcp_provider_transport CHECK (
        transport_type IN (
            'STREAMABLE_HTTP', 'LEGACY_SSE', 'STDIO_MANAGED'
        )
    ),
    CONSTRAINT ck_gateway_mcp_provider_status CHECK (
        status IN ('CONFIGURED', 'SYNCED', 'DEGRADED', 'DISABLED')
    ),
    CONSTRAINT ck_gateway_mcp_provider_revision CHECK (revision >= 0)
);

CREATE UNIQUE INDEX uk_gateway_mcp_provider_code_active
    ON gateway_mcp_remote_provider (gateway_group_id, provider_code)
    WHERE deleted = FALSE;
CREATE INDEX idx_gateway_mcp_provider_group_status
    ON gateway_mcp_remote_provider (gateway_group_id, status, enabled)
    WHERE deleted = FALSE;

CREATE TABLE gateway_mcp_remote_capability (
    id VARCHAR(64) PRIMARY KEY,
    provider_id VARCHAR(64) NOT NULL
        REFERENCES gateway_mcp_remote_provider(id),
    primitive_type VARCHAR(32) NOT NULL,
    remote_name VARCHAR(512) NOT NULL,
    descriptor JSONB NOT NULL DEFAULT '{}'::jsonb,
    capability_fingerprint VARCHAR(128) NOT NULL,
    synced_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_gateway_mcp_remote_primitive CHECK (
        primitive_type IN (
            'TOOL', 'RESOURCE', 'RESOURCE_TEMPLATE', 'PROMPT', 'APP'
        )
    ),
    CONSTRAINT ck_gateway_mcp_remote_descriptor
        CHECK (jsonb_typeof(descriptor) = 'object')
);

CREATE UNIQUE INDEX uk_gateway_mcp_remote_capability
    ON gateway_mcp_remote_capability
        (provider_id, primitive_type, remote_name);
CREATE INDEX idx_gateway_mcp_remote_capability_fingerprint
    ON gateway_mcp_remote_capability
        (provider_id, capability_fingerprint);

CREATE TABLE gateway_mcp_remote_mount_draft (
    id VARCHAR(64) PRIMARY KEY,
    gateway_group_id VARCHAR(64) NOT NULL REFERENCES gateway_group(id),
    server_id VARCHAR(64) NOT NULL REFERENCES gateway_mcp_server(id),
    provider_id VARCHAR(64) NOT NULL
        REFERENCES gateway_mcp_remote_provider(id),
    namespace VARCHAR(256) NOT NULL,
    capability_fingerprint VARCHAR(128) NOT NULL,
    content JSONB NOT NULL DEFAULT '{}'::jsonb,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    revision BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT ck_gateway_mcp_mount_content
        CHECK (jsonb_typeof(content) = 'object'),
    CONSTRAINT ck_gateway_mcp_mount_revision CHECK (revision >= 0)
);

CREATE UNIQUE INDEX uk_gateway_mcp_mount_namespace_active
    ON gateway_mcp_remote_mount_draft (server_id, namespace)
    WHERE deleted = FALSE;
CREATE INDEX idx_gateway_mcp_mount_group_provider
    ON gateway_mcp_remote_mount_draft
        (gateway_group_id, provider_id, enabled)
    WHERE deleted = FALSE;

CREATE TABLE gateway_mcp_approval (
    id VARCHAR(64) PRIMARY KEY,
    token_digest VARCHAR(64) NOT NULL,
    subject_id VARCHAR(128) NOT NULL,
    tenant_id VARCHAR(128) NOT NULL,
    client_id VARCHAR(128) NOT NULL,
    server_code VARCHAR(128) NOT NULL,
    tool_name VARCHAR(256) NOT NULL,
    argument_digest VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    revision BIGINT NOT NULL DEFAULT 0,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    CONSTRAINT uk_gateway_mcp_approval_digest UNIQUE (token_digest),
    CONSTRAINT ck_gateway_mcp_approval_token_digest CHECK (
        length(token_digest) = 64
    ),
    CONSTRAINT ck_gateway_mcp_approval_argument_digest CHECK (
        length(argument_digest) = 64
    ),
    CONSTRAINT ck_gateway_mcp_approval_status CHECK (
        status IN ('PENDING', 'CONSUMED', 'EXPIRED', 'REVOKED')
    ),
    CONSTRAINT ck_gateway_mcp_approval_expiry CHECK (expires_at > issued_at),
    CONSTRAINT ck_gateway_mcp_approval_consumed CHECK (
        (status = 'CONSUMED' AND consumed_at IS NOT NULL)
        OR (status <> 'CONSUMED')
    ),
    CONSTRAINT ck_gateway_mcp_approval_revision CHECK (revision >= 0)
);

CREATE INDEX idx_gateway_mcp_approval_owner
    ON gateway_mcp_approval
        (subject_id, tenant_id, client_id, server_code, tool_name);
CREATE INDEX idx_gateway_mcp_approval_pending_expiry
    ON gateway_mcp_approval (expires_at)
    WHERE status = 'PENDING';

CREATE TABLE gateway_mcp_task_instance (
    id VARCHAR(64) PRIMARY KEY,
    principal_fingerprint VARCHAR(128) NOT NULL,
    subject_id VARCHAR(128) NOT NULL,
    tenant_id VARCHAR(128) NOT NULL,
    client_id VARCHAR(128) NOT NULL,
    server_code VARCHAR(128) NOT NULL,
    tool_name VARCHAR(256) NOT NULL,
    request_digest VARCHAR(64) NOT NULL,
    state VARCHAR(32) NOT NULL,
    input_payload JSONB,
    result_payload JSONB,
    error_payload JSONB,
    worker_owner VARCHAR(256),
    lease_until TIMESTAMPTZ,
    execution_deadline TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL,
    revision BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_gateway_mcp_task_request_digest CHECK (
        length(request_digest) = 64
    ),
    CONSTRAINT ck_gateway_mcp_task_state CHECK (
        state IN (
            'WORKING', 'INPUT_REQUIRED', 'COMPLETED', 'FAILED',
            'CANCELLED'
        )
    ),
    CONSTRAINT ck_gateway_mcp_task_attempts CHECK (
        attempt_count >= 0 AND max_attempts > 0
            AND attempt_count <= max_attempts
    ),
    CONSTRAINT ck_gateway_mcp_task_expiry CHECK (
        expires_at > created_at AND execution_deadline > created_at
    ),
    CONSTRAINT ck_gateway_mcp_task_lease CHECK (
        (worker_owner IS NULL AND lease_until IS NULL)
        OR (worker_owner IS NOT NULL AND lease_until IS NOT NULL)
    ),
    CONSTRAINT ck_gateway_mcp_task_revision CHECK (revision >= 0)
);

CREATE INDEX idx_gateway_mcp_task_owner
    ON gateway_mcp_task_instance
        (principal_fingerprint, tenant_id, client_id, created_at DESC);
CREATE INDEX idx_gateway_mcp_task_status_expiry
    ON gateway_mcp_task_instance (state, expires_at);
CREATE INDEX idx_gateway_mcp_task_pending_worker
    ON gateway_mcp_task_instance
        (lease_until, created_at)
    WHERE state = 'WORKING';

ALTER TABLE gateway_mcp_tool_draft
    ADD CONSTRAINT fk_gateway_mcp_tool_operation
        FOREIGN KEY (operation_id) REFERENCES gateway_operation(id),
    ADD CONSTRAINT fk_gateway_mcp_tool_remote_mount
        FOREIGN KEY (remote_mount_id)
        REFERENCES gateway_mcp_remote_mount_draft(id);

ALTER TABLE gateway_mcp_resource_draft
    ADD CONSTRAINT fk_gateway_mcp_resource_operation
        FOREIGN KEY (operation_id) REFERENCES gateway_operation(id),
    ADD CONSTRAINT fk_gateway_mcp_resource_remote_mount
        FOREIGN KEY (remote_mount_id)
        REFERENCES gateway_mcp_remote_mount_draft(id);

ALTER TABLE gateway_mcp_resource_template_draft
    ADD CONSTRAINT fk_gateway_mcp_template_operation
        FOREIGN KEY (operation_id) REFERENCES gateway_operation(id),
    ADD CONSTRAINT fk_gateway_mcp_template_remote_mount
        FOREIGN KEY (remote_mount_id)
        REFERENCES gateway_mcp_remote_mount_draft(id);

ALTER TABLE gateway_mcp_prompt_draft
    ADD CONSTRAINT fk_gateway_mcp_prompt_operation
        FOREIGN KEY (operation_id) REFERENCES gateway_operation(id),
    ADD CONSTRAINT fk_gateway_mcp_prompt_remote_mount
        FOREIGN KEY (remote_mount_id)
        REFERENCES gateway_mcp_remote_mount_draft(id);
