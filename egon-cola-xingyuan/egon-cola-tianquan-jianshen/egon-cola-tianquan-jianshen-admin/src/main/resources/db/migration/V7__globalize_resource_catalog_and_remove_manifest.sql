/*
 * Destructive RBAC3 catalog boundary.
 *
 * V7 is intentionally not backward compatible with the V1-V6 catalog shape.
 * The catalog is a service-global fact; tenant purchase/enablement is represented
 * only by rbac3_tenant_application. Existing authorization/catalog rows are not
 * copied between tenants. The deployment runbook must take the approved backup
 * before applying this migration.
 */

DO $$
DECLARE
    constraint_row RECORD;
BEGIN
    /* Remove every FK touching the old tenant-scoped catalog/Manifest graph. */
    FOR constraint_row IN
        SELECT child_ns.nspname AS child_schema,
               child.relname AS child_table,
               constraint_def.conname AS constraint_name
          FROM pg_constraint constraint_def
          JOIN pg_class child
            ON child.oid = constraint_def.conrelid
          JOIN pg_namespace child_ns
            ON child_ns.oid = child.relnamespace
          JOIN pg_class parent
            ON parent.oid = constraint_def.confrelid
         WHERE constraint_def.contype = 'f'
           AND (child.relname IN (
                    'rbac3_application', 'rbac3_resource_manifest',
                    'rbac3_resource', 'rbac3_permission',
                    'rbac3_permission_resource', 'rbac3_field_definition'
                )
                OR parent.relname IN (
                    'rbac3_application', 'rbac3_resource_manifest',
                    'rbac3_resource', 'rbac3_permission',
                    'rbac3_permission_resource', 'rbac3_field_definition'
                ))
    LOOP
        EXECUTE format(
                'ALTER TABLE %I.%I DROP CONSTRAINT IF EXISTS %I',
                constraint_row.child_schema,
                constraint_row.child_table,
                constraint_row.constraint_name);
    END LOOP;
END
$$;

/* The old graph is intentionally discarded; V7 never copies a tenant catalog. */
TRUNCATE TABLE
    rbac3_user_active_role,
    rbac3_role_permission,
    rbac3_field_rule,
    rbac3_data_rule_ref,
    rbac3_data_rule,
    rbac3_role_inheritance,
    rbac3_role_closure,
    rbac3_user_role_assignment,
    rbac3_auto_assignment_rule,
    rbac3_role_prerequisite,
    rbac3_role_cardinality,
    rbac3_sod_member,
    rbac3_sod_set,
    rbac3_operation_sod_rule,
    rbac3_business_participation,
    rbac3_service_permission,
    rbac3_service_principal,
    rbac3_permission_resource,
    rbac3_field_definition,
    rbac3_resource,
    rbac3_permission,
    rbac3_role,
    rbac3_application
    RESTART IDENTITY CASCADE;

DROP TRIGGER IF EXISTS trg_rbac3_resource_identity_immutable ON rbac3_resource;

ALTER TABLE rbac3_application
    DROP COLUMN IF EXISTS tenant_id CASCADE;
ALTER TABLE rbac3_application
    DROP COLUMN IF EXISTS current_manifest_id CASCADE;
ALTER TABLE rbac3_application
    DROP COLUMN IF EXISTS current_manifest_version CASCADE;

ALTER TABLE rbac3_permission
    DROP COLUMN IF EXISTS tenant_id CASCADE;
ALTER TABLE rbac3_resource
    DROP COLUMN IF EXISTS tenant_id CASCADE;
ALTER TABLE rbac3_resource
    DROP COLUMN IF EXISTS source_manifest_id CASCADE;
ALTER TABLE rbac3_permission_resource
    DROP COLUMN IF EXISTS tenant_id CASCADE;
ALTER TABLE rbac3_field_definition
    DROP COLUMN IF EXISTS tenant_id CASCADE;
ALTER TABLE rbac3_field_definition
    DROP COLUMN IF EXISTS source_manifest_id CASCADE;

ALTER TABLE rbac3_application
    ADD COLUMN IF NOT EXISTS ci_report_build_id VARCHAR(256),
    ADD COLUMN IF NOT EXISTS ci_report_checksum VARCHAR(128),
    ADD COLUMN IF NOT EXISTS ci_reported_at TIMESTAMPTZ;

ALTER TABLE rbac3_permission
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN IF NOT EXISTS source_build_id VARCHAR(256),
    ADD COLUMN IF NOT EXISTS source_checksum VARCHAR(128),
    ADD COLUMN IF NOT EXISTS ci_reported_at TIMESTAMPTZ;

