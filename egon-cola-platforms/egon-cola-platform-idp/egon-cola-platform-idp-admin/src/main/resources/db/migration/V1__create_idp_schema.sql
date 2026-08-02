CREATE TABLE identity_user (
    id                  VARCHAR(64) PRIMARY KEY,
    username            VARCHAR(128) NOT NULL,
    username_normalized VARCHAR(128) NOT NULL,
    display_name        VARCHAR(200) NOT NULL,
    status              VARCHAR(32) NOT NULL,
    token_version       BIGINT NOT NULL DEFAULT 0,
    failed_login_count  INTEGER NOT NULL DEFAULT 0,
    locked_until        TIMESTAMPTZ,
    last_login_at       TIMESTAMPTZ,
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    UNIQUE (username_normalized),
    CHECK (token_version >= 0),
    CHECK (failed_login_count >= 0),
    CHECK (version >= 0),
    CHECK (status IN ('ACTIVE', 'DISABLED', 'LOCKED'))
);

CREATE TABLE identity_user_credential (
    id                   VARCHAR(64) PRIMARY KEY,
    identity_sub         VARCHAR(64) NOT NULL REFERENCES identity_user(id),
    credential_type      VARCHAR(32) NOT NULL,
    password_hash        VARCHAR(512) NOT NULL,
    password_changed_at  TIMESTAMPTZ NOT NULL,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    status               VARCHAR(32) NOT NULL,
    version              BIGINT NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL,
    UNIQUE (identity_sub, credential_type),
    CHECK (credential_type IN ('PASSWORD')),
    CHECK (status IN ('ACTIVE', 'REVOKED')),
    CHECK (version >= 0)
);

CREATE TABLE identity_client (
    client_id                 VARCHAR(128) PRIMARY KEY,
    client_name               VARCHAR(200) NOT NULL,
    client_type               VARCHAR(32) NOT NULL,
    status                    VARCHAR(32) NOT NULL,
    pkce_required             BOOLEAN NOT NULL DEFAULT TRUE,
    access_token_ttl_seconds  INTEGER NOT NULL DEFAULT 900,
    refresh_token_ttl_seconds INTEGER NOT NULL DEFAULT 604800,
    version                   BIGINT NOT NULL DEFAULT 0,
    created_at                TIMESTAMPTZ NOT NULL,
    updated_at                TIMESTAMPTZ NOT NULL,
    CHECK (client_type IN ('PUBLIC', 'CONFIDENTIAL')),
    CHECK (status IN ('ACTIVE', 'DISABLED')),
    CHECK (access_token_ttl_seconds BETWEEN 300 AND 1800),
    CHECK (refresh_token_ttl_seconds BETWEEN 86400 AND 2592000),
    CHECK (version >= 0)
);

CREATE TABLE identity_client_redirect_uri (
    id           VARCHAR(64) PRIMARY KEY,
    client_id    VARCHAR(128) NOT NULL REFERENCES identity_client(client_id),
    redirect_uri VARCHAR(2048) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    UNIQUE (client_id, redirect_uri)
);

CREATE TABLE identity_client_audience (
    id         VARCHAR(64) PRIMARY KEY,
    client_id  VARCHAR(128) NOT NULL REFERENCES identity_client(client_id),
    audience   VARCHAR(256) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (client_id, audience)
);

CREATE TABLE identity_signing_key (
    kid                   VARCHAR(128) PRIMARY KEY,
    algorithm             VARCHAR(32) NOT NULL,
    encrypted_private_key TEXT NOT NULL,
    public_jwk            JSONB NOT NULL,
    status                VARCHAR(32) NOT NULL,
    activated_at          TIMESTAMPTZ,
    retired_at            TIMESTAMPTZ,
    version               BIGINT NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ NOT NULL,
    updated_at            TIMESTAMPTZ NOT NULL,
    CHECK (algorithm IN ('RS256')),
    CHECK (status IN ('PUBLISHED', 'ACTIVE', 'RETIRED')),
    CHECK (version >= 0)
);

CREATE TABLE identity_audit_log (
    id          VARCHAR(64) PRIMARY KEY,
    event_type  VARCHAR(128) NOT NULL,
    actor_sub   VARCHAR(64),
    target_sub  VARCHAR(64),
    tenant_id   VARCHAR(64),
    session_id  VARCHAR(64),
    client_id   VARCHAR(128),
    source_ip   VARCHAR(64),
    user_agent  VARCHAR(1024),
    result      VARCHAR(32) NOT NULL,
    reason      VARCHAR(128) NOT NULL,
    trace_id    VARCHAR(128),
    payload     JSONB NOT NULL DEFAULT '{}'::JSONB,
    occurred_at TIMESTAMPTZ NOT NULL,
    CHECK (result IN ('SUCCESS', 'FAILURE'))
);

CREATE TABLE identity_outbox_event (
    id              VARCHAR(64) PRIMARY KEY,
    aggregate_type  VARCHAR(128) NOT NULL,
    aggregate_id    VARCHAR(128) NOT NULL,
    event_type      VARCHAR(128) NOT NULL,
    payload         JSONB NOT NULL,
    status          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    attempt_count   INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    published_at    TIMESTAMPTZ,
    CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    CHECK (attempt_count >= 0)
);

CREATE INDEX idx_identity_user_status
    ON identity_user(status);
CREATE INDEX idx_identity_credential_subject
    ON identity_user_credential(identity_sub, status);
CREATE INDEX idx_identity_audit_actor_time
    ON identity_audit_log(actor_sub, occurred_at DESC);
CREATE INDEX idx_identity_audit_target_time
    ON identity_audit_log(target_sub, occurred_at DESC);
CREATE INDEX idx_identity_outbox_delivery
    ON identity_outbox_event(status, next_attempt_at, created_at);
