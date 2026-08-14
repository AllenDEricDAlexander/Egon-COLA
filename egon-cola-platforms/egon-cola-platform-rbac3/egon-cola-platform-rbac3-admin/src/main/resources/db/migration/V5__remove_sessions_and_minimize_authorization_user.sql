/*
 * Destructive RBAC3 identity boundary.
 *
 * V1-V4 are intentionally left unchanged. This migration is allowed to run
 * only against a clean RBAC identity/session schema; the IdP is the owner of
 * user credentials and refresh tokens after this version.
 */
DO
$$
    DECLARE
        row_count BIGINT;
    BEGIN
        SELECT count(*) INTO row_count FROM rbac3_user;
        IF row_count > 0 THEN
            RAISE EXCEPTION
                'RBAC3 V5 requires an empty rbac3_user table; found % rows', row_count;
        END IF;

        SELECT count(*) INTO row_count FROM rbac3_user_credential;
        IF row_count > 0 THEN
            RAISE EXCEPTION
                'RBAC3 V5 refuses to destroy rbac3_user_credential rows: %', row_count;
        END IF;

        SELECT count(*) INTO row_count FROM rbac3_external_identity;
        IF row_count > 0 THEN
            RAISE EXCEPTION
                'RBAC3 V5 refuses to destroy rbac3_external_identity rows: %', row_count;
        END IF;

        SELECT count(*) INTO row_count FROM rbac3_service_credential;
        IF row_count > 0 THEN
            RAISE EXCEPTION
                'RBAC3 V5 refuses to destroy rbac3_service_credential rows: %', row_count;
        END IF;

        SELECT count(*) INTO row_count FROM rbac3_session;
        IF row_count > 0 THEN
            RAISE EXCEPTION
                'RBAC3 V5 refuses to destroy rbac3_session rows: %', row_count;
        END IF;

        SELECT count(*) INTO row_count FROM rbac3_session_active_role;
        IF row_count > 0 THEN
            RAISE EXCEPTION
                'RBAC3 V5 refuses to destroy rbac3_session_active_role rows: %', row_count;
        END IF;

        SELECT count(*) INTO row_count FROM rbac3_refresh_token;
        IF row_count > 0 THEN
            RAISE EXCEPTION
                'RBAC3 V5 refuses to destroy rbac3_refresh_token rows: %', row_count;
        END IF;

        SELECT count(*) INTO row_count FROM rbac3_authorization_mutation;
        IF row_count > 0 THEN
            RAISE EXCEPTION
                'RBAC3 V5 requires an empty rbac3_authorization_mutation table; found % rows',
                row_count;
        END IF;
    END
$$;

/* Detach the mutation record from personnel sessions before dropping them. */
ALTER TABLE rbac3_authorization_mutation
    DROP CONSTRAINT fk_rbac3_authorization_mutation_session,
    DROP CONSTRAINT ck_rbac3_authorization_mutation_scope,
    DROP CONSTRAINT ck_rbac3_authorization_mutation_scope_target,
    DROP CONSTRAINT ck_rbac3_authorization_mutation_versions;

DROP INDEX idx_rbac3_authorization_mutation_scope;

ALTER TABLE rbac3_authorization_mutation
    DROP COLUMN session_id,
    DROP COLUMN old_session_version,
    DROP COLUMN new_session_version,
    RENAME COLUMN fence_created_at TO guard_created_at;

ALTER TABLE rbac3_authorization_mutation
    ADD CONSTRAINT ck_rbac3_authorization_mutation_scope
        CHECK (scope_type IN ('USER', 'TENANT')),
    ADD CONSTRAINT ck_rbac3_authorization_mutation_scope_target
        CHECK (
            (scope_type = 'USER' AND user_id IS NOT NULL)
                OR (scope_type = 'TENANT' AND user_id IS NULL)
            ),
    ADD CONSTRAINT ck_rbac3_authorization_mutation_versions CHECK (
        (old_auth_version IS NULL OR old_auth_version >= 0)
            AND (new_auth_version IS NULL OR new_auth_version >= 0)
            AND (old_policy_version IS NULL OR old_policy_version >= 0)
            AND (new_policy_version IS NULL OR new_policy_version >= 0)
            AND version >= 0
        );

CREATE INDEX idx_rbac3_authorization_mutation_scope
    ON rbac3_authorization_mutation (tenant_id, user_id, status);

/* Refresh tokens and session-bound active roles are IdP-owned state. */
ALTER TABLE rbac3_refresh_token
    DROP CONSTRAINT fk_rbac3_refresh_token_replacement,
    DROP CONSTRAINT fk_rbac3_refresh_token_session;
DROP TABLE rbac3_refresh_token;

DROP TABLE rbac3_session_active_role;
DROP TABLE rbac3_session;

/* RBAC never stores either user or service credential material. */
DROP TABLE rbac3_user_credential;
DROP TABLE rbac3_service_credential;
DROP TABLE rbac3_external_identity;

/* Replace the legacy local profile projection with a direct IdP subject link. */
DROP INDEX idx_rbac3_user_primary_org;

ALTER TABLE rbac3_user
    DROP CONSTRAINT uq_rbac3_user_username,
    DROP CONSTRAINT fk_rbac3_user_primary_org,
    DROP CONSTRAINT fk_rbac3_user_primary_position,
    DROP CONSTRAINT ck_rbac3_user_directory_version,
    DROP COLUMN username,
    DROP COLUMN normalized_username,
    DROP COLUMN display_name,
    DROP COLUMN primary_org_unit_id,
    DROP COLUMN primary_position_id,
    DROP COLUMN directory_snapshot_version,
    DROP COLUMN locked_until,
    DROP COLUMN archived_at,
    ADD COLUMN identity_sub VARCHAR(200) NOT NULL;

ALTER TABLE rbac3_user
    ADD CONSTRAINT uq_rbac3_user_identity_sub
        UNIQUE (tenant_id, identity_sub),
    ADD CONSTRAINT ck_rbac3_user_identity_sub
        CHECK (length(btrim(identity_sub)) > 0);

CREATE INDEX idx_rbac3_user_identity_lookup
    ON rbac3_user (identity_sub, tenant_id, status);

/* Durable user active-role projection used by authorization snapshots. */
CREATE TABLE rbac3_user_active_role
(
    tenant_id               BIGINT      NOT NULL,
    user_id                 BIGINT      NOT NULL,
    application_id          BIGINT      NOT NULL,
    root_role_id            BIGINT      NOT NULL,
    eligible_assignment_ids JSONB       NOT NULL DEFAULT '[]'::jsonb,
    activated_at            TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_rbac3_user_active_role_root
        UNIQUE (tenant_id, user_id, root_role_id),
    CONSTRAINT fk_rbac3_user_active_role_user
        FOREIGN KEY (tenant_id, user_id)
            REFERENCES rbac3_user (tenant_id, id),
    CONSTRAINT fk_rbac3_user_active_role_root
        FOREIGN KEY (tenant_id, application_id, root_role_id)
            REFERENCES rbac3_role (tenant_id, application_id, id),
    CONSTRAINT ck_rbac3_user_active_role_evidence
        CHECK (jsonb_typeof(eligible_assignment_ids) = 'array')
);

CREATE INDEX idx_rbac3_user_active_role_application
    ON rbac3_user_active_role (tenant_id, user_id, application_id);
CREATE INDEX idx_rbac3_user_active_role_root
    ON rbac3_user_active_role (tenant_id, root_role_id);
