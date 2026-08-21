ALTER TABLE identity_client
    ADD COLUMN app_id VARCHAR(128);

ALTER TABLE identity_client
    ADD CONSTRAINT ck_identity_client_confidential_app_id
        CHECK (client_type <> 'CONFIDENTIAL' OR app_id IS NOT NULL)
        NOT VALID;

CREATE UNIQUE INDEX uq_identity_client_confidential_app_id
    ON identity_client(app_id)
    WHERE client_type = 'CONFIDENTIAL' AND app_id IS NOT NULL;

CREATE INDEX idx_identity_client_status_type
    ON identity_client(status, client_type);

CREATE TABLE identity_client_secret (
    id          VARCHAR(64) PRIMARY KEY,
    client_id   VARCHAR(128) NOT NULL
        REFERENCES identity_client(client_id) ON DELETE RESTRICT,
    secret_hash VARCHAR(512) NOT NULL,
    secret_hint VARCHAR(8) NOT NULL,
    status      VARCHAR(32) NOT NULL,
    version     BIGINT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    CONSTRAINT ck_identity_client_secret_status
        CHECK (status IN ('ACTIVE', 'REVOKED')),
    CONSTRAINT ck_identity_client_secret_version
        CHECK (version >= 0),
    CONSTRAINT ck_identity_client_secret_revoked_at
        CHECK (
            (status = 'ACTIVE' AND revoked_at IS NULL)
            OR
            (status = 'REVOKED' AND revoked_at IS NOT NULL)
        )
);

CREATE UNIQUE INDEX uq_identity_client_active_secret
    ON identity_client_secret(client_id)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_identity_client_secret_history
    ON identity_client_secret(client_id, created_at DESC);

CREATE TABLE identity_tenant (
    id         VARCHAR(64) PRIMARY KEY,
    tenant_code VARCHAR(64) NOT NULL,
    tenant_name VARCHAR(200) NOT NULL,
    status     VARCHAR(32) NOT NULL,
    settings   JSONB NOT NULL DEFAULT '{}'::JSONB,
    version    BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    CONSTRAINT ck_identity_tenant_id
        CHECK (id ~ '^[0-9]+$'),
    CONSTRAINT ck_identity_tenant_status
        CHECK (status IN ('INITIALIZING', 'ACTIVE', 'SUSPENDED', 'CLOSED')),
    CONSTRAINT ck_identity_tenant_settings
        CHECK (jsonb_typeof(settings) = 'object'),
    CONSTRAINT ck_identity_tenant_settings_size
        CHECK (pg_column_size(settings) <= 65536),
    CONSTRAINT ck_identity_tenant_version
        CHECK (version >= 0)
);

CREATE UNIQUE INDEX uq_identity_tenant_code_lower
    ON identity_tenant(LOWER(tenant_code));

CREATE INDEX idx_identity_tenant_status_updated
    ON identity_tenant(status, updated_at DESC, id);

CREATE TABLE identity_tenant_membership (
    id           VARCHAR(64) PRIMARY KEY,
    tenant_id    VARCHAR(64) NOT NULL
        REFERENCES identity_tenant(id) ON DELETE RESTRICT,
    identity_sub VARCHAR(64) NOT NULL
        REFERENCES identity_user(id) ON DELETE RESTRICT,
    status       VARCHAR(32) NOT NULL,
    version      BIGINT NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    created_by   VARCHAR(128) NOT NULL,
    updated_by   VARCHAR(128) NOT NULL,
    CONSTRAINT ck_identity_tenant_membership_status
        CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_identity_tenant_membership_version
        CHECK (version >= 0),
    CONSTRAINT uq_identity_tenant_member
        UNIQUE (tenant_id, identity_sub)
);

CREATE INDEX idx_identity_membership_by_subject
    ON identity_tenant_membership(identity_sub, status, tenant_id);