ALTER TABLE rbac3_resource
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN IF NOT EXISTS source_checksum VARCHAR(128),
    ADD COLUMN IF NOT EXISTS ci_reported_at TIMESTAMPTZ;

ALTER TABLE rbac3_permission_resource
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN IF NOT EXISTS source_build_id VARCHAR(256),
    ADD COLUMN IF NOT EXISTS source_checksum VARCHAR(128),
    ADD COLUMN IF NOT EXISTS ci_reported_at TIMESTAMPTZ;

ALTER TABLE rbac3_field_definition
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN IF NOT EXISTS source_build_id VARCHAR(256),
    ADD COLUMN IF NOT EXISTS source_checksum VARCHAR(128),
    ADD COLUMN IF NOT EXISTS ci_reported_at TIMESTAMPTZ;

ALTER TABLE rbac3_permission
    DROP CONSTRAINT IF EXISTS ck_rbac3_permission_status,
    ADD CONSTRAINT ck_rbac3_permission_status
        CHECK (status IN ('PENDING_VALIDATION', 'ACTIVE', 'DEPRECATED', 'ARCHIVED')),
    ADD CONSTRAINT ck_rbac3_permission_source
        CHECK (source_type IN ('MANUAL', 'CI_REPORT'));

ALTER TABLE rbac3_resource
    DROP CONSTRAINT IF EXISTS ck_rbac3_resource_source,
    ADD CONSTRAINT ck_rbac3_resource_source
        CHECK (source_type IN ('MANUAL', 'CI_REPORT'));

ALTER TABLE rbac3_permission_resource
    ADD CONSTRAINT ck_rbac3_permission_resource_source
        CHECK (source_type IN ('MANUAL', 'CI_REPORT'));

ALTER TABLE rbac3_field_definition
    DROP CONSTRAINT IF EXISTS ck_rbac3_field_definition_status,
    ADD CONSTRAINT ck_rbac3_field_definition_status
        CHECK (status IN (
            'PENDING_VALIDATION', 'ACTIVE', 'STALE', 'DISABLED', 'ARCHIVED')),
    ADD CONSTRAINT ck_rbac3_field_definition_source
        CHECK (source_type IN ('MANUAL', 'CI_REPORT'));

ALTER TABLE rbac3_application
    ADD CONSTRAINT ck_rbac3_application_ci_report
        CHECK (
            (ci_report_build_id IS NULL
                AND ci_report_checksum IS NULL
                AND ci_reported_at IS NULL)
            OR (ci_report_build_id IS NOT NULL
                AND ci_report_checksum IS NOT NULL
                AND ci_reported_at IS NOT NULL)
        );

CREATE UNIQUE INDEX uk_rbac3_application_code_global
    ON rbac3_application (application_code);
CREATE UNIQUE INDEX uk_rbac3_application_ddc_application_global
    ON rbac3_application (ddc_application_id);
CREATE INDEX idx_rbac3_application_business_code
    ON rbac3_application (ddc_business_id, status);
CREATE UNIQUE INDEX uk_rbac3_permission_code_global
    ON rbac3_permission (permission_code);
CREATE UNIQUE INDEX uk_rbac3_resource_code_global
    ON rbac3_resource (application_id, resource_type, resource_code);
CREATE UNIQUE INDEX uk_rbac3_permission_resource_mapping_global
    ON rbac3_permission_resource (resource_id, mapping_version);
CREATE UNIQUE INDEX uk_rbac3_field_definition_code_global
    ON rbac3_field_definition (application_id, resource_id, field_code);

CREATE INDEX idx_rbac3_permission_source
    ON rbac3_permission (application_id, source_type, status);
CREATE INDEX idx_rbac3_resource_source
    ON rbac3_resource (application_id, source_type, status, resource_type);
CREATE INDEX idx_rbac3_field_definition_source
    ON rbac3_field_definition (application_id, source_type, status);

CREATE TABLE rbac3_tenant_application (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
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
    CONSTRAINT uq_rbac3_tenant_application_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_rbac3_tenant_application_tenant_app
        UNIQUE (tenant_id, application_id),
    CONSTRAINT fk_rbac3_tenant_application_tenant
        FOREIGN KEY (tenant_id) REFERENCES rbac3_tenant(id),
    CONSTRAINT fk_rbac3_tenant_application_application
        FOREIGN KEY (application_id) REFERENCES rbac3_application(id),
    CONSTRAINT ck_rbac3_tenant_application_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED', 'EXPIRED')),
    CONSTRAINT ck_rbac3_tenant_application_source
        CHECK (source_type IN ('MANUAL', 'PURCHASE', 'SYSTEM')),
    CONSTRAINT ck_rbac3_tenant_application_window
        CHECK (valid_to IS NULL OR valid_to > valid_from),
    CONSTRAINT ck_rbac3_tenant_application_version CHECK (version >= 0)
);

