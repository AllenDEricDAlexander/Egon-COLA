DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM rbac3_application)
       OR EXISTS (SELECT 1 FROM rbac3_service_principal)
       OR EXISTS (SELECT 1 FROM rbac3_service_permission)
       OR EXISTS (SELECT 1 FROM rbac3_operation_sod_rule)
       OR EXISTS (SELECT 1 FROM rbac3_business_participation) THEN
        RAISE EXCEPTION 'RBAC3 V6 requires an empty legacy application authorization graph';
    END IF;
END $$;

ALTER TABLE rbac3_service_permission
    DROP CONSTRAINT fk_rbac3_service_permission_application,
    DROP CONSTRAINT fk_rbac3_service_permission_principal,
    DROP CONSTRAINT uq_rbac3_service_permission_fact,
    DROP COLUMN application_code;

ALTER TABLE rbac3_service_principal
    DROP CONSTRAINT fk_rbac3_service_principal_application,
    DROP CONSTRAINT uq_rbac3_service_principal_application_id;

DROP INDEX idx_rbac3_service_principal_application;
DROP INDEX idx_rbac3_operation_sod_lookup;
DROP INDEX idx_rbac3_participation_conflict;

ALTER TABLE rbac3_service_principal
    ADD COLUMN application_id BIGINT NOT NULL,
    ADD CONSTRAINT uq_rbac3_service_principal_application_id
        UNIQUE (tenant_id, application_id, id);

ALTER TABLE rbac3_operation_sod_rule
    DROP CONSTRAINT fk_rbac3_operation_sod_application,
    DROP CONSTRAINT uq_rbac3_operation_sod_fact,
    ADD COLUMN application_id BIGINT NOT NULL,
    DROP COLUMN application_code;

ALTER TABLE rbac3_business_participation
    DROP CONSTRAINT fk_rbac3_business_participation_application,
    DROP CONSTRAINT uq_rbac3_business_participation_event,
    ADD COLUMN application_id BIGINT NOT NULL,
    DROP COLUMN application_code;

ALTER TABLE rbac3_application
    ADD COLUMN ddc_application_id VARCHAR(64) NOT NULL,
    ADD COLUMN ddc_business_id VARCHAR(64) NOT NULL;

ALTER TABLE rbac3_application
    DROP CONSTRAINT uq_rbac3_application_identity,
    DROP CONSTRAINT uq_rbac3_application_code,
    ADD CONSTRAINT uq_rbac3_application_tenant_ddc_application
        UNIQUE (tenant_id, ddc_application_id);

ALTER TABLE rbac3_service_principal
    ADD CONSTRAINT fk_rbac3_service_principal_application
        FOREIGN KEY (tenant_id, application_id)
        REFERENCES rbac3_application (tenant_id, id);

ALTER TABLE rbac3_service_permission
    ADD CONSTRAINT uq_rbac3_service_permission_fact
        UNIQUE (tenant_id, principal_id, permission_id, application_id),
    ADD CONSTRAINT fk_rbac3_service_permission_principal
        FOREIGN KEY (tenant_id, application_id, principal_id)
        REFERENCES rbac3_service_principal (tenant_id, application_id, id);

ALTER TABLE rbac3_operation_sod_rule
    ADD CONSTRAINT uq_rbac3_operation_sod_fact UNIQUE (
        tenant_id, application_id, business_resource,
        prior_action_code, forbidden_later_action_code, valid_from),
    ADD CONSTRAINT fk_rbac3_operation_sod_application
        FOREIGN KEY (tenant_id, application_id)
        REFERENCES rbac3_application (tenant_id, id);

ALTER TABLE rbac3_business_participation
    ADD CONSTRAINT uq_rbac3_business_participation_event
        UNIQUE (tenant_id, application_id, business_event_id),
    ADD CONSTRAINT fk_rbac3_business_participation_application
        FOREIGN KEY (tenant_id, application_id)
        REFERENCES rbac3_application (tenant_id, id);

CREATE INDEX idx_rbac3_service_principal_application
    ON rbac3_service_principal (tenant_id, application_id, status);

CREATE INDEX idx_rbac3_operation_sod_lookup
    ON rbac3_operation_sod_rule
        (tenant_id, application_id, business_resource, status);

CREATE INDEX idx_rbac3_participation_conflict
    ON rbac3_business_participation (
        tenant_id, application_id, business_resource, business_id,
        actor_user_id, action_code
    );

ALTER TABLE rbac3_org_unit
    ALTER COLUMN snapshot_id DROP NOT NULL,
    ADD COLUMN source_type VARCHAR(32);

UPDATE rbac3_org_unit
   SET source_type = 'DIRECTORY_SNAPSHOT';

ALTER TABLE rbac3_org_unit
    ALTER COLUMN source_type SET NOT NULL,
    ADD CONSTRAINT ck_rbac3_org_unit_source
        CHECK (
            (source_type = 'DIRECTORY_SNAPSHOT' AND snapshot_id IS NOT NULL)
            OR (source_type = 'MANUAL' AND snapshot_id IS NULL)
        );

