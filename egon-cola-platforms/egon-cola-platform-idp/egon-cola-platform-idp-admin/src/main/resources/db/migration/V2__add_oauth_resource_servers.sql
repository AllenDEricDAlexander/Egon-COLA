CREATE TABLE identity_resource_server (
    id                           VARCHAR(64) PRIMARY KEY,
    resource_server_id           VARCHAR(128) NOT NULL,
    resource_uri                 VARCHAR(2048) NOT NULL,
    biz_code                     VARCHAR(128) NOT NULL,
    app_code                     VARCHAR(128) NOT NULL,
    environment                  VARCHAR(128) NOT NULL,
    display_name                 VARCHAR(200) NOT NULL,
    management_client_id         VARCHAR(128) NOT NULL REFERENCES identity_client(client_id),
    rbac_application_code        VARCHAR(128) NOT NULL,
    entry_permission_code        VARCHAR(256) NOT NULL,
    admission_ticket_ttl_seconds INTEGER NOT NULL,
    status                       VARCHAR(32) NOT NULL,
    version                      BIGINT NOT NULL DEFAULT 0,
    created_at                   TIMESTAMPTZ NOT NULL,
    updated_at                   TIMESTAMPTZ NOT NULL,
    UNIQUE (resource_server_id),
    UNIQUE (resource_uri),
    UNIQUE (biz_code, app_code, environment),
    UNIQUE (management_client_id),
    CHECK (admission_ticket_ttl_seconds BETWEEN 30 AND 900),
    CHECK (status IN ('ACTIVE', 'DISABLED')),
    CHECK (version >= 0)
);

CREATE TABLE identity_client_jwk (
    id           VARCHAR(64) PRIMARY KEY,
    client_id    VARCHAR(128) NOT NULL REFERENCES identity_client(client_id),
    kid          VARCHAR(128) NOT NULL,
    algorithm    VARCHAR(32) NOT NULL,
    public_jwk   JSONB NOT NULL,
    valid_from   TIMESTAMPTZ NOT NULL,
    valid_to     TIMESTAMPTZ NOT NULL,
    status       VARCHAR(32) NOT NULL,
    last_used_at TIMESTAMPTZ,
    version      BIGINT NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    UNIQUE (client_id, kid),
    CHECK (algorithm IN ('RS256')),
    CHECK (status IN ('ACTIVE', 'DISABLED')),
    CHECK (valid_to > valid_from),
    CHECK (last_used_at IS NULL OR last_used_at >= valid_from),
    CHECK (version >= 0)
);

CREATE TABLE identity_client_resource_grant (
    id                 VARCHAR(64) PRIMARY KEY,
    client_id          VARCHAR(128) NOT NULL REFERENCES identity_client(client_id),
    resource_server_id VARCHAR(128) NOT NULL REFERENCES identity_resource_server(resource_server_id),
    grant_type         VARCHAR(32) NOT NULL,
    tenant_id          VARCHAR(64),
    allowed_scopes     JSONB NOT NULL DEFAULT '[]'::JSONB,
    status             VARCHAR(32) NOT NULL,
    version            BIGINT NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ NOT NULL,
    updated_at         TIMESTAMPTZ NOT NULL,
    CHECK (grant_type IN ('USER_DELEGATION', 'CLIENT_CREDENTIALS')),
    CHECK (status IN ('ACTIVE', 'DISABLED')),
    CHECK (jsonb_typeof(allowed_scopes) = 'array'),
    CHECK (
        (grant_type = 'USER_DELEGATION' AND tenant_id IS NULL
            AND allowed_scopes = '[]'::JSONB)
        OR
        (grant_type = 'CLIENT_CREDENTIALS' AND tenant_id IS NOT NULL
            AND jsonb_array_length(allowed_scopes) > 0)
    ),
    CHECK (version >= 0)
);

CREATE UNIQUE INDEX uq_identity_user_resource_grant
    ON identity_client_resource_grant(client_id, resource_server_id)
    WHERE grant_type = 'USER_DELEGATION';

CREATE UNIQUE INDEX uq_identity_service_resource_grant
    ON identity_client_resource_grant(client_id, resource_server_id, tenant_id)
    WHERE grant_type = 'CLIENT_CREDENTIALS';

CREATE INDEX idx_identity_resource_scope
    ON identity_resource_server(biz_code, app_code, environment, status);

CREATE INDEX idx_identity_resource_grant_lookup
    ON identity_client_resource_grant(
        client_id,
        resource_server_id,
        grant_type,
        status
    );

DROP TABLE identity_client_audience;
