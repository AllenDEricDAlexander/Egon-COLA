CREATE TABLE gateway_mcp_managed_tool_override (
    tool_id VARCHAR(64) PRIMARY KEY,
    gateway_group_id VARCHAR(64) NOT NULL REFERENCES gateway_group(id),
    operation_id VARCHAR(64) NOT NULL REFERENCES gateway_operation(id),
    server_id VARCHAR(64) REFERENCES gateway_mcp_server(id),
    additional_permissions JSONB NOT NULL DEFAULT '[]'::jsonb,
    minimum_risk_level VARCHAR(16),
    enabled BOOLEAN,
    revision BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT uk_gateway_mcp_managed_tool_operation
        UNIQUE (gateway_group_id, operation_id),
    CONSTRAINT ck_gateway_mcp_managed_tool_permissions
        CHECK (jsonb_typeof(additional_permissions) = 'array'),
    CONSTRAINT ck_gateway_mcp_managed_tool_risk CHECK (
        minimum_risk_level IS NULL
        OR minimum_risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')
    ),
    CONSTRAINT ck_gateway_mcp_managed_tool_enabled
        CHECK (enabled IS NULL OR enabled = FALSE),
    CONSTRAINT ck_gateway_mcp_managed_tool_override CHECK (
        server_id IS NOT NULL
        OR jsonb_array_length(additional_permissions) > 0
        OR minimum_risk_level IS NOT NULL
        OR enabled = FALSE
    ),
    CONSTRAINT ck_gateway_mcp_managed_tool_revision CHECK (revision >= 0)
);

CREATE INDEX idx_gateway_mcp_managed_tool_group
    ON gateway_mcp_managed_tool_override (gateway_group_id, operation_id);

CREATE TABLE gateway_mcp_remote_tool_draft (
    id VARCHAR(64) PRIMARY KEY,
    gateway_group_id VARCHAR(64) NOT NULL REFERENCES gateway_group(id),
    server_id VARCHAR(64) NOT NULL REFERENCES gateway_mcp_server(id),
    tool_name VARCHAR(256) NOT NULL,
    remote_mount_id VARCHAR(64) NOT NULL
        REFERENCES gateway_mcp_remote_mount_draft(id),
    content JSONB NOT NULL DEFAULT '{}'::jsonb,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    revision BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT ck_gateway_mcp_remote_tool_content
        CHECK (jsonb_typeof(content) = 'object'),
    CONSTRAINT ck_gateway_mcp_remote_tool_revision CHECK (revision >= 0)
);

CREATE UNIQUE INDEX uk_gateway_mcp_remote_tool_name_active
    ON gateway_mcp_remote_tool_draft (server_id, tool_name)
    WHERE deleted = FALSE;
CREATE INDEX idx_gateway_mcp_remote_tool_group_server
    ON gateway_mcp_remote_tool_draft
        (gateway_group_id, server_id, enabled)
    WHERE deleted = FALSE;
CREATE INDEX idx_gateway_mcp_remote_tool_mount
    ON gateway_mcp_remote_tool_draft (remote_mount_id)
    WHERE deleted = FALSE;

INSERT INTO gateway_mcp_remote_tool_draft(
    id, gateway_group_id, server_id, tool_name, remote_mount_id,
    content, enabled, revision, deleted,
    created_at, created_by, updated_at, updated_by
)
SELECT id, gateway_group_id, server_id, tool_name, remote_mount_id,
       content - 'sourceType' - 'operationId' - 'remoteMountId',
       enabled, revision, deleted,
       created_at, created_by, updated_at, updated_by
  FROM gateway_mcp_tool_draft
 WHERE source_type = 'REMOTE_MCP';

DROP TABLE gateway_mcp_tool_draft;
