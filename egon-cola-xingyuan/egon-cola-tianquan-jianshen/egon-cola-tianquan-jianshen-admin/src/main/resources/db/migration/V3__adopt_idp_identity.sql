ALTER TABLE rbac3_external_identity
    ADD COLUMN identity_sub VARCHAR(512);

UPDATE rbac3_external_identity
   SET identity_sub = external_subject_id
 WHERE upper(provider_code) = 'IDP';

UPDATE rbac3_external_identity
   SET identity_sub = 'legacy:' || lower(provider_code) || ':' || external_subject_id,
       status = 'DISABLED'
 WHERE upper(provider_code) <> 'IDP';

ALTER TABLE rbac3_external_identity
    ALTER COLUMN identity_sub SET NOT NULL;

ALTER TABLE rbac3_external_identity
    ADD CONSTRAINT uq_rbac3_external_identity_global_sub
        UNIQUE (tenant_id, identity_sub);

CREATE INDEX idx_rbac3_external_identity_global_lookup
    ON rbac3_external_identity (identity_sub, tenant_id, status);

DELETE FROM rbac3_refresh_token;
DELETE FROM rbac3_session_active_role;
DELETE FROM rbac3_authorization_mutation WHERE session_id IS NOT NULL;
DELETE FROM rbac3_session;

ALTER TABLE rbac3_session
    ADD COLUMN identity_sub VARCHAR(128) NOT NULL,
    ADD COLUMN context_version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN context_expires_at TIMESTAMPTZ NOT NULL;

ALTER TABLE rbac3_session
    ALTER COLUMN token_family_id DROP NOT NULL;

ALTER TABLE rbac3_session
    ADD CONSTRAINT fk_rbac3_session_identity_mapping
        FOREIGN KEY (tenant_id, identity_sub)
        REFERENCES rbac3_external_identity(tenant_id, identity_sub),
    ADD CONSTRAINT ck_rbac3_session_context_version
        CHECK (context_version >= 0),
    ADD CONSTRAINT ck_rbac3_session_context_expiry
        CHECK (context_expires_at > authenticated_at);

CREATE INDEX idx_rbac3_session_authorization_context
    ON rbac3_session (tenant_id, session_id, identity_sub, status);