CREATE INDEX idx_rbac3_tenant_application_status
    ON rbac3_tenant_application (tenant_id, status, valid_from, valid_to);

/* Rebuild the global catalog FKs and the tenant-fact -> global-id boundary. */
ALTER TABLE rbac3_resource
    ADD CONSTRAINT fk_rbac3_resource_application_global
        FOREIGN KEY (application_id) REFERENCES rbac3_application(id),
    ADD CONSTRAINT fk_rbac3_resource_parent_global
        FOREIGN KEY (parent_resource_id) REFERENCES rbac3_resource(id),
    ADD CONSTRAINT fk_rbac3_resource_required_permission_global
        FOREIGN KEY (required_permission_id) REFERENCES rbac3_permission(id);

ALTER TABLE rbac3_permission
    ADD CONSTRAINT fk_rbac3_permission_application_global
        FOREIGN KEY (application_id) REFERENCES rbac3_application(id);

ALTER TABLE rbac3_permission_resource
    ADD CONSTRAINT fk_rbac3_permission_resource_application_global
        FOREIGN KEY (application_id) REFERENCES rbac3_application(id),
    ADD CONSTRAINT fk_rbac3_permission_resource_permission_global
        FOREIGN KEY (permission_id) REFERENCES rbac3_permission(id),
    ADD CONSTRAINT fk_rbac3_permission_resource_resource_global
        FOREIGN KEY (resource_id) REFERENCES rbac3_resource(id);

ALTER TABLE rbac3_field_definition
    ADD CONSTRAINT fk_rbac3_field_definition_application_global
        FOREIGN KEY (application_id) REFERENCES rbac3_application(id),
    ADD CONSTRAINT fk_rbac3_field_definition_resource_global
        FOREIGN KEY (resource_id) REFERENCES rbac3_resource(id);

ALTER TABLE rbac3_role
    ADD CONSTRAINT fk_rbac3_role_application_global
        FOREIGN KEY (application_id) REFERENCES rbac3_application(id),
    ADD CONSTRAINT fk_rbac3_role_landing_route_global
        FOREIGN KEY (landing_route_id) REFERENCES rbac3_resource(id);

ALTER TABLE rbac3_role_permission
    ADD CONSTRAINT fk_rbac3_role_permission_permission_global
        FOREIGN KEY (permission_id) REFERENCES rbac3_permission(id);

ALTER TABLE rbac3_field_rule
    ADD CONSTRAINT fk_rbac3_field_rule_permission_global
        FOREIGN KEY (permission_id) REFERENCES rbac3_permission(id),
    ADD CONSTRAINT fk_rbac3_field_rule_definition_global
        FOREIGN KEY (field_definition_id) REFERENCES rbac3_field_definition(id);

ALTER TABLE rbac3_data_rule
    ADD CONSTRAINT fk_rbac3_data_rule_permission_global
        FOREIGN KEY (permission_id) REFERENCES rbac3_permission(id);

ALTER TABLE rbac3_service_principal
    ADD CONSTRAINT fk_rbac3_service_principal_application_global
        FOREIGN KEY (application_id) REFERENCES rbac3_application(id);

ALTER TABLE rbac3_service_permission
    ADD CONSTRAINT fk_rbac3_service_permission_application_global
        FOREIGN KEY (application_id) REFERENCES rbac3_application(id),
    ADD CONSTRAINT fk_rbac3_service_permission_permission_global
        FOREIGN KEY (permission_id) REFERENCES rbac3_permission(id);

ALTER TABLE rbac3_sod_set
    ADD CONSTRAINT fk_rbac3_sod_set_application_global
        FOREIGN KEY (application_id) REFERENCES rbac3_application(id);

ALTER TABLE rbac3_operation_sod_rule
    ADD CONSTRAINT fk_rbac3_operation_sod_application_global
        FOREIGN KEY (application_id) REFERENCES rbac3_application(id);

ALTER TABLE rbac3_business_participation
    ADD CONSTRAINT fk_rbac3_business_participation_application_global
        FOREIGN KEY (application_id) REFERENCES rbac3_application(id);

DROP TABLE IF EXISTS rbac3_resource_manifest CASCADE;