CREATE INDEX idx_identity_membership_by_tenant
    ON identity_tenant_membership(tenant_id, status, identity_sub);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM identity_client_resource_grant
        WHERE grant_type = 'CLIENT_CREDENTIALS'
          AND (
              tenant_id IS NULL
              OR tenant_id !~ '^[0-9]+$'
              OR length(tenant_id) > 54
          )
    ) THEN
        RAISE EXCEPTION
            'V5 cannot adopt a non-canonical CLIENT_CREDENTIALS tenant_id';
    END IF;
END
$$;

INSERT INTO identity_tenant (
    id,
    tenant_code,
    tenant_name,
    status,
    settings,
    version,
    created_at,
    updated_at,
    created_by,
    updated_by
)
SELECT DISTINCT
    tenant_id,
    'migrating-' || tenant_id,
    'Migrating tenant ' || tenant_id,
    'INITIALIZING',
    '{}'::JSONB,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'flyway-v5',
    'flyway-v5'
FROM identity_client_resource_grant
WHERE grant_type = 'CLIENT_CREDENTIALS'
ON CONFLICT (id) DO NOTHING;

ALTER TABLE identity_client_resource_grant
    ADD COLUMN grant_context VARCHAR(16);

UPDATE identity_client_resource_grant
SET grant_context = CASE
    WHEN grant_type = 'CLIENT_CREDENTIALS' THEN 'TENANT'
    ELSE NULL
END;

DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    FOR constraint_name IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'identity_client_resource_grant'::regclass
          AND contype = 'c'
    LOOP
        EXECUTE format(
            'ALTER TABLE identity_client_resource_grant DROP CONSTRAINT %I',
            constraint_name
        );
    END LOOP;
END
$$;

ALTER TABLE identity_client_resource_grant
    ADD CONSTRAINT fk_identity_grant_tenant
        FOREIGN KEY (tenant_id) REFERENCES identity_tenant(id)
        ON DELETE RESTRICT,
    ADD CONSTRAINT ck_identity_grant_type
        CHECK (grant_type IN ('USER_DELEGATION', 'CLIENT_CREDENTIALS')),
    ADD CONSTRAINT ck_identity_grant_status
        CHECK (status IN ('ACTIVE', 'DISABLED')),
    ADD CONSTRAINT ck_identity_grant_scopes_array
        CHECK (jsonb_typeof(allowed_scopes) = 'array'),
    ADD CONSTRAINT ck_identity_grant_context_shape
        CHECK (
            (
                grant_type = 'USER_DELEGATION'
                AND grant_context IS NULL
                AND tenant_id IS NULL
                AND allowed_scopes = '[]'::JSONB
            )
            OR
            (
                grant_type = 'CLIENT_CREDENTIALS'
                AND grant_context = 'TENANT'
                AND tenant_id IS NOT NULL
                AND jsonb_array_length(allowed_scopes) > 0
            )
            OR
            (
                grant_type = 'CLIENT_CREDENTIALS'
                AND grant_context = 'PLATFORM'
                AND tenant_id IS NULL
                AND jsonb_array_length(allowed_scopes) > 0
            )
        ),
    ADD CONSTRAINT ck_identity_grant_version
        CHECK (version >= 0);

DROP INDEX uq_identity_service_resource_grant;

CREATE UNIQUE INDEX uq_identity_tenant_service_grant
    ON identity_client_resource_grant(client_id, resource_server_id, tenant_id)
    WHERE grant_type = 'CLIENT_CREDENTIALS'
      AND grant_context = 'TENANT';

CREATE UNIQUE INDEX uq_identity_platform_service_grant
    ON identity_client_resource_grant(client_id, resource_server_id)
    WHERE grant_type = 'CLIENT_CREDENTIALS'
      AND grant_context = 'PLATFORM';

ALTER TABLE identity_resource_server
    DROP COLUMN admission_ticket_ttl_seconds CASCADE;

DROP TABLE identity_client_jwk;