ALTER TABLE rbac3_position
    ALTER COLUMN snapshot_id DROP NOT NULL,
    ADD COLUMN source_type VARCHAR(32);

UPDATE rbac3_position
   SET source_type = 'DIRECTORY_SNAPSHOT';

ALTER TABLE rbac3_position
    ALTER COLUMN source_type SET NOT NULL,
    ADD CONSTRAINT ck_rbac3_position_source
        CHECK (
            (source_type = 'DIRECTORY_SNAPSHOT' AND snapshot_id IS NOT NULL)
            OR (source_type = 'MANUAL' AND snapshot_id IS NULL)
        );

CREATE TABLE rbac3_user_business_access (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    ddc_business_id VARCHAR(64) NOT NULL,
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
    CONSTRAINT uq_rbac3_user_business_access_tenant_id
        UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_user_business_access_source
        UNIQUE (tenant_id, user_id, ddc_business_id, source_type, source_id),
    CONSTRAINT fk_rbac3_user_business_access_user
        FOREIGN KEY (tenant_id, user_id)
        REFERENCES rbac3_user (tenant_id, id),
    CONSTRAINT ck_rbac3_user_business_access_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED', 'EXPIRED')),
    CONSTRAINT ck_rbac3_user_business_access_window
        CHECK (valid_to IS NULL OR valid_to > valid_from),
    CONSTRAINT ck_rbac3_user_business_access_version
        CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_user_business_access_user_window
    ON rbac3_user_business_access
        (tenant_id, user_id, status, valid_from, valid_to);
CREATE INDEX idx_rbac3_user_business_access_business
    ON rbac3_user_business_access
        (tenant_id, ddc_business_id, status);

CREATE TABLE rbac3_user_org_assignment (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    org_unit_id BIGINT NOT NULL,
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
    CONSTRAINT uq_rbac3_user_org_assignment_tenant_id
        UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_user_org_assignment_source
        UNIQUE (tenant_id, user_id, org_unit_id, source_type, source_id),
    CONSTRAINT fk_rbac3_user_org_assignment_user
        FOREIGN KEY (tenant_id, user_id)
        REFERENCES rbac3_user (tenant_id, id),
    CONSTRAINT fk_rbac3_user_org_assignment_org
        FOREIGN KEY (tenant_id, org_unit_id)
        REFERENCES rbac3_org_unit (tenant_id, id),
    CONSTRAINT ck_rbac3_user_org_assignment_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED', 'EXPIRED')),
    CONSTRAINT ck_rbac3_user_org_assignment_window
        CHECK (valid_to IS NULL OR valid_to > valid_from),
    CONSTRAINT ck_rbac3_user_org_assignment_version
        CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_user_org_assignment_user_window
    ON rbac3_user_org_assignment
        (tenant_id, user_id, status, valid_from, valid_to);
CREATE INDEX idx_rbac3_user_org_assignment_org_window
    ON rbac3_user_org_assignment
        (tenant_id, org_unit_id, status, valid_from, valid_to);

CREATE TABLE rbac3_user_position_assignment (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    org_unit_id BIGINT NOT NULL,
    position_id BIGINT NOT NULL,
    primary_assignment BOOLEAN NOT NULL DEFAULT FALSE,
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
    CONSTRAINT uq_rbac3_user_position_assignment_tenant_id
        UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_user_position_assignment_source
        UNIQUE (tenant_id, user_id, org_unit_id, position_id, source_type, source_id),
    CONSTRAINT fk_rbac3_user_position_assignment_user
        FOREIGN KEY (tenant_id, user_id)
        REFERENCES rbac3_user (tenant_id, id),
    CONSTRAINT fk_rbac3_user_position_assignment_org
        FOREIGN KEY (tenant_id, org_unit_id)
        REFERENCES rbac3_org_unit (tenant_id, id),
    CONSTRAINT fk_rbac3_user_position_assignment_position
        FOREIGN KEY (tenant_id, position_id)
        REFERENCES rbac3_position (tenant_id, id),
    CONSTRAINT ck_rbac3_user_position_assignment_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED', 'EXPIRED')),
    CONSTRAINT ck_rbac3_user_position_assignment_window
        CHECK (valid_to IS NULL OR valid_to > valid_from),
    CONSTRAINT ck_rbac3_user_position_assignment_version
        CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_user_position_assignment_user_window
    ON rbac3_user_position_assignment
        (tenant_id, user_id, status, valid_from, valid_to);
CREATE INDEX idx_rbac3_user_position_assignment_org_window
    ON rbac3_user_position_assignment
        (tenant_id, org_unit_id, status, valid_from, valid_to);
CREATE INDEX idx_rbac3_user_position_assignment_position_window
    ON rbac3_user_position_assignment
        (tenant_id, position_id, status, valid_from, valid_to);

DROP TRIGGER IF EXISTS trg_rbac3_business_participation_append_only
    ON rbac3_business_participation;

CREATE TRIGGER trg_rbac3_business_participation_append_only
    BEFORE UPDATE OR DELETE ON rbac3_business_participation
    FOR EACH ROW EXECUTE FUNCTION rbac3_reject_append_only_change();
