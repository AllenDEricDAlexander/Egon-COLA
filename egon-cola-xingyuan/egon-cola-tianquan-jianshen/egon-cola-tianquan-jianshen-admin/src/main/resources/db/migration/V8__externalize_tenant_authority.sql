/*
 * Externalize tenant catalog ownership to IdP while retaining RBAC policy state.
 *
 * The maintenance tool must set the verified gate settings in the same database
 * session before Flyway applies this destructive migration. Missing or inconsistent
 * gate facts abort before any DDL can commit.
 */
DO $$
DECLARE
    gate_id TEXT := current_setting('rbac3.tenant_authority.gate_id', true);
    gate_checksum TEXT := current_setting(
        'rbac3.tenant_authority.gate_checksum', true);
    source_count_text TEXT := current_setting(
        'rbac3.tenant_authority.source_count', true);
    orphan_count_text TEXT := current_setting(
        'rbac3.tenant_authority.orphan_count', true);
    duplicate_count_text TEXT := current_setting(
        'rbac3.tenant_authority.duplicate_count', true);
    placeholder_count_text TEXT := current_setting(
        'rbac3.tenant_authority.placeholder_count', true);
    source_count BIGINT;
    actual_count BIGINT;
BEGIN
    IF gate_id IS DISTINCT FROM 'VERIFIED'
            OR gate_checksum IS NULL
            OR btrim(gate_checksum) = '' THEN
        RAISE EXCEPTION
            'rbac3 tenant authority gate is not verified';
    END IF;
    IF source_count_text IS NULL
            OR source_count_text !~ '^[0-9]+$'
            OR orphan_count_text IS DISTINCT FROM '0'
            OR duplicate_count_text IS DISTINCT FROM '0'
            OR placeholder_count_text IS DISTINCT FROM '0' THEN
        RAISE EXCEPTION
            'rbac3 tenant authority gate facts are invalid';
    END IF;
    source_count := source_count_text::BIGINT;
    SELECT count(*) INTO actual_count FROM rbac3_tenant;
    IF actual_count <> source_count THEN
        RAISE EXCEPTION
            'rbac3 tenant authority source count does not match verified gate';
    END IF;
END
$$;

CREATE TABLE rbac3_tenant_authorization_state (
    tenant_id       BIGINT PRIMARY KEY,
    policy_version  BIGINT NOT NULL DEFAULT 0,
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL,
    created_by      VARCHAR(128) NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    updated_by      VARCHAR(128) NOT NULL,
    CONSTRAINT ck_rbac3_tenant_authorization_policy_version
        CHECK (policy_version >= 0),
    CONSTRAINT ck_rbac3_tenant_authorization_version
        CHECK (version >= 0)
);

INSERT INTO rbac3_tenant_authorization_state (
    tenant_id,
    policy_version,
    version,
    created_at,
    created_by,
    updated_at,
    updated_by
)
SELECT
    id,
    policy_version,
    version,
    created_at,
    created_by,
    updated_at,
    updated_by
FROM rbac3_tenant;

DO $$
DECLARE
    gate_count BIGINT := current_setting(
        'rbac3.tenant_authority.source_count')::BIGINT;
    state_count BIGINT;
BEGIN
    SELECT count(*) INTO state_count
      FROM rbac3_tenant_authorization_state;
    IF state_count <> gate_count THEN
        RAISE EXCEPTION
            'rbac3 tenant authorization state coverage does not match gate';
    END IF;
END
$$;

/* Repoint every actual inbound tenant FK without changing child tenant_id values. */
DO $$
DECLARE
    constraint_row RECORD;
    original_definition TEXT;
    replacement_definition TEXT;
BEGIN
    FOR constraint_row IN
        SELECT constraint_def.oid,
               child_ns.nspname AS child_schema,
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
           AND parent.relname = 'rbac3_tenant'
           AND child_ns.nspname = current_schema()
    LOOP
        original_definition := pg_get_constraintdef(constraint_row.oid);
        replacement_definition := replace(
            original_definition,
            'REFERENCES rbac3_tenant(id)',
            'REFERENCES rbac3_tenant_authorization_state(tenant_id)'
        );
        replacement_definition := replace(
            replacement_definition,
            'REFERENCES public.rbac3_tenant(id)',
            'REFERENCES rbac3_tenant_authorization_state(tenant_id)'
        );
        IF replacement_definition = original_definition THEN
            RAISE EXCEPTION
                'rbac3 tenant FK definition could not be retargeted';
        END IF;
        EXECUTE format(
            'ALTER TABLE %I.%I DROP CONSTRAINT %I',
            constraint_row.child_schema,
            constraint_row.child_table,
            constraint_row.constraint_name
        );
        EXECUTE format(
            'ALTER TABLE %I.%I ADD CONSTRAINT %I %s',
            constraint_row.child_schema,
            constraint_row.child_table,
            constraint_row.constraint_name,
            replacement_definition
        );
    END LOOP;
END
$$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
          FROM pg_constraint constraint_def
          JOIN pg_class parent
            ON parent.oid = constraint_def.confrelid
         WHERE constraint_def.contype = 'f'
           AND parent.relname = 'rbac3_tenant'
    ) THEN
        RAISE EXCEPTION
            'zero remaining references to rbac3_tenant was not proven';
    END IF;
END
$$;

DROP TABLE rbac3_tenant;
