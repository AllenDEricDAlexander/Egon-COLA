-- The schema is created and owned by operations. EgonColaPostgreDdlRunner sets
-- a transaction-local search_path to the empty tianquan_oauth schema.

CREATE TABLE oauth_identity_user (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    create_time TIMESTAMPTZ(6) NOT NULL,
    create_user_id VARCHAR(128) NOT NULL,
    update_time TIMESTAMPTZ(6) NOT NULL,
    update_user_id VARCHAR(128) NOT NULL,
    deleted_at TIMESTAMP(6),
    identity_sub VARCHAR(64) NOT NULL,
    username VARCHAR(128) NOT NULL,
    username_normalized VARCHAR(128) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    failed_login_count INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ(6),
    last_login_at TIMESTAMPTZ(6),
    CONSTRAINT pk_oauth_identity_user PRIMARY KEY (id),
    CONSTRAINT ck_oauth_identity_user_partition CHECK (tenant_id > 0),
    CONSTRAINT ck_oauth_identity_user_version CHECK (version >= 0),
    CONSTRAINT ck_oauth_identity_user_failed_logins CHECK (failed_login_count >= 0),
    CONSTRAINT ck_oauth_identity_user_status CHECK (status IN ('ACTIVE', 'DISABLED', 'LOCKED')),
    CONSTRAINT uk_oauth_identity_user_subject_permanent UNIQUE (tenant_id, identity_sub)
);
CREATE UNIQUE INDEX uk_oauth_identity_user_sub_life ON oauth_identity_user (tenant_id, identity_sub, deleted_at);
CREATE UNIQUE INDEX uk_oauth_identity_user_sub_active ON oauth_identity_user (tenant_id, identity_sub) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_oauth_identity_user_name_life ON oauth_identity_user (tenant_id, username_normalized, deleted_at);
CREATE UNIQUE INDEX uk_oauth_identity_user_name_active ON oauth_identity_user (tenant_id, username_normalized) WHERE deleted_at IS NULL;
CREATE INDEX ix_oauth_identity_user_lookup ON oauth_identity_user (tenant_id, username_normalized, identity_sub) WHERE deleted_at IS NULL;

CREATE TABLE oauth_identity_credential (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    create_time TIMESTAMPTZ(6) NOT NULL,
    create_user_id VARCHAR(128) NOT NULL,
    update_time TIMESTAMPTZ(6) NOT NULL,
    update_user_id VARCHAR(128) NOT NULL,
    deleted_at TIMESTAMP(6),
    legacy_id VARCHAR(64) NOT NULL,
    identity_sub VARCHAR(64) NOT NULL,
    credential_type VARCHAR(32) NOT NULL DEFAULT 'PASSWORD',
    password_hash VARCHAR(512) NOT NULL,
    password_changed_at TIMESTAMPTZ(6) NOT NULL,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT pk_oauth_identity_credential PRIMARY KEY (id),
    CONSTRAINT ck_oauth_identity_credential_partition CHECK (tenant_id > 0),
    CONSTRAINT ck_oauth_identity_credential_version CHECK (version >= 0),
    CONSTRAINT ck_oauth_identity_credential_type CHECK (credential_type = 'PASSWORD'),
    CONSTRAINT ck_oauth_identity_credential_status CHECK (status IN ('ACTIVE', 'REVOKED')),
    CONSTRAINT uk_oauth_identity_credential_legacy_permanent UNIQUE (tenant_id, legacy_id)
);
CREATE UNIQUE INDEX uk_oauth_identity_credential_legacy_life ON oauth_identity_credential (tenant_id, legacy_id, deleted_at);
CREATE UNIQUE INDEX uk_oauth_identity_credential_legacy_active ON oauth_identity_credential (tenant_id, legacy_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_oauth_identity_credential_sub_life ON oauth_identity_credential (tenant_id, identity_sub, credential_type, deleted_at);
CREATE UNIQUE INDEX uk_oauth_identity_credential_sub_active ON oauth_identity_credential (tenant_id, identity_sub, credential_type) WHERE deleted_at IS NULL;
CREATE INDEX ix_oauth_identity_credential_lookup ON oauth_identity_credential (tenant_id, identity_sub, credential_type) WHERE deleted_at IS NULL;

CREATE TABLE oauth_identity_tenant (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    create_time TIMESTAMPTZ(6) NOT NULL,
    create_user_id VARCHAR(128) NOT NULL,
    update_time TIMESTAMPTZ(6) NOT NULL,
    update_user_id VARCHAR(128) NOT NULL,
    deleted_at TIMESTAMP(6),
    identity_tenant_id VARCHAR(64) NOT NULL,
    tenant_code VARCHAR(64) NOT NULL,
    tenant_name VARCHAR(200) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'INITIALIZING',
    settings JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT pk_oauth_identity_tenant PRIMARY KEY (id),
    CONSTRAINT ck_oauth_identity_tenant_partition CHECK (tenant_id > 0),
    CONSTRAINT ck_oauth_identity_tenant_version CHECK (version >= 0),
    CONSTRAINT ck_oauth_identity_tenant_external_id CHECK (
        identity_tenant_id ~ '^[1-9][0-9]*$'
        AND length(identity_tenant_id) <= 19
        AND (length(identity_tenant_id) < 19 OR identity_tenant_id <= '9223372036854775807')
    ),
    CONSTRAINT ck_oauth_identity_tenant_code CHECK (tenant_code ~ '^[a-z][a-z0-9-]{2,63}$'),
    CONSTRAINT ck_oauth_identity_tenant_status CHECK (status IN ('INITIALIZING', 'ACTIVE', 'SUSPENDED', 'CLOSED')),
    CONSTRAINT ck_oauth_identity_tenant_settings CHECK (jsonb_typeof(settings) = 'object' AND pg_column_size(settings) <= 65536),
    CONSTRAINT uk_oauth_identity_tenant_external_permanent UNIQUE (tenant_id, identity_tenant_id)
);
CREATE UNIQUE INDEX uk_oauth_identity_tenant_id_life ON oauth_identity_tenant (tenant_id, identity_tenant_id, deleted_at);
CREATE UNIQUE INDEX uk_oauth_identity_tenant_id_active ON oauth_identity_tenant (tenant_id, identity_tenant_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_oauth_identity_tenant_code_life ON oauth_identity_tenant (tenant_id, lower(tenant_code), deleted_at);
CREATE UNIQUE INDEX uk_oauth_identity_tenant_code_active ON oauth_identity_tenant (tenant_id, lower(tenant_code)) WHERE deleted_at IS NULL;
CREATE INDEX ix_oauth_identity_tenant_lookup ON oauth_identity_tenant (tenant_id, status, update_time DESC, identity_tenant_id) WHERE deleted_at IS NULL;

CREATE TABLE oauth_identity_membership (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    create_time TIMESTAMPTZ(6) NOT NULL,
    create_user_id VARCHAR(128) NOT NULL,
    update_time TIMESTAMPTZ(6) NOT NULL,
    update_user_id VARCHAR(128) NOT NULL,
    deleted_at TIMESTAMP(6),
    membership_id VARCHAR(64) NOT NULL,
    identity_tenant_id VARCHAR(64) NOT NULL,
    identity_sub VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT pk_oauth_identity_membership PRIMARY KEY (id),
    CONSTRAINT ck_oauth_identity_membership_partition CHECK (tenant_id > 0),
    CONSTRAINT ck_oauth_identity_membership_version CHECK (version >= 0),
    CONSTRAINT ck_oauth_identity_membership_tenant_id CHECK (
        identity_tenant_id ~ '^[1-9][0-9]*$'
        AND length(identity_tenant_id) <= 19
        AND (length(identity_tenant_id) < 19 OR identity_tenant_id <= '9223372036854775807')
    ),
    CONSTRAINT ck_oauth_identity_membership_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT uk_oauth_identity_membership_external_permanent UNIQUE (tenant_id, membership_id)
);
CREATE UNIQUE INDEX uk_oauth_identity_membership_id_life ON oauth_identity_membership (tenant_id, membership_id, deleted_at);
CREATE UNIQUE INDEX uk_oauth_identity_membership_id_active ON oauth_identity_membership (tenant_id, membership_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_oauth_identity_membership_tenant_sub_life ON oauth_identity_membership (tenant_id, identity_tenant_id, identity_sub, deleted_at);
CREATE UNIQUE INDEX uk_oauth_identity_membership_tenant_sub_active ON oauth_identity_membership (tenant_id, identity_tenant_id, identity_sub) WHERE deleted_at IS NULL;
CREATE INDEX ix_oauth_identity_membership_by_subject ON oauth_identity_membership (tenant_id, identity_sub, update_time DESC, identity_tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_oauth_identity_membership_by_tenant ON oauth_identity_membership (tenant_id, identity_tenant_id, update_time DESC, identity_sub) WHERE deleted_at IS NULL;

CREATE TABLE oauth_realm (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    create_time TIMESTAMPTZ(6) NOT NULL,
    create_user_id VARCHAR(128) NOT NULL,
    update_time TIMESTAMPTZ(6) NOT NULL,
    update_user_id VARCHAR(128) NOT NULL,
    deleted_at TIMESTAMP(6),
    issuer VARCHAR(2048) NOT NULL,
    realm_key VARCHAR(64) NOT NULL,
    realm_type VARCHAR(16) NOT NULL,
    business_tenant_id BIGINT,
    storage_partition_id BIGINT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'INITIALIZING',
    active_kid VARCHAR(128),
    trust_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_oauth_realm PRIMARY KEY (id),
    CONSTRAINT ck_oauth_realm_partition CHECK (tenant_id > 0 AND storage_partition_id > 0 AND tenant_id = storage_partition_id),
    CONSTRAINT ck_oauth_realm_version CHECK (version >= 0 AND trust_version >= 0),
    CONSTRAINT ck_oauth_realm_type CHECK (realm_type IN ('TENANT', 'PLATFORM')),
    CONSTRAINT ck_oauth_realm_status CHECK (status IN ('INITIALIZING', 'ACTIVE', 'SUSPENDED', 'CLOSED')),
    CONSTRAINT ck_oauth_realm_business_tenant CHECK ((realm_type = 'TENANT' AND business_tenant_id > 0) OR (realm_type = 'PLATFORM' AND business_tenant_id IS NULL)),
    CONSTRAINT uk_oauth_realm_issuer_permanent UNIQUE (tenant_id, issuer),
    CONSTRAINT uk_oauth_realm_partition_permanent UNIQUE (tenant_id, storage_partition_id)
);
CREATE UNIQUE INDEX uk_realm_1_life ON oauth_realm (tenant_id, issuer, deleted_at);
CREATE UNIQUE INDEX uk_realm_1_active ON oauth_realm (tenant_id, issuer) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_realm_2_life ON oauth_realm (tenant_id, realm_key, deleted_at);
CREATE UNIQUE INDEX uk_realm_2_active ON oauth_realm (tenant_id, realm_key) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_realm_3_life ON oauth_realm (tenant_id, storage_partition_id, deleted_at);
CREATE UNIQUE INDEX uk_realm_3_active ON oauth_realm (tenant_id, storage_partition_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_realm_access ON oauth_realm (status, realm_key) WHERE deleted_at IS NULL;

CREATE TABLE oauth_client (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    create_time TIMESTAMPTZ(6) NOT NULL,
    create_user_id VARCHAR(128) NOT NULL,
    update_time TIMESTAMPTZ(6) NOT NULL,
    update_user_id VARCHAR(128) NOT NULL,
    deleted_at TIMESTAMP(6),
    client_id VARCHAR(128) NOT NULL,
    app_id VARCHAR(128),
    client_name VARCHAR(200) NOT NULL,
    client_type VARCHAR(16) NOT NULL,
    purpose VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    secret_hash VARCHAR(512),
    secret_hint VARCHAR(8),
    credential_version BIGINT NOT NULL DEFAULT 0,
    redirect_uris JSONB NOT NULL DEFAULT '[]'::jsonb,
    resource_uris JSONB NOT NULL DEFAULT '[]'::jsonb,
    allowed_scopes JSONB NOT NULL DEFAULT '[]'::jsonb,
    access_ttl_seconds INTEGER NOT NULL DEFAULT 300,
    refresh_ttl_seconds INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT pk_oauth_client PRIMARY KEY (id),
    CONSTRAINT ck_oauth_client_partition CHECK (tenant_id > 0),
    CONSTRAINT ck_oauth_client_version CHECK (version >= 0 AND credential_version >= 0),
    CONSTRAINT ck_oauth_client_type CHECK (client_type IN ('PUBLIC', 'CONFIDENTIAL')),
    CONSTRAINT ck_oauth_client_purpose CHECK (purpose IN ('USER', 'SERVICE')),
    CONSTRAINT ck_oauth_client_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_oauth_client_redirects CHECK (jsonb_typeof(redirect_uris) = 'array'),
    CONSTRAINT ck_oauth_client_resources CHECK (jsonb_typeof(resource_uris) = 'array'),
    CONSTRAINT ck_oauth_client_scopes CHECK (jsonb_typeof(allowed_scopes) = 'array'),
    CONSTRAINT ck_oauth_client_access_ttl CHECK (access_ttl_seconds BETWEEN 300 AND 600),
    CONSTRAINT ck_oauth_client_refresh_ttl CHECK (refresh_ttl_seconds = 0 OR (client_type = 'CONFIDENTIAL' AND purpose = 'USER' AND refresh_ttl_seconds BETWEEN 3600 AND 604800)),
    CONSTRAINT uk_oauth_client_identity_permanent UNIQUE (tenant_id, client_id)
);
CREATE UNIQUE INDEX uk_client_1_life ON oauth_client (tenant_id, client_id, deleted_at);
CREATE UNIQUE INDEX uk_client_1_active ON oauth_client (tenant_id, client_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_client_access ON oauth_client (tenant_id, client_id) WHERE deleted_at IS NULL;

CREATE TABLE oauth_grant (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    create_time TIMESTAMPTZ(6) NOT NULL,
    create_user_id VARCHAR(128) NOT NULL,
    update_time TIMESTAMPTZ(6) NOT NULL,
    update_user_id VARCHAR(128) NOT NULL,
    deleted_at TIMESTAMP(6),
    client_pk BIGINT NOT NULL,
    resource_server_id VARCHAR(128) NOT NULL,
    grant_type VARCHAR(32) NOT NULL,
    allowed_scopes JSONB NOT NULL DEFAULT '[]'::jsonb,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    resource_version BIGINT NOT NULL,
    CONSTRAINT pk_oauth_grant PRIMARY KEY (id),
    CONSTRAINT ck_oauth_grant_partition CHECK (tenant_id > 0),
    CONSTRAINT ck_oauth_grant_version CHECK (version >= 0 AND resource_version >= 0),
    CONSTRAINT ck_oauth_grant_type CHECK (grant_type IN ('USER_DELEGATION', 'CLIENT_CREDENTIALS')),
    CONSTRAINT ck_oauth_grant_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_oauth_grant_scopes CHECK (jsonb_typeof(allowed_scopes) = 'array')
);
CREATE UNIQUE INDEX uk_grant_1_life ON oauth_grant (tenant_id, client_pk, resource_server_id, grant_type, deleted_at);
CREATE UNIQUE INDEX uk_grant_1_active ON oauth_grant (tenant_id, client_pk, resource_server_id, grant_type) WHERE deleted_at IS NULL;
CREATE INDEX ix_grant_access ON oauth_grant (tenant_id, client_pk, resource_server_id) WHERE deleted_at IS NULL;

CREATE TABLE oauth_authorization (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    create_time TIMESTAMPTZ(6) NOT NULL,
    create_user_id VARCHAR(128) NOT NULL,
    update_time TIMESTAMPTZ(6) NOT NULL,
    update_user_id VARCHAR(128) NOT NULL,
    deleted_at TIMESTAMP(6),
    client_pk BIGINT NOT NULL,
    principal_name VARCHAR(128) NOT NULL,
    principal_type VARCHAR(16) NOT NULL,
    grant_type VARCHAR(32) NOT NULL,
    authorized_scopes JSONB NOT NULL DEFAULT '[]'::jsonb,
    audiences JSONB NOT NULL DEFAULT '[]'::jsonb,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    auth_time TIMESTAMPTZ(6),
    acr VARCHAR(64),
    attributes_ciphertext TEXT NOT NULL,
    absolute_expires_at TIMESTAMPTZ(6) NOT NULL,
    revoked_at TIMESTAMPTZ(6),
    CONSTRAINT pk_oauth_authorization PRIMARY KEY (id),
    CONSTRAINT ck_oauth_authorization_partition CHECK (tenant_id > 0),
    CONSTRAINT ck_oauth_authorization_version CHECK (version >= 0),
    CONSTRAINT ck_oauth_authorization_principal CHECK (principal_type IN ('USER', 'SERVICE')),
    CONSTRAINT ck_oauth_authorization_grant CHECK (grant_type IN ('authorization_code', 'client_credentials')),
    CONSTRAINT ck_oauth_authorization_status CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED')),
    CONSTRAINT ck_oauth_authorization_scopes CHECK (jsonb_typeof(authorized_scopes) = 'array'),
    CONSTRAINT ck_oauth_authorization_audiences CHECK (jsonb_typeof(audiences) = 'array'),
    CONSTRAINT ck_oauth_authorization_expiry CHECK (absolute_expires_at > create_time)
);
CREATE INDEX ix_authorization_access ON oauth_authorization (tenant_id, principal_name, status, id) WHERE deleted_at IS NULL;
CREATE INDEX ix_authorization_retention ON oauth_authorization (tenant_id, absolute_expires_at, id);

CREATE TABLE oauth_token (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    create_time TIMESTAMPTZ(6) NOT NULL,
    create_user_id VARCHAR(128) NOT NULL,
    update_time TIMESTAMPTZ(6) NOT NULL,
    update_user_id VARCHAR(128) NOT NULL,
    deleted_at TIMESTAMP(6),
    authorization_pk BIGINT NOT NULL,
    token_type VARCHAR(16) NOT NULL,
    token_digest CHAR(64) NOT NULL,
    token_ciphertext TEXT,
    jti VARCHAR(64),
    issued_at TIMESTAMPTZ(6) NOT NULL,
    expires_at TIMESTAMPTZ(6) NOT NULL,
    consumed_at TIMESTAMPTZ(6),
    revoked_at TIMESTAMPTZ(6),
    metadata_ciphertext TEXT NOT NULL,
    CONSTRAINT pk_oauth_token PRIMARY KEY (id),
    CONSTRAINT ck_oauth_token_partition CHECK (tenant_id > 0),
    CONSTRAINT ck_oauth_token_version CHECK (version >= 0),
    CONSTRAINT ck_oauth_token_type CHECK (token_type IN ('ACCESS', 'REFRESH', 'ID_TOKEN', 'AUTHORIZATION_CODE')),
    CONSTRAINT ck_oauth_token_digest CHECK (token_digest ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_oauth_token_jti CHECK (token_type NOT IN ('ACCESS', 'ID_TOKEN') OR jti IS NOT NULL),
    CONSTRAINT ck_oauth_token_expiry CHECK (expires_at > issued_at),
    CONSTRAINT uk_oauth_token_digest_permanent UNIQUE (tenant_id, token_digest)
);
CREATE UNIQUE INDEX uk_token_1_life ON oauth_token (tenant_id, token_digest, deleted_at);
CREATE UNIQUE INDEX uk_token_1_active ON oauth_token (tenant_id, token_digest) WHERE deleted_at IS NULL;
CREATE INDEX ix_token_access ON oauth_token (tenant_id, authorization_pk, token_type) WHERE deleted_at IS NULL;
CREATE INDEX ix_token_retention ON oauth_token (tenant_id, expires_at, id);

CREATE TABLE oauth_consent (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    create_time TIMESTAMPTZ(6) NOT NULL,
    create_user_id VARCHAR(128) NOT NULL,
    update_time TIMESTAMPTZ(6) NOT NULL,
    update_user_id VARCHAR(128) NOT NULL,
    deleted_at TIMESTAMP(6),
    client_pk BIGINT NOT NULL,
    subject VARCHAR(128) NOT NULL,
    authorities JSONB NOT NULL DEFAULT '[]'::jsonb,
    granted_at TIMESTAMPTZ(6) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT pk_oauth_consent PRIMARY KEY (id),
    CONSTRAINT ck_oauth_consent_partition CHECK (tenant_id > 0),
    CONSTRAINT ck_oauth_consent_version CHECK (version >= 0),
    CONSTRAINT ck_oauth_consent_authorities CHECK (jsonb_typeof(authorities) = 'array'),
    CONSTRAINT ck_oauth_consent_status CHECK (status IN ('ACTIVE', 'REVOKED'))
);
CREATE UNIQUE INDEX uk_consent_1_life ON oauth_consent (tenant_id, client_pk, subject, deleted_at);
CREATE UNIQUE INDEX uk_consent_1_active ON oauth_consent (tenant_id, client_pk, subject) WHERE deleted_at IS NULL;
CREATE INDEX ix_consent_access ON oauth_consent (tenant_id, client_pk, subject) WHERE deleted_at IS NULL;

CREATE TABLE oauth_signing_key (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    create_time TIMESTAMPTZ(6) NOT NULL,
    create_user_id VARCHAR(128) NOT NULL,
    update_time TIMESTAMPTZ(6) NOT NULL,
    update_user_id VARCHAR(128) NOT NULL,
    deleted_at TIMESTAMP(6),
    kid VARCHAR(128) NOT NULL,
    algorithm VARCHAR(16) NOT NULL DEFAULT 'RS256',
    public_jwk JSONB NOT NULL,
    private_key_ciphertext TEXT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PUBLISHED',
    published_at TIMESTAMPTZ(6) NOT NULL,
    activated_at TIMESTAMPTZ(6),
    last_issued_at TIMESTAMPTZ(6),
    verify_until TIMESTAMPTZ(6),
    retired_at TIMESTAMPTZ(6),
    CONSTRAINT pk_oauth_signing_key PRIMARY KEY (id),
    CONSTRAINT ck_oauth_signing_key_partition CHECK (tenant_id > 0),
    CONSTRAINT ck_oauth_signing_key_version CHECK (version >= 0),
    CONSTRAINT ck_oauth_signing_key_algorithm CHECK (algorithm = 'RS256'),
    CONSTRAINT ck_oauth_signing_key_jwk CHECK (jsonb_typeof(public_jwk) = 'object'),
    CONSTRAINT ck_oauth_signing_key_status CHECK (status IN ('PUBLISHED', 'ACTIVE', 'VERIFY_ONLY', 'RETIRED', 'COMPROMISED')),
    CONSTRAINT uk_oauth_signing_key_kid_permanent UNIQUE (tenant_id, kid)
);
CREATE UNIQUE INDEX uk_signing_key_1_life ON oauth_signing_key (tenant_id, kid, deleted_at);
CREATE UNIQUE INDEX uk_signing_key_1_active ON oauth_signing_key (tenant_id, kid) WHERE deleted_at IS NULL;
CREATE INDEX ix_signing_key_access ON oauth_signing_key (tenant_id, status, kid) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_key_one_active ON oauth_signing_key (tenant_id) WHERE status = 'ACTIVE' AND deleted_at IS NULL;

CREATE TABLE oauth_security_state (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    create_time TIMESTAMPTZ(6) NOT NULL,
    create_user_id VARCHAR(128) NOT NULL,
    update_time TIMESTAMPTZ(6) NOT NULL,
    update_user_id VARCHAR(128) NOT NULL,
    deleted_at TIMESTAMP(6),
    target_type VARCHAR(16) NOT NULL,
    target_key VARCHAR(128) NOT NULL,
    generation BIGINT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    revision BIGINT NOT NULL DEFAULT 0,
    reason_code VARCHAR(128) NOT NULL DEFAULT 'INITIALIZED',
    CONSTRAINT pk_oauth_security_state PRIMARY KEY (id),
    CONSTRAINT ck_oauth_security_state_partition CHECK (tenant_id > 0),
    CONSTRAINT ck_oauth_security_state_version CHECK (version >= 0 AND generation >= 0 AND revision >= 0),
    CONSTRAINT ck_oauth_security_state_target CHECK (target_type IN ('REALM', 'CLIENT', 'SUBJECT'))
);
CREATE UNIQUE INDEX uk_security_state_1_life ON oauth_security_state (tenant_id, target_type, target_key, deleted_at);
CREATE UNIQUE INDEX uk_security_state_1_active ON oauth_security_state (tenant_id, target_type, target_key) WHERE deleted_at IS NULL;
CREATE INDEX ix_security_state_access ON oauth_security_state (tenant_id, revision) WHERE deleted_at IS NULL;

CREATE TABLE oauth_revocation (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    create_time TIMESTAMPTZ(6) NOT NULL,
    create_user_id VARCHAR(128) NOT NULL,
    update_time TIMESTAMPTZ(6) NOT NULL,
    update_user_id VARCHAR(128) NOT NULL,
    deleted_at TIMESTAMP(6),
    target_type VARCHAR(16) NOT NULL,
    target_key VARCHAR(128) NOT NULL,
    reason_code VARCHAR(128) NOT NULL,
    revoked_at TIMESTAMPTZ(6) NOT NULL,
    expires_at TIMESTAMPTZ(6),
    revision BIGINT NOT NULL,
    projection_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT pk_oauth_revocation PRIMARY KEY (id),
    CONSTRAINT ck_oauth_revocation_partition CHECK (tenant_id > 0),
    CONSTRAINT ck_oauth_revocation_version CHECK (version >= 0 AND revision >= 0),
    CONSTRAINT ck_oauth_revocation_target CHECK (target_type IN ('JTI', 'AUTHORIZATION', 'KID')),
    CONSTRAINT ck_oauth_revocation_projection CHECK (projection_status IN ('PENDING', 'APPLIED')),
    CONSTRAINT ck_oauth_revocation_expiry CHECK (expires_at IS NULL OR expires_at > revoked_at)
);
CREATE UNIQUE INDEX uk_revocation_1_life ON oauth_revocation (tenant_id, target_type, target_key, deleted_at);
CREATE UNIQUE INDEX uk_revocation_1_active ON oauth_revocation (tenant_id, target_type, target_key) WHERE deleted_at IS NULL;
CREATE INDEX ix_revocation_access ON oauth_revocation (tenant_id, revision) WHERE deleted_at IS NULL;
CREATE INDEX ix_revocation_retention ON oauth_revocation (tenant_id, expires_at, id);

CREATE TABLE oauth_audit (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    create_time TIMESTAMPTZ(6) NOT NULL,
    create_user_id VARCHAR(128) NOT NULL,
    update_time TIMESTAMPTZ(6) NOT NULL,
    update_user_id VARCHAR(128) NOT NULL,
    deleted_at TIMESTAMP(6),
    event_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    subject_ref VARCHAR(128),
    client_ref VARCHAR(128),
    token_ref CHAR(64),
    resource_ref VARCHAR(2048),
    result VARCHAR(16) NOT NULL,
    reason_code VARCHAR(128) NOT NULL,
    trace_id VARCHAR(128),
    occurred_at TIMESTAMPTZ(6) NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    audit_kind VARCHAR(16) NOT NULL DEFAULT 'OAUTH',
    public_id VARCHAR(64) NOT NULL,
    actor_sub VARCHAR(64),
    target_sub VARCHAR(64),
    identity_tenant_id VARCHAR(64),
    CONSTRAINT pk_oauth_audit PRIMARY KEY (id),
    CONSTRAINT ck_oauth_audit_partition CHECK (tenant_id > 0),
    CONSTRAINT ck_oauth_audit_version CHECK (version >= 0),
    CONSTRAINT ck_oauth_audit_result CHECK (result IN ('SUCCESS', 'DENIED', 'FAILED', 'FAILURE')),
    CONSTRAINT ck_oauth_audit_kind CHECK (audit_kind IN ('OAUTH', 'IDENTITY')),
    CONSTRAINT ck_oauth_audit_payload CHECK (jsonb_typeof(payload) = 'object' AND octet_length(payload::text) <= 16384),
    CONSTRAINT ck_oauth_audit_token_ref CHECK (token_ref IS NULL OR token_ref ~ '^[0-9a-f]{64}$'),
    CONSTRAINT uk_oauth_audit_event_permanent UNIQUE (tenant_id, event_id)
);
CREATE UNIQUE INDEX uk_audit_1_life ON oauth_audit (tenant_id, event_id, deleted_at);
CREATE UNIQUE INDEX uk_audit_1_active ON oauth_audit (tenant_id, event_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_audit_public_id_life ON oauth_audit (tenant_id, public_id, deleted_at);
CREATE UNIQUE INDEX uk_audit_public_id_active ON oauth_audit (tenant_id, public_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_audit_access ON oauth_audit (tenant_id, occurred_at DESC, id DESC) WHERE deleted_at IS NULL;
CREATE INDEX ix_audit_retention ON oauth_audit (tenant_id, occurred_at, id);
CREATE INDEX ix_audit_identity_page ON oauth_audit (tenant_id, occurred_at DESC, public_id DESC) WHERE audit_kind = 'IDENTITY' AND deleted_at IS NULL;
CREATE INDEX ix_audit_identity_actor ON oauth_audit (tenant_id, actor_sub, occurred_at DESC, public_id DESC) WHERE audit_kind = 'IDENTITY' AND deleted_at IS NULL;

CREATE TABLE oauth_resource (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    create_time TIMESTAMPTZ(6) NOT NULL,
    create_user_id VARCHAR(128) NOT NULL,
    update_time TIMESTAMPTZ(6) NOT NULL,
    update_user_id VARCHAR(128) NOT NULL,
    deleted_at TIMESTAMP(6),
    resource_server_id VARCHAR(128) NOT NULL,
    resource_uri VARCHAR(2048) NOT NULL,
    biz_code VARCHAR(128) NOT NULL,
    app_code VARCHAR(128) NOT NULL,
    environment VARCHAR(128) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    management_client_id VARCHAR(128) NOT NULL,
    management_client_pk BIGINT NOT NULL,
    rbac_application_code VARCHAR(128) NOT NULL,
    entry_permission_code VARCHAR(256) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    admission_ticket_ttl_seconds INTEGER NOT NULL DEFAULT 300,
    CONSTRAINT pk_oauth_resource PRIMARY KEY (id),
    CONSTRAINT ck_oauth_resource_partition CHECK (tenant_id > 0),
    CONSTRAINT ck_oauth_resource_version CHECK (version >= 0),
    CONSTRAINT ck_oauth_resource_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_oauth_resource_ticket_ttl CHECK (admission_ticket_ttl_seconds BETWEEN 30 AND 900),
    CONSTRAINT uk_oauth_resource_server_permanent UNIQUE (tenant_id, resource_server_id),
    CONSTRAINT uk_oauth_resource_uri_permanent UNIQUE (tenant_id, resource_uri),
    CONSTRAINT uk_oauth_resource_app_permanent UNIQUE (tenant_id, biz_code, app_code, environment),
    CONSTRAINT uk_oauth_resource_manager_permanent UNIQUE (tenant_id, management_client_pk)
);
CREATE UNIQUE INDEX uk_resource_1_life ON oauth_resource (tenant_id, resource_server_id, deleted_at);
CREATE UNIQUE INDEX uk_resource_1_active ON oauth_resource (tenant_id, resource_server_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_resource_2_life ON oauth_resource (tenant_id, resource_uri, deleted_at);
CREATE UNIQUE INDEX uk_resource_2_active ON oauth_resource (tenant_id, resource_uri) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_resource_3_life ON oauth_resource (tenant_id, biz_code, app_code, environment, deleted_at);
CREATE UNIQUE INDEX uk_resource_3_active ON oauth_resource (tenant_id, biz_code, app_code, environment) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_resource_4_life ON oauth_resource (tenant_id, management_client_pk, deleted_at);
CREATE UNIQUE INDEX uk_resource_4_active ON oauth_resource (tenant_id, management_client_pk) WHERE deleted_at IS NULL;
