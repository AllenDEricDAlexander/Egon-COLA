/*
 * Fresh RBAC3 schema at the current resource-grant boundary.
 *
 * Empty databases start here without replaying the retired identity/session
 * migrations. Existing versioned migration files remain immutable.
 * Local tenant/user seeds use exact IdP IDs supplied through connection settings;
 * no historical user data, tenant IDs, credentials or Flyway history are copied.
 */

CREATE FUNCTION rbac3_reject_append_only_change() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    RAISE EXCEPTION '% is append-only', TG_TABLE_NAME
        USING ERRCODE = '55000';
    RETURN OLD;
END;
$$;

CREATE FUNCTION rbac3_reject_immutable_column_change() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    column_name TEXT;
BEGIN
    FOREACH column_name IN ARRAY TG_ARGV LOOP
        IF (to_jsonb(NEW) -> column_name)
                IS DISTINCT FROM (to_jsonb(OLD) -> column_name) THEN
            RAISE EXCEPTION '% column %.% is immutable',
                TG_OP, TG_TABLE_NAME, column_name
                USING ERRCODE = '55000';
        END IF;
    END LOOP;
    RETURN NEW;
END;
$$;

CREATE FUNCTION rbac3_sync_application_identity() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF NEW.application_id IS NULL AND NEW.application_code IS NOT NULL THEN
        SELECT application.id
        INTO NEW.application_id
        FROM rbac3_application application
        WHERE application.application_code = NEW.application_code;
    ELSIF NEW.application_code IS NULL AND NEW.application_id IS NOT NULL THEN
        SELECT application.application_code
        INTO NEW.application_code
        FROM rbac3_application application
        WHERE application.id = NEW.application_id;
    ELSIF NEW.application_id IS NOT NULL
            AND NEW.application_code IS NOT NULL THEN
        PERFORM 1
        FROM rbac3_application application
        WHERE application.id = NEW.application_id
          AND application.application_code = NEW.application_code;
        IF NOT FOUND THEN
            RAISE EXCEPTION
                'RBAC3 application id and code do not identify the same application'
                USING ERRCODE = '23503';
        END IF;
    END IF;

    IF NEW.application_id IS NULL OR NEW.application_code IS NULL THEN
        RAISE EXCEPTION 'RBAC3 application identity is required'
            USING ERRCODE = '23502';
    END IF;
    RETURN NEW;
END
$$;

CREATE TABLE rbac3_application (
    id bigint NOT NULL,
    application_code character varying(128) NOT NULL,
    application_name character varying(200) NOT NULL,
    display_priority integer DEFAULT 1000 NOT NULL,
    status character varying(32) NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    ddc_application_id character varying(64) NOT NULL,
    ddc_business_id character varying(64) NOT NULL,
    ci_report_build_id character varying(256),
    ci_report_checksum character varying(128),
    ci_reported_at timestamp with time zone,
    CONSTRAINT ck_rbac3_application_ci_report CHECK ((((ci_report_build_id IS NULL) AND (ci_report_checksum IS NULL) AND (ci_reported_at IS NULL)) OR ((ci_report_build_id IS NOT NULL) AND (ci_report_checksum IS NOT NULL) AND (ci_reported_at IS NOT NULL)))),
    CONSTRAINT ck_rbac3_application_priority CHECK ((display_priority >= 0)),
    CONSTRAINT ck_rbac3_application_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'DISABLED'::character varying, 'ARCHIVED'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_application_version CHECK ((version >= 0))
);

CREATE TABLE rbac3_audit_log (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    event_type character varying(128) NOT NULL,
    outcome character varying(32) NOT NULL,
    severity character varying(32) NOT NULL,
    actor_type character varying(32) NOT NULL,
    actor_id character varying(128) NOT NULL,
    target_type character varying(128),
    target_id character varying(128),
    management_policy_id bigint,
    reason_code character varying(128),
    request_id character varying(128) NOT NULL,
    trace_id character varying(128) NOT NULL,
    client_ip inet,
    user_agent text,
    before_snapshot jsonb,
    after_snapshot jsonb,
    payload_checksum character varying(256) NOT NULL,
    created_at timestamp with time zone NOT NULL,
    CONSTRAINT ck_rbac3_audit_actor CHECK (((actor_type)::text = ANY ((ARRAY['USER'::character varying, 'SERVICE'::character varying, 'SYSTEM'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_audit_checksum CHECK ((length((payload_checksum)::text) > 0)),
    CONSTRAINT ck_rbac3_audit_outcome CHECK (((outcome)::text = ANY ((ARRAY['SUCCESS'::character varying, 'DENIED'::character varying, 'FAILED'::character varying, 'PENDING_PROPAGATION'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_audit_severity CHECK (((severity)::text = ANY ((ARRAY['INFO'::character varying, 'WARN'::character varying, 'HIGH'::character varying, 'CRITICAL'::character varying])::text[])))
);

CREATE TABLE rbac3_authorization_mutation (
    mutation_id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    user_id bigint,
    scope_type character varying(32) NOT NULL,
    command_id character varying(128) NOT NULL,
    status character varying(32) NOT NULL,
    old_auth_version bigint,
    new_auth_version bigint,
    old_policy_version bigint,
    new_policy_version bigint,
    guard_created_at timestamp with time zone,
    committed_at timestamp with time zone,
    projected_at timestamp with time zone,
    completed_at timestamp with time zone,
    last_error_code character varying(128),
    attempt integer DEFAULT 0 NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    CONSTRAINT ck_rbac3_authorization_mutation_attempt CHECK ((attempt >= 0)),
    CONSTRAINT ck_rbac3_authorization_mutation_scope CHECK (((scope_type)::text = ANY ((ARRAY['USER'::character varying, 'TENANT'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_authorization_mutation_scope_target CHECK (((((scope_type)::text = 'USER'::text) AND (user_id IS NOT NULL)) OR (((scope_type)::text = 'TENANT'::text) AND (user_id IS NULL)))),
    CONSTRAINT ck_rbac3_authorization_mutation_status CHECK (((status)::text = ANY ((ARRAY['PREPARING'::character varying, 'COMMITTED'::character varying, 'PROJECTED'::character varying, 'COMPLETED'::character varying, 'ABORTED'::character varying, 'RECOVERY_REQUIRED'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_authorization_mutation_user_target CHECK ((((scope_type)::text <> 'USER'::text) OR (user_id IS NOT NULL))),
    CONSTRAINT ck_rbac3_authorization_mutation_versions CHECK ((((old_auth_version IS NULL) OR (old_auth_version >= 0)) AND ((new_auth_version IS NULL) OR (new_auth_version >= 0)) AND ((old_policy_version IS NULL) OR (old_policy_version >= 0)) AND ((new_policy_version IS NULL) OR (new_policy_version >= 0)) AND (version >= 0)))
);

CREATE TABLE rbac3_auto_assignment_rule (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    rule_code character varying(128) NOT NULL,
    match_type character varying(32) NOT NULL,
    match_ref_id bigint,
    role_id bigint NOT NULL,
    status character varying(32) NOT NULL,
    valid_from timestamp with time zone NOT NULL,
    valid_to timestamp with time zone,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    CONSTRAINT ck_rbac3_auto_assignment_match CHECK (((((match_type)::text = 'ALL_ACTIVE_USERS'::text) AND (match_ref_id IS NULL)) OR (((match_type)::text = 'POSITION'::text) AND (match_ref_id IS NOT NULL)))),
    CONSTRAINT ck_rbac3_auto_assignment_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'DISABLED'::character varying, 'EXPIRED'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_auto_assignment_version CHECK ((version >= 0)),
    CONSTRAINT ck_rbac3_auto_assignment_window CHECK (((valid_to IS NULL) OR (valid_to > valid_from)))
);

CREATE TABLE rbac3_business_participation (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    business_resource character varying(128) NOT NULL,
    business_id character varying(256) NOT NULL,
    actor_user_id bigint NOT NULL,
    action_code character varying(128) NOT NULL,
    business_event_id character varying(128) NOT NULL,
    occurred_at timestamp with time zone NOT NULL,
    trace_id character varying(128) NOT NULL,
    payload_digest character varying(128) NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    application_id bigint NOT NULL,
    application_code character varying(128) NOT NULL
);

CREATE TABLE rbac3_data_rule (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    application_id bigint NOT NULL,
    role_id bigint NOT NULL,
    permission_id bigint NOT NULL,
    scope_type character varying(32) NOT NULL,
    directory_snapshot_version bigint,
    status character varying(32) NOT NULL,
    valid_from timestamp with time zone NOT NULL,
    valid_to timestamp with time zone,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    CONSTRAINT ck_rbac3_data_rule_directory_version CHECK (((directory_snapshot_version IS NULL) OR (directory_snapshot_version >= 0))),
    CONSTRAINT ck_rbac3_data_rule_scope CHECK (((scope_type)::text = ANY ((ARRAY['ALL'::character varying, 'SELF'::character varying, 'DEPT'::character varying, 'DEPT_TREE'::character varying, 'ORG'::character varying, 'ORG_TREE'::character varying, 'CUSTOM'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_data_rule_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'DISABLED'::character varying, 'EXPIRED'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_data_rule_version CHECK ((version >= 0)),
    CONSTRAINT ck_rbac3_data_rule_window CHECK (((valid_to IS NULL) OR (valid_to > valid_from)))
);

CREATE TABLE rbac3_data_rule_ref (
    tenant_id bigint NOT NULL,
    data_rule_id bigint NOT NULL,
    ref_type character varying(32) NOT NULL,
    ref_id bigint NOT NULL,
    CONSTRAINT ck_rbac3_data_rule_ref_type CHECK (((ref_type)::text = ANY ((ARRAY['USER'::character varying, 'DEPT'::character varying, 'ORG'::character varying, 'POSITION'::character varying])::text[])))
);

CREATE TABLE rbac3_directory_snapshot (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    provider_code character varying(128) NOT NULL,
    snapshot_version bigint NOT NULL,
    checksum character varying(128) NOT NULL,
    status character varying(32) NOT NULL,
    generated_at timestamp with time zone NOT NULL,
    received_at timestamp with time zone NOT NULL,
    activated_at timestamp with time zone,
    payload jsonb NOT NULL,
    counts jsonb DEFAULT '{}'::jsonb NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    CONSTRAINT ck_rbac3_directory_snapshot_status CHECK (((status)::text = ANY ((ARRAY['RECEIVED'::character varying, 'VALIDATED'::character varying, 'ACTIVE'::character varying, 'REJECTED'::character varying, 'ARCHIVED'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_directory_snapshot_version CHECK (((snapshot_version >= 0) AND (version >= 0)))
);

CREATE TABLE rbac3_field_definition (
    id bigint NOT NULL,
    application_id bigint NOT NULL,
    resource_id bigint NOT NULL,
    field_code character varying(128) NOT NULL,
    json_path character varying(512) NOT NULL,
    data_type character varying(32) NOT NULL,
    sensitivity character varying(32) NOT NULL,
    default_access character varying(32) NOT NULL,
    masking_strategy character varying(32),
    writable boolean DEFAULT false NOT NULL,
    exportable boolean DEFAULT false NOT NULL,
    status character varying(32) NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    source_type character varying(32) DEFAULT 'MANUAL'::character varying NOT NULL,
    source_build_id character varying(256),
    source_checksum character varying(128),
    ci_reported_at timestamp with time zone,
    CONSTRAINT ck_rbac3_field_definition_access CHECK (((default_access)::text = ANY ((ARRAY['NONE'::character varying, 'MASKED_READ'::character varying, 'READ'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_field_definition_sensitivity CHECK (((sensitivity)::text = ANY ((ARRAY['NORMAL'::character varying, 'INTERNAL'::character varying, 'CONFIDENTIAL'::character varying, 'HIGH'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_field_definition_source_v13 CHECK (((source_type)::text = ANY ((ARRAY['MANUAL'::character varying, 'CI_REGISTRATION'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_field_definition_status CHECK (((status)::text = ANY ((ARRAY['PENDING_VALIDATION'::character varying, 'ACTIVE'::character varying, 'STALE'::character varying, 'DISABLED'::character varying, 'ARCHIVED'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_field_definition_type CHECK (((data_type)::text = ANY ((ARRAY['STRING'::character varying, 'NUMBER'::character varying, 'BOOLEAN'::character varying, 'DATE'::character varying, 'DATETIME'::character varying, 'OBJECT'::character varying, 'ARRAY'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_field_definition_version CHECK ((version >= 0))
);

CREATE TABLE rbac3_field_rule (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    application_id bigint NOT NULL,
    role_id bigint NOT NULL,
    permission_id bigint NOT NULL,
    field_definition_id bigint NOT NULL,
    access_level character varying(32) NOT NULL,
    valid_from timestamp with time zone NOT NULL,
    valid_to timestamp with time zone,
    status character varying(32) NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    CONSTRAINT ck_rbac3_field_rule_access CHECK (((access_level)::text = ANY ((ARRAY['NONE'::character varying, 'MASKED_READ'::character varying, 'READ'::character varying, 'WRITE'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_field_rule_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'DISABLED'::character varying, 'EXPIRED'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_field_rule_version CHECK ((version >= 0)),
    CONSTRAINT ck_rbac3_field_rule_window CHECK (((valid_to IS NULL) OR (valid_to > valid_from)))
);

CREATE TABLE rbac3_idempotency_record (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    actor_type character varying(32) NOT NULL,
    actor_id character varying(128) NOT NULL,
    operation_code character varying(128) NOT NULL,
    key_hash character varying(256) NOT NULL,
    request_hash character varying(256) NOT NULL,
    resource_type character varying(128),
    resource_id character varying(128),
    response_status integer,
    response_digest character varying(256),
    status character varying(32) NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    CONSTRAINT ck_rbac3_idempotency_actor CHECK (((actor_type)::text = ANY ((ARRAY['USER'::character varying, 'SERVICE'::character varying, 'SYSTEM'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_idempotency_response_status CHECK (((response_status IS NULL) OR ((response_status >= 100) AND (response_status <= 599)))),
    CONSTRAINT ck_rbac3_idempotency_status CHECK (((status)::text = ANY ((ARRAY['PROCESSING'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_idempotency_version CHECK ((version >= 0))
);

CREATE TABLE rbac3_management_operation (
    tenant_id bigint NOT NULL,
    policy_id bigint NOT NULL,
    operation_code character varying(32) NOT NULL,
    CONSTRAINT ck_rbac3_management_operation_code CHECK (((operation_code)::text = ANY ((ARRAY['VIEW_ASSIGNMENT'::character varying, 'ASSIGN_ROLE'::character varying, 'REVOKE_ROLE'::character varying, 'SUSPEND_ROLE'::character varying, 'RESUME_ROLE'::character varying, 'TEMPORARY_ASSIGN'::character varying, 'VIEW_AUDIT'::character varying, 'VIEW_IMPACT'::character varying, 'SELF_REVOKE_LOW_RISK'::character varying])::text[])))
);

CREATE TABLE rbac3_management_policy (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    policy_code character varying(128) NOT NULL,
    name character varying(200) NOT NULL,
    status character varying(32) NOT NULL,
    valid_from timestamp with time zone NOT NULL,
    valid_to timestamp with time zone,
    max_assignment_days integer,
    max_risk_level character varying(32) NOT NULL,
    required_auth_strength character varying(32) NOT NULL,
    require_reason boolean DEFAULT false NOT NULL,
    require_ticket boolean DEFAULT false NOT NULL,
    include_inherited_subject_roles boolean DEFAULT false CONSTRAINT rbac3_management_policy_include_inherited_subject_role_not_null NOT NULL,
    require_all_affiliations_in_scope boolean DEFAULT false CONSTRAINT rbac3_management_policy_require_all_affiliations_in_sc_not_null NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    CONSTRAINT ck_rbac3_management_policy_auth CHECK (((required_auth_strength)::text = ANY ((ARRAY['PASSWORD'::character varying, 'MFA'::character varying, 'STRONG'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_management_policy_days CHECK (((max_assignment_days IS NULL) OR (max_assignment_days > 0))),
    CONSTRAINT ck_rbac3_management_policy_risk CHECK (((max_risk_level)::text = ANY ((ARRAY['LOW'::character varying, 'MEDIUM'::character varying, 'HIGH'::character varying, 'CRITICAL'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_management_policy_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'DISABLED'::character varying, 'EXPIRED'::character varying, 'ARCHIVED'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_management_policy_version CHECK ((version >= 0)),
    CONSTRAINT ck_rbac3_management_policy_window CHECK (((valid_to IS NULL) OR (valid_to > valid_from)))
);

CREATE TABLE rbac3_management_role (
    tenant_id bigint NOT NULL,
    policy_id bigint NOT NULL,
    role_id bigint NOT NULL
);

CREATE TABLE rbac3_management_scope (
    tenant_id bigint NOT NULL,
    policy_id bigint NOT NULL,
    scope_type character varying(32) NOT NULL,
    scope_ref_id bigint,
    CONSTRAINT ck_rbac3_management_scope_ref CHECK ((((scope_type)::text = 'SELF_DEPT'::text) OR (scope_ref_id IS NOT NULL))),
    CONSTRAINT ck_rbac3_management_scope_type CHECK (((scope_type)::text = ANY ((ARRAY['SELF_DEPT'::character varying, 'DEPT'::character varying, 'DEPT_TREE'::character varying, 'ORG'::character varying, 'ORG_TREE'::character varying, 'CUSTOM_DEPT'::character varying, 'CUSTOM_USER'::character varying])::text[])))
);

CREATE TABLE rbac3_management_subject (
    tenant_id bigint NOT NULL,
    policy_id bigint NOT NULL,
    subject_type character varying(32) NOT NULL,
    subject_id bigint NOT NULL,
    CONSTRAINT ck_rbac3_management_subject_type CHECK (((subject_type)::text = ANY ((ARRAY['USER'::character varying, 'ROLE'::character varying, 'POSITION'::character varying])::text[])))
);

CREATE TABLE rbac3_operation_sod_rule (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    business_resource character varying(128) NOT NULL,
    prior_action_code character varying(128) NOT NULL,
    forbidden_later_action_code character varying(128) NOT NULL,
    lookback_from timestamp with time zone,
    status character varying(32) NOT NULL,
    valid_from timestamp with time zone NOT NULL,
    valid_to timestamp with time zone,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    application_id bigint NOT NULL,
    application_code character varying(128) NOT NULL,
    CONSTRAINT ck_rbac3_operation_sod_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'DISABLED'::character varying, 'EXPIRED'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_operation_sod_version CHECK ((version >= 0)),
    CONSTRAINT ck_rbac3_operation_sod_window CHECK (((valid_to IS NULL) OR (valid_to > valid_from)))
);

CREATE TABLE rbac3_org_unit (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    snapshot_id bigint,
    unit_type character varying(32) NOT NULL,
    code character varying(128) NOT NULL,
    name character varying(200) NOT NULL,
    parent_id bigint,
    path text NOT NULL,
    depth integer NOT NULL,
    status character varying(32) NOT NULL,
    external_id character varying(256),
    valid_from timestamp with time zone NOT NULL,
    valid_to timestamp with time zone,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    source_type character varying(32) NOT NULL,
    CONSTRAINT ck_rbac3_org_unit_depth CHECK (((depth >= 0) AND (depth <= 20))),
    CONSTRAINT ck_rbac3_org_unit_source CHECK (((((source_type)::text = 'DIRECTORY_SNAPSHOT'::text) AND (snapshot_id IS NOT NULL)) OR (((source_type)::text = 'MANUAL'::text) AND (snapshot_id IS NULL)))),
    CONSTRAINT ck_rbac3_org_unit_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'ARCHIVED'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_org_unit_type CHECK (((unit_type)::text = ANY ((ARRAY['ORG'::character varying, 'DEPT'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_org_unit_version CHECK ((version >= 0)),
    CONSTRAINT ck_rbac3_org_unit_window CHECK (((valid_to IS NULL) OR (valid_to > valid_from)))
);

CREATE TABLE rbac3_permission (
    id bigint NOT NULL,
    application_id bigint NOT NULL,
    permission_code character varying(128) NOT NULL,
    permission_name character varying(200) NOT NULL,
    risk_level character varying(32) NOT NULL,
    status character varying(32) NOT NULL,
    description text,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    source_type character varying(32) DEFAULT 'MANUAL'::character varying NOT NULL,
    source_build_id character varying(256),
    source_checksum character varying(128),
    ci_reported_at timestamp with time zone,
    CONSTRAINT ck_rbac3_permission_risk CHECK (((risk_level)::text = ANY ((ARRAY['LOW'::character varying, 'MEDIUM'::character varying, 'HIGH'::character varying, 'CRITICAL'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_permission_source_v13 CHECK (((source_type)::text = ANY ((ARRAY['MANUAL'::character varying, 'CI_REGISTRATION'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_permission_status CHECK (((status)::text = ANY ((ARRAY['PENDING_VALIDATION'::character varying, 'ACTIVE'::character varying, 'DEPRECATED'::character varying, 'ARCHIVED'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_permission_version CHECK ((version >= 0))
);

CREATE TABLE rbac3_position (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    snapshot_id bigint,
    code character varying(128) NOT NULL,
    name character varying(200) NOT NULL,
    org_unit_id bigint NOT NULL,
    status character varying(32) NOT NULL,
    external_id character varying(256),
    valid_from timestamp with time zone NOT NULL,
    valid_to timestamp with time zone,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    source_type character varying(32) NOT NULL,
    CONSTRAINT ck_rbac3_position_source CHECK (((((source_type)::text = 'DIRECTORY_SNAPSHOT'::text) AND (snapshot_id IS NOT NULL)) OR (((source_type)::text = 'MANUAL'::text) AND (snapshot_id IS NULL)))),
    CONSTRAINT ck_rbac3_position_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'ARCHIVED'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_position_version CHECK ((version >= 0)),
    CONSTRAINT ck_rbac3_position_window CHECK (((valid_to IS NULL) OR (valid_to > valid_from)))
);

CREATE TABLE rbac3_resource (
    id bigint NOT NULL,
    application_id bigint NOT NULL,
    resource_type character varying(32) NOT NULL,
    resource_code character varying(128) NOT NULL,
    resource_name character varying(200) NOT NULL,
    parent_resource_id bigint,
    required_permission_id bigint,
    status character varying(32) NOT NULL,
    source_build_id character varying(256),
    mechanical_facts jsonb DEFAULT '{}'::jsonb NOT NULL,
    display_metadata jsonb DEFAULT '{}'::jsonb NOT NULL,
    stale_since timestamp with time zone,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    source_type character varying(32) DEFAULT 'MANUAL'::character varying NOT NULL,
    source_checksum character varying(128),
    ci_reported_at timestamp with time zone,
    suggested_permission_code character varying(256),
    CONSTRAINT ck_rbac3_resource_source_v13 CHECK (((source_type)::text = ANY ((ARRAY['MANUAL'::character varying, 'CI_REGISTRATION'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_resource_status CHECK (((status)::text = ANY ((ARRAY['PENDING_VALIDATION'::character varying, 'ACTIVE'::character varying, 'STALE'::character varying, 'ARCHIVED'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_resource_type CHECK (((resource_type)::text = ANY ((ARRAY['APP'::character varying, 'MENU'::character varying, 'ROUTE'::character varying, 'ACTION'::character varying, 'API'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_resource_version CHECK ((version >= 0))
);

CREATE TABLE rbac3_resource_api_binding (
    id bigint NOT NULL,
    application_id bigint NOT NULL,
    source_resource_id bigint NOT NULL,
    api_resource_id bigint NOT NULL,
    source_build_id character varying(256) NOT NULL,
    source_checksum character varying(256) NOT NULL,
    status character varying(32) NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    CONSTRAINT ck_rbac3_resource_api_binding_distinct CHECK ((source_resource_id <> api_resource_id)),
    CONSTRAINT ck_rbac3_resource_api_binding_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'STALE'::character varying, 'DISABLED'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_resource_api_binding_version CHECK ((version >= 0))
);

CREATE TABLE rbac3_role (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    application_id bigint NOT NULL,
    role_code character varying(128) NOT NULL,
    role_name character varying(200) NOT NULL,
    role_type character varying(32) NOT NULL,
    risk_level character varying(32) NOT NULL,
    privileged boolean DEFAULT false NOT NULL,
    status character varying(32) NOT NULL,
    landing_route_id bigint,
    landing_priority integer DEFAULT 1000 NOT NULL,
    max_assignment_days integer,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    CONSTRAINT ck_rbac3_role_code CHECK (((role_code)::text ~ '^[A-Z][A-Z0-9_]{2,63}$'::text)),
    CONSTRAINT ck_rbac3_role_landing_priority CHECK ((landing_priority >= 0)),
    CONSTRAINT ck_rbac3_role_max_days CHECK (((max_assignment_days IS NULL) OR (max_assignment_days > 0))),
    CONSTRAINT ck_rbac3_role_risk CHECK (((risk_level)::text = ANY ((ARRAY['LOW'::character varying, 'MEDIUM'::character varying, 'HIGH'::character varying, 'CRITICAL'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_role_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'DISABLED'::character varying, 'ARCHIVED'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_role_type CHECK (((role_type)::text = ANY ((ARRAY['PUBLIC'::character varying, 'POSITION'::character varying, 'MANAGEMENT'::character varying, 'TEMPORARY'::character varying, 'EMERGENCY'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_role_version CHECK ((version >= 0))
);

CREATE TABLE rbac3_role_cardinality (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    role_id bigint NOT NULL,
    scope_type character varying(32) NOT NULL,
    max_active integer NOT NULL,
    status character varying(32) NOT NULL,
    valid_from timestamp with time zone NOT NULL,
    valid_to timestamp with time zone,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    CONSTRAINT ck_rbac3_role_cardinality_max CHECK ((max_active > 0)),
    CONSTRAINT ck_rbac3_role_cardinality_scope CHECK (((scope_type)::text = ANY ((ARRAY['TENANT'::character varying, 'ORG'::character varying, 'DEPT'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_role_cardinality_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'DISABLED'::character varying, 'EXPIRED'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_role_cardinality_version CHECK ((version >= 0)),
    CONSTRAINT ck_rbac3_role_cardinality_window CHECK (((valid_to IS NULL) OR (valid_to > valid_from)))
);

CREATE TABLE rbac3_role_closure (
    tenant_id bigint NOT NULL,
    application_id bigint NOT NULL,
    ancestor_role_id bigint NOT NULL,
    descendant_role_id bigint NOT NULL,
    depth integer NOT NULL,
    CONSTRAINT ck_rbac3_role_closure_depth CHECK (((depth >= 0) AND (depth <= 10))),
    CONSTRAINT ck_rbac3_role_closure_self CHECK (((ancestor_role_id = descendant_role_id) = (depth = 0)))
);

CREATE TABLE rbac3_role_inheritance (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    application_id bigint NOT NULL,
    senior_role_id bigint NOT NULL,
    junior_role_id bigint NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    CONSTRAINT ck_rbac3_role_inheritance_distinct CHECK ((senior_role_id <> junior_role_id)),
    CONSTRAINT ck_rbac3_role_inheritance_version CHECK ((version >= 0))
);

CREATE TABLE rbac3_role_prerequisite (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    target_role_id bigint NOT NULL,
    group_code character varying(128) NOT NULL,
    match_mode character varying(32) NOT NULL,
    prerequisite_role_id bigint NOT NULL,
    status character varying(32) NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    CONSTRAINT ck_rbac3_role_prerequisite_distinct CHECK ((target_role_id <> prerequisite_role_id)),
    CONSTRAINT ck_rbac3_role_prerequisite_mode CHECK (((match_mode)::text = ANY ((ARRAY['ALL_OF'::character varying, 'ANY_OF'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_role_prerequisite_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'DISABLED'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_role_prerequisite_version CHECK ((version >= 0))
);

CREATE TABLE rbac3_role_resource_grant (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    application_id bigint NOT NULL,
    role_id bigint NOT NULL,
    resource_id bigint NOT NULL,
    valid_from timestamp with time zone NOT NULL,
    valid_to timestamp with time zone,
    status character varying(32) NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    CONSTRAINT ck_rbac3_role_resource_grant_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'DISABLED'::character varying, 'EXPIRED'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_role_resource_grant_version CHECK ((version >= 0)),
    CONSTRAINT ck_rbac3_role_resource_grant_window CHECK (((valid_to IS NULL) OR (valid_to > valid_from)))
);

CREATE TABLE rbac3_service_permission (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    application_id bigint NOT NULL,
    principal_id bigint NOT NULL,
    permission_id bigint NOT NULL,
    valid_from timestamp with time zone NOT NULL,
    valid_to timestamp with time zone,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    application_code character varying(128) NOT NULL,
    CONSTRAINT ck_rbac3_service_permission_version CHECK ((version >= 0)),
    CONSTRAINT ck_rbac3_service_permission_window CHECK (((valid_to IS NULL) OR (valid_to > valid_from)))
);

CREATE TABLE rbac3_service_principal (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    service_code character varying(128) NOT NULL,
    application_code character varying(128) NOT NULL,
    display_name character varying(200) NOT NULL,
    status character varying(32) NOT NULL,
    allowed_envs jsonb DEFAULT '[]'::jsonb NOT NULL,
    allowed_namespaces jsonb DEFAULT '[]'::jsonb NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    application_id bigint NOT NULL,
    CONSTRAINT ck_rbac3_service_principal_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'DISABLED'::character varying, 'ARCHIVED'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_service_principal_version CHECK ((version >= 0))
);

CREATE TABLE rbac3_sod_member (
    tenant_id bigint NOT NULL,
    sod_set_id bigint NOT NULL,
    role_id bigint NOT NULL
);

CREATE TABLE rbac3_sod_set (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    application_id bigint,
    set_code character varying(128) NOT NULL,
    constraint_type character varying(32) NOT NULL,
    max_active_roles integer NOT NULL,
    status character varying(32) NOT NULL,
    valid_from timestamp with time zone NOT NULL,
    valid_to timestamp with time zone,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    CONSTRAINT ck_rbac3_sod_set_application CHECK ((((constraint_type)::text <> 'DSD'::text) OR (application_id IS NOT NULL))),
    CONSTRAINT ck_rbac3_sod_set_max CHECK ((max_active_roles >= 1)),
    CONSTRAINT ck_rbac3_sod_set_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'DISABLED'::character varying, 'EXPIRED'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_sod_set_type CHECK (((constraint_type)::text = ANY ((ARRAY['SSD'::character varying, 'DSD'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_sod_set_version CHECK ((version >= 0)),
    CONSTRAINT ck_rbac3_sod_set_window CHECK (((valid_to IS NULL) OR (valid_to > valid_from)))
);

CREATE TABLE rbac3_tenant_application (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    application_id bigint NOT NULL,
    status character varying(32) NOT NULL,
    valid_from timestamp with time zone NOT NULL,
    valid_to timestamp with time zone,
    source_type character varying(32) NOT NULL,
    source_id character varying(128) NOT NULL,
    reason character varying(500),
    ticket_no character varying(128),
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    CONSTRAINT ck_rbac3_tenant_application_source CHECK (((source_type)::text = ANY ((ARRAY['MANUAL'::character varying, 'PURCHASE'::character varying, 'SYSTEM'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_tenant_application_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'SUSPENDED'::character varying, 'REVOKED'::character varying, 'EXPIRED'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_tenant_application_version CHECK ((version >= 0)),
    CONSTRAINT ck_rbac3_tenant_application_window CHECK (((valid_to IS NULL) OR (valid_to > valid_from)))
);

CREATE TABLE rbac3_tenant_authorization_state (
    tenant_id bigint NOT NULL,
    policy_version bigint DEFAULT 0 NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    CONSTRAINT ck_rbac3_tenant_authorization_policy_version CHECK ((policy_version >= 0)),
    CONSTRAINT ck_rbac3_tenant_authorization_version CHECK ((version >= 0))
);

CREATE TABLE rbac3_user (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    status character varying(32) NOT NULL,
    auth_version bigint DEFAULT 0 NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    identity_sub character varying(200) NOT NULL,
    CONSTRAINT ck_rbac3_user_auth_version CHECK ((auth_version >= 0)),
    CONSTRAINT ck_rbac3_user_identity_sub CHECK ((length(btrim((identity_sub)::text)) > 0)),
    CONSTRAINT ck_rbac3_user_status CHECK (((status)::text = ANY ((ARRAY['INVITED'::character varying, 'ACTIVE'::character varying, 'LOCKED'::character varying, 'DISABLED'::character varying, 'ARCHIVED'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_user_version CHECK ((version >= 0))
);

CREATE TABLE rbac3_user_active_role (
    tenant_id bigint NOT NULL,
    user_id bigint NOT NULL,
    application_id bigint NOT NULL,
    root_role_id bigint NOT NULL,
    eligible_assignment_ids jsonb DEFAULT '[]'::jsonb NOT NULL,
    activated_at timestamp with time zone NOT NULL,
    CONSTRAINT ck_rbac3_user_active_role_evidence CHECK ((jsonb_typeof(eligible_assignment_ids) = 'array'::text))
);

CREATE TABLE rbac3_user_business_access (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    user_id bigint NOT NULL,
    ddc_business_id character varying(64) NOT NULL,
    status character varying(32) NOT NULL,
    valid_from timestamp with time zone NOT NULL,
    valid_to timestamp with time zone,
    source_type character varying(32) NOT NULL,
    source_id character varying(128) NOT NULL,
    reason character varying(500),
    ticket_no character varying(128),
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    CONSTRAINT ck_rbac3_user_business_access_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'SUSPENDED'::character varying, 'REVOKED'::character varying, 'EXPIRED'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_user_business_access_version CHECK ((version >= 0)),
    CONSTRAINT ck_rbac3_user_business_access_window CHECK (((valid_to IS NULL) OR (valid_to > valid_from)))
);

CREATE TABLE rbac3_user_org_assignment (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    user_id bigint NOT NULL,
    org_unit_id bigint NOT NULL,
    status character varying(32) NOT NULL,
    valid_from timestamp with time zone NOT NULL,
    valid_to timestamp with time zone,
    source_type character varying(32) NOT NULL,
    source_id character varying(128) NOT NULL,
    reason character varying(500),
    ticket_no character varying(128),
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    CONSTRAINT ck_rbac3_user_org_assignment_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'SUSPENDED'::character varying, 'REVOKED'::character varying, 'EXPIRED'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_user_org_assignment_version CHECK ((version >= 0)),
    CONSTRAINT ck_rbac3_user_org_assignment_window CHECK (((valid_to IS NULL) OR (valid_to > valid_from)))
);

CREATE TABLE rbac3_user_position_assignment (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    user_id bigint NOT NULL,
    org_unit_id bigint NOT NULL,
    position_id bigint NOT NULL,
    primary_assignment boolean DEFAULT false NOT NULL,
    status character varying(32) NOT NULL,
    valid_from timestamp with time zone NOT NULL,
    valid_to timestamp with time zone,
    source_type character varying(32) NOT NULL,
    source_id character varying(128) NOT NULL,
    reason character varying(500),
    ticket_no character varying(128),
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    CONSTRAINT ck_rbac3_user_position_assignment_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'SUSPENDED'::character varying, 'REVOKED'::character varying, 'EXPIRED'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_user_position_assignment_version CHECK ((version >= 0)),
    CONSTRAINT ck_rbac3_user_position_assignment_window CHECK (((valid_to IS NULL) OR (valid_to > valid_from)))
);

CREATE TABLE rbac3_user_position_snapshot (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    snapshot_id bigint NOT NULL,
    user_id bigint NOT NULL,
    position_id bigint NOT NULL,
    org_unit_id bigint NOT NULL,
    primary_flag boolean DEFAULT false NOT NULL,
    valid_from timestamp with time zone NOT NULL,
    valid_to timestamp with time zone,
    status character varying(32) NOT NULL,
    external_assignment_id character varying(256),
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    CONSTRAINT ck_rbac3_user_position_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'ARCHIVED'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_user_position_version CHECK ((version >= 0)),
    CONSTRAINT ck_rbac3_user_position_window CHECK (((valid_to IS NULL) OR (valid_to > valid_from)))
);

CREATE TABLE rbac3_user_role_assignment (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    user_id bigint NOT NULL,
    role_id bigint NOT NULL,
    assignment_type character varying(32) NOT NULL,
    status character varying(32) NOT NULL,
    valid_from timestamp with time zone NOT NULL,
    valid_to timestamp with time zone,
    source_type character varying(32) NOT NULL,
    source_id character varying(128) NOT NULL,
    reason character varying(500),
    ticket_no character varying(128),
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(128) NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by character varying(128) NOT NULL,
    CONSTRAINT ck_rbac3_assignment_bounded CHECK ((((assignment_type)::text <> ALL ((ARRAY['TEMPORARY'::character varying, 'EMERGENCY'::character varying])::text[])) OR (valid_to IS NOT NULL))),
    CONSTRAINT ck_rbac3_assignment_status CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'ACTIVE'::character varying, 'SUSPENDED'::character varying, 'EXPIRED'::character varying, 'REVOKED'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_assignment_type CHECK (((assignment_type)::text = ANY ((ARRAY['AUTO'::character varying, 'DIRECT'::character varying, 'TEMPORARY'::character varying, 'EMERGENCY'::character varying])::text[]))),
    CONSTRAINT ck_rbac3_assignment_version CHECK ((version >= 0)),
    CONSTRAINT ck_rbac3_assignment_window CHECK (((valid_to IS NULL) OR (valid_to > valid_from)))
);

ALTER TABLE ONLY rbac3_data_rule_ref
    ADD CONSTRAINT pk_rbac3_data_rule_ref PRIMARY KEY (tenant_id, data_rule_id, ref_type, ref_id);

ALTER TABLE ONLY rbac3_management_operation
    ADD CONSTRAINT pk_rbac3_management_operation PRIMARY KEY (tenant_id, policy_id, operation_code);

ALTER TABLE ONLY rbac3_management_role
    ADD CONSTRAINT pk_rbac3_management_role PRIMARY KEY (tenant_id, policy_id, role_id);

ALTER TABLE ONLY rbac3_management_subject
    ADD CONSTRAINT pk_rbac3_management_subject PRIMARY KEY (tenant_id, policy_id, subject_type, subject_id);

ALTER TABLE ONLY rbac3_role_closure
    ADD CONSTRAINT pk_rbac3_role_closure PRIMARY KEY (tenant_id, application_id, ancestor_role_id, descendant_role_id);

ALTER TABLE ONLY rbac3_sod_member
    ADD CONSTRAINT pk_rbac3_sod_member PRIMARY KEY (tenant_id, sod_set_id, role_id);

ALTER TABLE ONLY rbac3_application
    ADD CONSTRAINT rbac3_application_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_audit_log
    ADD CONSTRAINT rbac3_audit_log_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_auto_assignment_rule
    ADD CONSTRAINT rbac3_auto_assignment_rule_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_business_participation
    ADD CONSTRAINT rbac3_business_participation_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_data_rule
    ADD CONSTRAINT rbac3_data_rule_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_directory_snapshot
    ADD CONSTRAINT rbac3_directory_snapshot_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_field_definition
    ADD CONSTRAINT rbac3_field_definition_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_field_rule
    ADD CONSTRAINT rbac3_field_rule_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_idempotency_record
    ADD CONSTRAINT rbac3_idempotency_record_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_management_policy
    ADD CONSTRAINT rbac3_management_policy_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_operation_sod_rule
    ADD CONSTRAINT rbac3_operation_sod_rule_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_org_unit
    ADD CONSTRAINT rbac3_org_unit_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_permission
    ADD CONSTRAINT rbac3_permission_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_position
    ADD CONSTRAINT rbac3_position_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_resource_api_binding
    ADD CONSTRAINT rbac3_resource_api_binding_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_resource
    ADD CONSTRAINT rbac3_resource_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_role_cardinality
    ADD CONSTRAINT rbac3_role_cardinality_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_role_inheritance
    ADD CONSTRAINT rbac3_role_inheritance_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_role
    ADD CONSTRAINT rbac3_role_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_role_prerequisite
    ADD CONSTRAINT rbac3_role_prerequisite_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_role_resource_grant
    ADD CONSTRAINT rbac3_role_resource_grant_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_service_permission
    ADD CONSTRAINT rbac3_service_permission_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_service_principal
    ADD CONSTRAINT rbac3_service_principal_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_sod_set
    ADD CONSTRAINT rbac3_sod_set_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_tenant_application
    ADD CONSTRAINT rbac3_tenant_application_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_tenant_authorization_state
    ADD CONSTRAINT rbac3_tenant_authorization_state_pkey PRIMARY KEY (tenant_id);

ALTER TABLE ONLY rbac3_user_business_access
    ADD CONSTRAINT rbac3_user_business_access_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_user_org_assignment
    ADD CONSTRAINT rbac3_user_org_assignment_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_user
    ADD CONSTRAINT rbac3_user_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_user_position_assignment
    ADD CONSTRAINT rbac3_user_position_assignment_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_user_position_snapshot
    ADD CONSTRAINT rbac3_user_position_snapshot_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_user_role_assignment
    ADD CONSTRAINT rbac3_user_role_assignment_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rbac3_user_role_assignment
    ADD CONSTRAINT uq_rbac3_assignment_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY rbac3_authorization_mutation
    ADD CONSTRAINT uq_rbac3_authorization_mutation_command UNIQUE (command_id);

ALTER TABLE ONLY rbac3_authorization_mutation
    ADD CONSTRAINT uq_rbac3_authorization_mutation_id PRIMARY KEY (mutation_id);

ALTER TABLE ONLY rbac3_auto_assignment_rule
    ADD CONSTRAINT uq_rbac3_auto_assignment_code UNIQUE (tenant_id, rule_code);

ALTER TABLE ONLY rbac3_auto_assignment_rule
    ADD CONSTRAINT uq_rbac3_auto_assignment_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY rbac3_business_participation
    ADD CONSTRAINT uq_rbac3_business_participation_event UNIQUE (tenant_id, application_id, business_event_id);

ALTER TABLE ONLY rbac3_data_rule
    ADD CONSTRAINT uq_rbac3_data_rule_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY rbac3_directory_snapshot
    ADD CONSTRAINT uq_rbac3_directory_snapshot_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY rbac3_directory_snapshot
    ADD CONSTRAINT uq_rbac3_directory_snapshot_version UNIQUE (tenant_id, provider_code, snapshot_version);

ALTER TABLE ONLY rbac3_field_rule
    ADD CONSTRAINT uq_rbac3_field_rule_fact UNIQUE (tenant_id, role_id, permission_id, field_definition_id, valid_from);

ALTER TABLE ONLY rbac3_field_rule
    ADD CONSTRAINT uq_rbac3_field_rule_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY rbac3_idempotency_record
    ADD CONSTRAINT uq_rbac3_idempotency_key UNIQUE (tenant_id, actor_type, actor_id, operation_code, key_hash);

ALTER TABLE ONLY rbac3_idempotency_record
    ADD CONSTRAINT uq_rbac3_idempotency_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY rbac3_management_policy
    ADD CONSTRAINT uq_rbac3_management_policy_code UNIQUE (tenant_id, policy_code);

ALTER TABLE ONLY rbac3_management_policy
    ADD CONSTRAINT uq_rbac3_management_policy_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY rbac3_management_scope
    ADD CONSTRAINT uq_rbac3_management_scope UNIQUE (tenant_id, policy_id, scope_type, scope_ref_id);

ALTER TABLE ONLY rbac3_operation_sod_rule
    ADD CONSTRAINT uq_rbac3_operation_sod_fact UNIQUE (tenant_id, application_id, business_resource, prior_action_code, forbidden_later_action_code, valid_from);

ALTER TABLE ONLY rbac3_operation_sod_rule
    ADD CONSTRAINT uq_rbac3_operation_sod_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY rbac3_org_unit
    ADD CONSTRAINT uq_rbac3_org_unit_code UNIQUE (tenant_id, code);

ALTER TABLE ONLY rbac3_org_unit
    ADD CONSTRAINT uq_rbac3_org_unit_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY rbac3_position
    ADD CONSTRAINT uq_rbac3_position_code UNIQUE (tenant_id, code);

ALTER TABLE ONLY rbac3_position
    ADD CONSTRAINT uq_rbac3_position_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY rbac3_resource_api_binding
    ADD CONSTRAINT uq_rbac3_resource_api_binding_pair UNIQUE (source_resource_id, api_resource_id);

ALTER TABLE ONLY rbac3_resource
    ADD CONSTRAINT uq_rbac3_resource_application_id_v13 UNIQUE (application_id, id);

ALTER TABLE ONLY rbac3_role
    ADD CONSTRAINT uq_rbac3_role_application_id UNIQUE (tenant_id, application_id, id);

ALTER TABLE ONLY rbac3_role_cardinality
    ADD CONSTRAINT uq_rbac3_role_cardinality_fact UNIQUE (tenant_id, role_id, scope_type, valid_from);

ALTER TABLE ONLY rbac3_role_cardinality
    ADD CONSTRAINT uq_rbac3_role_cardinality_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY rbac3_role
    ADD CONSTRAINT uq_rbac3_role_code UNIQUE (tenant_id, application_id, role_code);

ALTER TABLE ONLY rbac3_role_inheritance
    ADD CONSTRAINT uq_rbac3_role_inheritance_edge UNIQUE (tenant_id, application_id, senior_role_id, junior_role_id);

ALTER TABLE ONLY rbac3_role_inheritance
    ADD CONSTRAINT uq_rbac3_role_inheritance_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY rbac3_role_prerequisite
    ADD CONSTRAINT uq_rbac3_role_prerequisite_fact UNIQUE (tenant_id, target_role_id, group_code, prerequisite_role_id);

ALTER TABLE ONLY rbac3_role_prerequisite
    ADD CONSTRAINT uq_rbac3_role_prerequisite_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY rbac3_role_resource_grant
    ADD CONSTRAINT uq_rbac3_role_resource_grant_fact UNIQUE (tenant_id, role_id, resource_id, valid_from);

ALTER TABLE ONLY rbac3_role
    ADD CONSTRAINT uq_rbac3_role_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY rbac3_service_permission
    ADD CONSTRAINT uq_rbac3_service_permission_fact UNIQUE (tenant_id, principal_id, permission_id, application_id);

ALTER TABLE ONLY rbac3_service_permission
    ADD CONSTRAINT uq_rbac3_service_permission_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY rbac3_service_principal
    ADD CONSTRAINT uq_rbac3_service_principal_application_id UNIQUE (tenant_id, application_id, id);

ALTER TABLE ONLY rbac3_service_principal
    ADD CONSTRAINT uq_rbac3_service_principal_code UNIQUE (tenant_id, service_code);

ALTER TABLE ONLY rbac3_service_principal
    ADD CONSTRAINT uq_rbac3_service_principal_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY rbac3_sod_set
    ADD CONSTRAINT uq_rbac3_sod_set_code UNIQUE (tenant_id, set_code);

ALTER TABLE ONLY rbac3_sod_set
    ADD CONSTRAINT uq_rbac3_sod_set_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY rbac3_tenant_application
    ADD CONSTRAINT uq_rbac3_tenant_application_tenant_app UNIQUE (tenant_id, application_id);

ALTER TABLE ONLY rbac3_tenant_application
    ADD CONSTRAINT uq_rbac3_tenant_application_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY rbac3_user_active_role
    ADD CONSTRAINT uq_rbac3_user_active_role_root UNIQUE (tenant_id, user_id, root_role_id);

ALTER TABLE ONLY rbac3_user_business_access
    ADD CONSTRAINT uq_rbac3_user_business_access_source UNIQUE (tenant_id, user_id, ddc_business_id, source_type, source_id);

ALTER TABLE ONLY rbac3_user_business_access
    ADD CONSTRAINT uq_rbac3_user_business_access_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY rbac3_user
    ADD CONSTRAINT uq_rbac3_user_identity_sub UNIQUE (tenant_id, identity_sub);

ALTER TABLE ONLY rbac3_user_org_assignment
    ADD CONSTRAINT uq_rbac3_user_org_assignment_source UNIQUE (tenant_id, user_id, org_unit_id, source_type, source_id);

ALTER TABLE ONLY rbac3_user_org_assignment
    ADD CONSTRAINT uq_rbac3_user_org_assignment_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY rbac3_user_position_assignment
    ADD CONSTRAINT uq_rbac3_user_position_assignment_source UNIQUE (tenant_id, user_id, org_unit_id, position_id, source_type, source_id);

ALTER TABLE ONLY rbac3_user_position_assignment
    ADD CONSTRAINT uq_rbac3_user_position_assignment_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY rbac3_user_position_snapshot
    ADD CONSTRAINT uq_rbac3_user_position_snapshot UNIQUE (tenant_id, snapshot_id, user_id, position_id, valid_from);

ALTER TABLE ONLY rbac3_user_position_snapshot
    ADD CONSTRAINT uq_rbac3_user_position_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE ONLY rbac3_user
    ADD CONSTRAINT uq_rbac3_user_tenant_id UNIQUE (tenant_id, id);

CREATE INDEX idx_rbac3_application_business_code ON rbac3_application USING btree (ddc_business_id, status);

CREATE INDEX idx_rbac3_assignment_role_active_window ON rbac3_user_role_assignment USING btree (tenant_id, role_id, status, valid_from, valid_to);

CREATE INDEX idx_rbac3_assignment_user_active_window ON rbac3_user_role_assignment USING btree (tenant_id, user_id, status, valid_from, valid_to);

CREATE INDEX idx_rbac3_audit_target_created ON rbac3_audit_log USING btree (tenant_id, target_type, target_id, created_at DESC);

CREATE INDEX idx_rbac3_audit_tenant_created ON rbac3_audit_log USING btree (tenant_id, created_at DESC);

CREATE INDEX idx_rbac3_audit_trace ON rbac3_audit_log USING btree (tenant_id, trace_id);

CREATE INDEX idx_rbac3_authorization_mutation_recovery ON rbac3_authorization_mutation USING btree (status, updated_at);

CREATE INDEX idx_rbac3_authorization_mutation_scope ON rbac3_authorization_mutation USING btree (tenant_id, user_id, status);

CREATE INDEX idx_rbac3_auto_assignment_match ON rbac3_auto_assignment_rule USING btree (tenant_id, status, match_type, match_ref_id);

CREATE INDEX idx_rbac3_closure_ancestor_depth ON rbac3_role_closure USING btree (tenant_id, application_id, ancestor_role_id, depth);

CREATE INDEX idx_rbac3_closure_descendant_depth ON rbac3_role_closure USING btree (tenant_id, application_id, descendant_role_id, depth);

CREATE INDEX idx_rbac3_data_rule_ref_lookup ON rbac3_data_rule_ref USING btree (tenant_id, ref_type, ref_id);

CREATE INDEX idx_rbac3_data_rule_role_permission ON rbac3_data_rule USING btree (tenant_id, role_id, permission_id, status);

CREATE INDEX idx_rbac3_directory_snapshot_active ON rbac3_directory_snapshot USING btree (tenant_id, provider_code, status, activated_at DESC);

CREATE INDEX idx_rbac3_field_definition_source ON rbac3_field_definition USING btree (application_id, source_type, status);

CREATE INDEX idx_rbac3_field_rule_role_permission ON rbac3_field_rule USING btree (tenant_id, role_id, permission_id, status);

CREATE INDEX idx_rbac3_idempotency_expiry ON rbac3_idempotency_record USING btree (expires_at);

CREATE INDEX idx_rbac3_idempotency_resource ON rbac3_idempotency_record USING btree (tenant_id, resource_type, resource_id);

CREATE INDEX idx_rbac3_management_operation_lookup ON rbac3_management_operation USING btree (tenant_id, operation_code);

CREATE INDEX idx_rbac3_management_policy_active ON rbac3_management_policy USING btree (tenant_id, status, valid_from, valid_to);

CREATE INDEX idx_rbac3_management_role_lookup ON rbac3_management_role USING btree (tenant_id, role_id);

CREATE INDEX idx_rbac3_management_scope_lookup ON rbac3_management_scope USING btree (tenant_id, scope_type, scope_ref_id);

CREATE INDEX idx_rbac3_management_subject_lookup ON rbac3_management_subject USING btree (tenant_id, subject_type, subject_id);

CREATE INDEX idx_rbac3_operation_sod_lookup ON rbac3_operation_sod_rule USING btree (tenant_id, application_id, business_resource, status);

CREATE INDEX idx_rbac3_org_unit_parent_status ON rbac3_org_unit USING btree (tenant_id, parent_id, status);

CREATE INDEX idx_rbac3_org_unit_path ON rbac3_org_unit USING btree (tenant_id, path);

CREATE INDEX idx_rbac3_participation_conflict ON rbac3_business_participation USING btree (tenant_id, application_id, business_resource, business_id, actor_user_id, action_code);

CREATE INDEX idx_rbac3_participation_occurred ON rbac3_business_participation USING btree (occurred_at);

CREATE INDEX idx_rbac3_permission_source ON rbac3_permission USING btree (application_id, source_type, status);

CREATE INDEX idx_rbac3_position_org_status ON rbac3_position USING btree (tenant_id, org_unit_id, status);

CREATE INDEX idx_rbac3_resource_api_binding_application_api_status ON rbac3_resource_api_binding USING btree (application_id, api_resource_id, status);

CREATE INDEX idx_rbac3_resource_api_binding_application_source_status ON rbac3_resource_api_binding USING btree (application_id, source_resource_id, status);

CREATE INDEX idx_rbac3_resource_source ON rbac3_resource USING btree (application_id, source_type, status, resource_type);

CREATE INDEX idx_rbac3_role_application_type_status ON rbac3_role USING btree (tenant_id, application_id, role_type, status);

CREATE INDEX idx_rbac3_role_cardinality_role ON rbac3_role_cardinality USING btree (tenant_id, role_id, status);

CREATE INDEX idx_rbac3_role_inheritance_junior ON rbac3_role_inheritance USING btree (tenant_id, application_id, junior_role_id);

CREATE INDEX idx_rbac3_role_prerequisite_target ON rbac3_role_prerequisite USING btree (tenant_id, target_role_id, status);

CREATE INDEX idx_rbac3_role_privileged_status ON rbac3_role USING btree (tenant_id, privileged, status);

CREATE INDEX idx_rbac3_role_resource_grant_resource_status ON rbac3_role_resource_grant USING btree (application_id, resource_id, status);

CREATE INDEX idx_rbac3_role_resource_grant_role_status_window ON rbac3_role_resource_grant USING btree (tenant_id, role_id, status, valid_from, valid_to);

CREATE INDEX idx_rbac3_service_permission_principal_window ON rbac3_service_permission USING btree (tenant_id, principal_id, valid_from, valid_to);

CREATE INDEX idx_rbac3_service_principal_application ON rbac3_service_principal USING btree (tenant_id, application_id, status);

CREATE INDEX idx_rbac3_sod_member_role ON rbac3_sod_member USING btree (tenant_id, role_id);

CREATE INDEX idx_rbac3_sod_set_lookup ON rbac3_sod_set USING btree (tenant_id, constraint_type, application_id, status);

CREATE INDEX idx_rbac3_tenant_application_status ON rbac3_tenant_application USING btree (tenant_id, status, valid_from, valid_to);

CREATE INDEX idx_rbac3_user_active_role_application ON rbac3_user_active_role USING btree (tenant_id, user_id, application_id);

CREATE INDEX idx_rbac3_user_active_role_root ON rbac3_user_active_role USING btree (tenant_id, root_role_id);

CREATE INDEX idx_rbac3_user_business_access_business ON rbac3_user_business_access USING btree (tenant_id, ddc_business_id, status);

CREATE INDEX idx_rbac3_user_business_access_user_window ON rbac3_user_business_access USING btree (tenant_id, user_id, status, valid_from, valid_to);

CREATE INDEX idx_rbac3_user_identity_lookup ON rbac3_user USING btree (identity_sub, tenant_id, status);

CREATE INDEX idx_rbac3_user_org_assignment_org_window ON rbac3_user_org_assignment USING btree (tenant_id, org_unit_id, status, valid_from, valid_to);

CREATE INDEX idx_rbac3_user_org_assignment_user_window ON rbac3_user_org_assignment USING btree (tenant_id, user_id, status, valid_from, valid_to);

CREATE INDEX idx_rbac3_user_position_assignment_org_window ON rbac3_user_position_assignment USING btree (tenant_id, org_unit_id, status, valid_from, valid_to);

CREATE INDEX idx_rbac3_user_position_assignment_position_window ON rbac3_user_position_assignment USING btree (tenant_id, position_id, status, valid_from, valid_to);

CREATE INDEX idx_rbac3_user_position_assignment_user_window ON rbac3_user_position_assignment USING btree (tenant_id, user_id, status, valid_from, valid_to);

CREATE INDEX idx_rbac3_user_position_position ON rbac3_user_position_snapshot USING btree (tenant_id, position_id, status);

CREATE INDEX idx_rbac3_user_position_user_window ON rbac3_user_position_snapshot USING btree (tenant_id, user_id, status, valid_from, valid_to);

CREATE INDEX idx_rbac3_user_tenant_status ON rbac3_user USING btree (tenant_id, status);

CREATE UNIQUE INDEX uk_rbac3_application_code_global ON rbac3_application USING btree (application_code);

CREATE UNIQUE INDEX uk_rbac3_application_ddc_application_global ON rbac3_application USING btree (ddc_application_id);

CREATE UNIQUE INDEX uk_rbac3_application_id_code ON rbac3_application USING btree (id, application_code);

CREATE UNIQUE INDEX uk_rbac3_field_definition_code_global ON rbac3_field_definition USING btree (application_id, resource_id, field_code);

CREATE UNIQUE INDEX uk_rbac3_management_scope_self_dept ON rbac3_management_scope USING btree (tenant_id, policy_id, scope_type) WHERE (scope_ref_id IS NULL);

CREATE UNIQUE INDEX uk_rbac3_permission_code_global ON rbac3_permission USING btree (permission_code);

CREATE UNIQUE INDEX uk_rbac3_resource_code_global ON rbac3_resource USING btree (application_id, resource_type, resource_code);

CREATE TRIGGER trg_rbac3_audit_log_append_only BEFORE DELETE OR UPDATE ON rbac3_audit_log FOR EACH ROW EXECUTE FUNCTION rbac3_reject_append_only_change();

CREATE TRIGGER trg_rbac3_business_participation_append_only BEFORE DELETE OR UPDATE ON rbac3_business_participation FOR EACH ROW EXECUTE FUNCTION rbac3_reject_append_only_change();

CREATE TRIGGER trg_rbac3_business_participation_application_identity BEFORE INSERT OR UPDATE OF application_id, application_code ON rbac3_business_participation FOR EACH ROW EXECUTE FUNCTION rbac3_sync_application_identity();

CREATE TRIGGER trg_rbac3_directory_snapshot_immutable BEFORE UPDATE ON rbac3_directory_snapshot FOR EACH ROW EXECUTE FUNCTION rbac3_reject_immutable_column_change('tenant_id', 'provider_code', 'snapshot_version', 'checksum', 'generated_at', 'payload');

CREATE TRIGGER trg_rbac3_operation_sod_application_identity BEFORE INSERT OR UPDATE OF application_id, application_code ON rbac3_operation_sod_rule FOR EACH ROW EXECUTE FUNCTION rbac3_sync_application_identity();

CREATE TRIGGER trg_rbac3_permission_code_immutable BEFORE UPDATE ON rbac3_permission FOR EACH ROW EXECUTE FUNCTION rbac3_reject_immutable_column_change('tenant_id', 'application_id', 'permission_code');

CREATE TRIGGER trg_rbac3_role_identity_immutable BEFORE UPDATE ON rbac3_role FOR EACH ROW EXECUTE FUNCTION rbac3_reject_immutable_column_change('tenant_id', 'application_id', 'role_code', 'role_type', 'privileged');

CREATE TRIGGER trg_rbac3_service_permission_application_identity BEFORE INSERT OR UPDATE OF application_id, application_code ON rbac3_service_permission FOR EACH ROW EXECUTE FUNCTION rbac3_sync_application_identity();

ALTER TABLE ONLY rbac3_user_role_assignment
    ADD CONSTRAINT fk_rbac3_assignment_role FOREIGN KEY (tenant_id, role_id) REFERENCES rbac3_role(tenant_id, id);

ALTER TABLE ONLY rbac3_user_role_assignment
    ADD CONSTRAINT fk_rbac3_assignment_user FOREIGN KEY (tenant_id, user_id) REFERENCES rbac3_user(tenant_id, id);

ALTER TABLE ONLY rbac3_audit_log
    ADD CONSTRAINT fk_rbac3_audit_management_policy FOREIGN KEY (tenant_id, management_policy_id) REFERENCES rbac3_management_policy(tenant_id, id);

ALTER TABLE ONLY rbac3_audit_log
    ADD CONSTRAINT fk_rbac3_audit_tenant FOREIGN KEY (tenant_id) REFERENCES rbac3_tenant_authorization_state(tenant_id);

ALTER TABLE ONLY rbac3_authorization_mutation
    ADD CONSTRAINT fk_rbac3_authorization_mutation_tenant FOREIGN KEY (tenant_id) REFERENCES rbac3_tenant_authorization_state(tenant_id);

ALTER TABLE ONLY rbac3_authorization_mutation
    ADD CONSTRAINT fk_rbac3_authorization_mutation_user FOREIGN KEY (tenant_id, user_id) REFERENCES rbac3_user(tenant_id, id);

ALTER TABLE ONLY rbac3_auto_assignment_rule
    ADD CONSTRAINT fk_rbac3_auto_assignment_position FOREIGN KEY (tenant_id, match_ref_id) REFERENCES rbac3_position(tenant_id, id);

ALTER TABLE ONLY rbac3_auto_assignment_rule
    ADD CONSTRAINT fk_rbac3_auto_assignment_role FOREIGN KEY (tenant_id, role_id) REFERENCES rbac3_role(tenant_id, id);

ALTER TABLE ONLY rbac3_business_participation
    ADD CONSTRAINT fk_rbac3_business_participation_actor FOREIGN KEY (tenant_id, actor_user_id) REFERENCES rbac3_user(tenant_id, id);

ALTER TABLE ONLY rbac3_business_participation
    ADD CONSTRAINT fk_rbac3_business_participation_application_code FOREIGN KEY (application_id, application_code) REFERENCES rbac3_application(id, application_code);

ALTER TABLE ONLY rbac3_business_participation
    ADD CONSTRAINT fk_rbac3_business_participation_application_global FOREIGN KEY (application_id) REFERENCES rbac3_application(id);

ALTER TABLE ONLY rbac3_data_rule
    ADD CONSTRAINT fk_rbac3_data_rule_permission_global FOREIGN KEY (permission_id) REFERENCES rbac3_permission(id);

ALTER TABLE ONLY rbac3_data_rule_ref
    ADD CONSTRAINT fk_rbac3_data_rule_ref_rule FOREIGN KEY (tenant_id, data_rule_id) REFERENCES rbac3_data_rule(tenant_id, id);

ALTER TABLE ONLY rbac3_data_rule
    ADD CONSTRAINT fk_rbac3_data_rule_role FOREIGN KEY (tenant_id, application_id, role_id) REFERENCES rbac3_role(tenant_id, application_id, id);

ALTER TABLE ONLY rbac3_directory_snapshot
    ADD CONSTRAINT fk_rbac3_directory_snapshot_tenant FOREIGN KEY (tenant_id) REFERENCES rbac3_tenant_authorization_state(tenant_id);

ALTER TABLE ONLY rbac3_field_definition
    ADD CONSTRAINT fk_rbac3_field_definition_application_global FOREIGN KEY (application_id) REFERENCES rbac3_application(id);

ALTER TABLE ONLY rbac3_field_definition
    ADD CONSTRAINT fk_rbac3_field_definition_resource_global FOREIGN KEY (resource_id) REFERENCES rbac3_resource(id);

ALTER TABLE ONLY rbac3_field_rule
    ADD CONSTRAINT fk_rbac3_field_rule_definition_global FOREIGN KEY (field_definition_id) REFERENCES rbac3_field_definition(id);

ALTER TABLE ONLY rbac3_field_rule
    ADD CONSTRAINT fk_rbac3_field_rule_permission_global FOREIGN KEY (permission_id) REFERENCES rbac3_permission(id);

ALTER TABLE ONLY rbac3_field_rule
    ADD CONSTRAINT fk_rbac3_field_rule_role FOREIGN KEY (tenant_id, application_id, role_id) REFERENCES rbac3_role(tenant_id, application_id, id);

ALTER TABLE ONLY rbac3_idempotency_record
    ADD CONSTRAINT fk_rbac3_idempotency_tenant FOREIGN KEY (tenant_id) REFERENCES rbac3_tenant_authorization_state(tenant_id);

ALTER TABLE ONLY rbac3_management_operation
    ADD CONSTRAINT fk_rbac3_management_operation_policy FOREIGN KEY (tenant_id, policy_id) REFERENCES rbac3_management_policy(tenant_id, id);

ALTER TABLE ONLY rbac3_management_policy
    ADD CONSTRAINT fk_rbac3_management_policy_tenant FOREIGN KEY (tenant_id) REFERENCES rbac3_tenant_authorization_state(tenant_id);

ALTER TABLE ONLY rbac3_management_role
    ADD CONSTRAINT fk_rbac3_management_role_policy FOREIGN KEY (tenant_id, policy_id) REFERENCES rbac3_management_policy(tenant_id, id);

ALTER TABLE ONLY rbac3_management_role
    ADD CONSTRAINT fk_rbac3_management_role_role FOREIGN KEY (tenant_id, role_id) REFERENCES rbac3_role(tenant_id, id);

ALTER TABLE ONLY rbac3_management_scope
    ADD CONSTRAINT fk_rbac3_management_scope_policy FOREIGN KEY (tenant_id, policy_id) REFERENCES rbac3_management_policy(tenant_id, id);

ALTER TABLE ONLY rbac3_management_subject
    ADD CONSTRAINT fk_rbac3_management_subject_policy FOREIGN KEY (tenant_id, policy_id) REFERENCES rbac3_management_policy(tenant_id, id);

ALTER TABLE ONLY rbac3_operation_sod_rule
    ADD CONSTRAINT fk_rbac3_operation_sod_application_code FOREIGN KEY (application_id, application_code) REFERENCES rbac3_application(id, application_code);

ALTER TABLE ONLY rbac3_operation_sod_rule
    ADD CONSTRAINT fk_rbac3_operation_sod_application_global FOREIGN KEY (application_id) REFERENCES rbac3_application(id);

ALTER TABLE ONLY rbac3_org_unit
    ADD CONSTRAINT fk_rbac3_org_unit_parent FOREIGN KEY (tenant_id, parent_id) REFERENCES rbac3_org_unit(tenant_id, id);

ALTER TABLE ONLY rbac3_org_unit
    ADD CONSTRAINT fk_rbac3_org_unit_snapshot FOREIGN KEY (tenant_id, snapshot_id) REFERENCES rbac3_directory_snapshot(tenant_id, id);

ALTER TABLE ONLY rbac3_permission
    ADD CONSTRAINT fk_rbac3_permission_application_global FOREIGN KEY (application_id) REFERENCES rbac3_application(id);

ALTER TABLE ONLY rbac3_position
    ADD CONSTRAINT fk_rbac3_position_org_unit FOREIGN KEY (tenant_id, org_unit_id) REFERENCES rbac3_org_unit(tenant_id, id);

ALTER TABLE ONLY rbac3_position
    ADD CONSTRAINT fk_rbac3_position_snapshot FOREIGN KEY (tenant_id, snapshot_id) REFERENCES rbac3_directory_snapshot(tenant_id, id);

ALTER TABLE ONLY rbac3_resource_api_binding
    ADD CONSTRAINT fk_rbac3_resource_api_binding_api FOREIGN KEY (application_id, api_resource_id) REFERENCES rbac3_resource(application_id, id);

ALTER TABLE ONLY rbac3_resource_api_binding
    ADD CONSTRAINT fk_rbac3_resource_api_binding_source FOREIGN KEY (application_id, source_resource_id) REFERENCES rbac3_resource(application_id, id);

ALTER TABLE ONLY rbac3_resource
    ADD CONSTRAINT fk_rbac3_resource_application_global FOREIGN KEY (application_id) REFERENCES rbac3_application(id);

ALTER TABLE ONLY rbac3_resource
    ADD CONSTRAINT fk_rbac3_resource_parent_global FOREIGN KEY (parent_resource_id) REFERENCES rbac3_resource(id);

ALTER TABLE ONLY rbac3_resource
    ADD CONSTRAINT fk_rbac3_resource_required_permission_global FOREIGN KEY (required_permission_id) REFERENCES rbac3_permission(id);

ALTER TABLE ONLY rbac3_role
    ADD CONSTRAINT fk_rbac3_role_application_global FOREIGN KEY (application_id) REFERENCES rbac3_application(id);

ALTER TABLE ONLY rbac3_role_cardinality
    ADD CONSTRAINT fk_rbac3_role_cardinality_role FOREIGN KEY (tenant_id, role_id) REFERENCES rbac3_role(tenant_id, id);

ALTER TABLE ONLY rbac3_role_closure
    ADD CONSTRAINT fk_rbac3_role_closure_ancestor FOREIGN KEY (tenant_id, application_id, ancestor_role_id) REFERENCES rbac3_role(tenant_id, application_id, id);

ALTER TABLE ONLY rbac3_role_closure
    ADD CONSTRAINT fk_rbac3_role_closure_descendant FOREIGN KEY (tenant_id, application_id, descendant_role_id) REFERENCES rbac3_role(tenant_id, application_id, id);

ALTER TABLE ONLY rbac3_role_inheritance
    ADD CONSTRAINT fk_rbac3_role_inheritance_junior FOREIGN KEY (tenant_id, application_id, junior_role_id) REFERENCES rbac3_role(tenant_id, application_id, id);

ALTER TABLE ONLY rbac3_role_inheritance
    ADD CONSTRAINT fk_rbac3_role_inheritance_senior FOREIGN KEY (tenant_id, application_id, senior_role_id) REFERENCES rbac3_role(tenant_id, application_id, id);

ALTER TABLE ONLY rbac3_role
    ADD CONSTRAINT fk_rbac3_role_landing_route_global FOREIGN KEY (landing_route_id) REFERENCES rbac3_resource(id);

ALTER TABLE ONLY rbac3_role_prerequisite
    ADD CONSTRAINT fk_rbac3_role_prerequisite_required FOREIGN KEY (tenant_id, prerequisite_role_id) REFERENCES rbac3_role(tenant_id, id);

ALTER TABLE ONLY rbac3_role_prerequisite
    ADD CONSTRAINT fk_rbac3_role_prerequisite_target FOREIGN KEY (tenant_id, target_role_id) REFERENCES rbac3_role(tenant_id, id);

ALTER TABLE ONLY rbac3_role_resource_grant
    ADD CONSTRAINT fk_rbac3_role_resource_grant_resource FOREIGN KEY (application_id, resource_id) REFERENCES rbac3_resource(application_id, id);

ALTER TABLE ONLY rbac3_role_resource_grant
    ADD CONSTRAINT fk_rbac3_role_resource_grant_role FOREIGN KEY (tenant_id, application_id, role_id) REFERENCES rbac3_role(tenant_id, application_id, id);

ALTER TABLE ONLY rbac3_service_permission
    ADD CONSTRAINT fk_rbac3_service_permission_application_code FOREIGN KEY (application_id, application_code) REFERENCES rbac3_application(id, application_code);

ALTER TABLE ONLY rbac3_service_permission
    ADD CONSTRAINT fk_rbac3_service_permission_application_global FOREIGN KEY (application_id) REFERENCES rbac3_application(id);

ALTER TABLE ONLY rbac3_service_permission
    ADD CONSTRAINT fk_rbac3_service_permission_permission_global FOREIGN KEY (permission_id) REFERENCES rbac3_permission(id);

ALTER TABLE ONLY rbac3_service_permission
    ADD CONSTRAINT fk_rbac3_service_permission_principal FOREIGN KEY (tenant_id, application_id, principal_id) REFERENCES rbac3_service_principal(tenant_id, application_id, id);

ALTER TABLE ONLY rbac3_service_principal
    ADD CONSTRAINT fk_rbac3_service_principal_application_global FOREIGN KEY (application_id) REFERENCES rbac3_application(id);

ALTER TABLE ONLY rbac3_service_principal
    ADD CONSTRAINT fk_rbac3_service_principal_tenant FOREIGN KEY (tenant_id) REFERENCES rbac3_tenant_authorization_state(tenant_id);

ALTER TABLE ONLY rbac3_sod_member
    ADD CONSTRAINT fk_rbac3_sod_member_role FOREIGN KEY (tenant_id, role_id) REFERENCES rbac3_role(tenant_id, id);

ALTER TABLE ONLY rbac3_sod_member
    ADD CONSTRAINT fk_rbac3_sod_member_set FOREIGN KEY (tenant_id, sod_set_id) REFERENCES rbac3_sod_set(tenant_id, id);

ALTER TABLE ONLY rbac3_sod_set
    ADD CONSTRAINT fk_rbac3_sod_set_application_global FOREIGN KEY (application_id) REFERENCES rbac3_application(id);

ALTER TABLE ONLY rbac3_tenant_application
    ADD CONSTRAINT fk_rbac3_tenant_application_application FOREIGN KEY (application_id) REFERENCES rbac3_application(id);

ALTER TABLE ONLY rbac3_tenant_application
    ADD CONSTRAINT fk_rbac3_tenant_application_tenant FOREIGN KEY (tenant_id) REFERENCES rbac3_tenant_authorization_state(tenant_id);

ALTER TABLE ONLY rbac3_user_active_role
    ADD CONSTRAINT fk_rbac3_user_active_role_root FOREIGN KEY (tenant_id, application_id, root_role_id) REFERENCES rbac3_role(tenant_id, application_id, id);

ALTER TABLE ONLY rbac3_user_active_role
    ADD CONSTRAINT fk_rbac3_user_active_role_user FOREIGN KEY (tenant_id, user_id) REFERENCES rbac3_user(tenant_id, id);

ALTER TABLE ONLY rbac3_user_business_access
    ADD CONSTRAINT fk_rbac3_user_business_access_user FOREIGN KEY (tenant_id, user_id) REFERENCES rbac3_user(tenant_id, id);

ALTER TABLE ONLY rbac3_user_org_assignment
    ADD CONSTRAINT fk_rbac3_user_org_assignment_org FOREIGN KEY (tenant_id, org_unit_id) REFERENCES rbac3_org_unit(tenant_id, id);

ALTER TABLE ONLY rbac3_user_org_assignment
    ADD CONSTRAINT fk_rbac3_user_org_assignment_user FOREIGN KEY (tenant_id, user_id) REFERENCES rbac3_user(tenant_id, id);

ALTER TABLE ONLY rbac3_user_position_assignment
    ADD CONSTRAINT fk_rbac3_user_position_assignment_org FOREIGN KEY (tenant_id, org_unit_id) REFERENCES rbac3_org_unit(tenant_id, id);

ALTER TABLE ONLY rbac3_user_position_assignment
    ADD CONSTRAINT fk_rbac3_user_position_assignment_position FOREIGN KEY (tenant_id, position_id) REFERENCES rbac3_position(tenant_id, id);

ALTER TABLE ONLY rbac3_user_position_assignment
    ADD CONSTRAINT fk_rbac3_user_position_assignment_user FOREIGN KEY (tenant_id, user_id) REFERENCES rbac3_user(tenant_id, id);

ALTER TABLE ONLY rbac3_user_position_snapshot
    ADD CONSTRAINT fk_rbac3_user_position_org FOREIGN KEY (tenant_id, org_unit_id) REFERENCES rbac3_org_unit(tenant_id, id);

ALTER TABLE ONLY rbac3_user_position_snapshot
    ADD CONSTRAINT fk_rbac3_user_position_position FOREIGN KEY (tenant_id, position_id) REFERENCES rbac3_position(tenant_id, id);

ALTER TABLE ONLY rbac3_user_position_snapshot
    ADD CONSTRAINT fk_rbac3_user_position_snapshot FOREIGN KEY (tenant_id, snapshot_id) REFERENCES rbac3_directory_snapshot(tenant_id, id);

ALTER TABLE ONLY rbac3_user_position_snapshot
    ADD CONSTRAINT fk_rbac3_user_position_user FOREIGN KEY (tenant_id, user_id) REFERENCES rbac3_user(tenant_id, id);

ALTER TABLE ONLY rbac3_user
    ADD CONSTRAINT fk_rbac3_user_tenant FOREIGN KEY (tenant_id) REFERENCES rbac3_tenant_authorization_state(tenant_id);

/* Built-in application and permission catalog; local authority is opt-in. */
CREATE TEMPORARY TABLE rbac3_builtin_application_seed (
    id BIGINT PRIMARY KEY,
    application_code VARCHAR(128) NOT NULL,
    application_name VARCHAR(200) NOT NULL,
    ddc_application_id VARCHAR(64) NOT NULL,
    ddc_business_id VARCHAR(64) NOT NULL,
    display_priority INTEGER NOT NULL
) ON COMMIT DROP;

INSERT INTO rbac3_builtin_application_seed (
    id, application_code, application_name,
    ddc_application_id, ddc_business_id, display_priority
) VALUES
    (8000000000000000001, 'rbac3-admin', 'RBAC3 Administration',
        'rbac3', 'permission', 0),
    (8000000000000000002, 'idp-admin', 'Identity Platform Administration',
        'idp', 'identity', 10),
    (8000000000000000003, 'gateway-admin', 'Gateway Administration',
        'gateway-admin', 'platform', 20),
    (8000000000000000004, 'ddc-admin', 'Dynamic Configuration Administration',
        'ddc', 'platform', 30),
    (8000000000000000005, 'mock-backend', 'Unified Identity Mock Backend',
        'mock-backend', 'platform', 40);

CREATE TEMPORARY TABLE rbac3_builtin_role_seed (
    application_code VARCHAR(128) NOT NULL,
    role_code VARCHAR(128) NOT NULL,
    role_name VARCHAR(200) NOT NULL,
    display_priority INTEGER NOT NULL,
    PRIMARY KEY (application_code, role_code)
) ON COMMIT DROP;

INSERT INTO rbac3_builtin_role_seed (
    application_code, role_code, role_name, display_priority
) VALUES
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'RBAC3 Administration Administrator', 0),
    ('idp-admin', 'IDP_LOCAL_ADMIN', 'Identity Platform Administration Administrator', 10),
    ('gateway-admin', 'GATEWAY_LOCAL_ADMIN', 'Gateway Administration Administrator', 20),
    ('ddc-admin', 'DDC_LOCAL_ADMIN', 'Dynamic Configuration Administration Administrator', 30),
    ('mock-backend', 'MOCK_LOCAL_ADMIN', 'Unified Identity Mock Backend Administrator', 40),
    ('mock-backend', 'MOCK_LOCAL_ENTRY', 'Unified Identity Mock Backend Entry', 41);

CREATE TEMPORARY TABLE rbac3_builtin_role_permission_seed (
    application_code VARCHAR(128) NOT NULL,
    role_code VARCHAR(128) NOT NULL,
    permission_code VARCHAR(128) NOT NULL,
    PRIMARY KEY (role_code, permission_code)
) ON COMMIT DROP;

INSERT INTO rbac3_builtin_role_permission_seed (
    application_code, role_code, permission_code
) VALUES
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:application:read'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:audit:read'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:authorization-constraint:manage'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:authorization-constraint:read'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:authorization-runtime:operate'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:authorization-runtime:read'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:authorization-simulation:execute'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:bootstrap:read'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:data-rule:manage'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:data-rule:read'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:directory-snapshot:read'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:directory:read'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:directory:sync'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:field-rule:manage'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:field-rule:read'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:management-policy:manage'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:management-policy:read'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:operation-sod:manage'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:operation-sod:read'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:resource-manifest:activate'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:resource-manifest:read'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:resource-manifest:submit'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:resource:archive'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:resource:read'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:role-activation:read'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:role-activation:use'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:role-assignment:manage'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:role-assignment:read'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:role-inheritance:manage'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:role-permission:manage'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:role:create'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:role:read'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:role:update'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:tenant:target'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:user-status:manage'),
    ('rbac3-admin', 'RBAC3_LOCAL_ADMIN', 'system:user:read'),
    ('idp-admin', 'IDP_LOCAL_ADMIN', 'idp:audit:read'),
    ('idp-admin', 'IDP_LOCAL_ADMIN', 'idp:bootstrap:read'),
    ('idp-admin', 'IDP_LOCAL_ADMIN', 'idp:identity:self:read'),
    ('idp-admin', 'IDP_LOCAL_ADMIN', 'idp:identity-user:create'),
    ('idp-admin', 'IDP_LOCAL_ADMIN', 'idp:identity-user:password-reset'),
    ('idp-admin', 'IDP_LOCAL_ADMIN', 'idp:identity-user:read'),
    ('idp-admin', 'IDP_LOCAL_ADMIN', 'idp:identity-user:revoke-all'),
    ('idp-admin', 'IDP_LOCAL_ADMIN', 'idp:identity-user:update'),
    ('idp-admin', 'IDP_LOCAL_ADMIN', 'idp:oauth-client:create'),
    ('idp-admin', 'IDP_LOCAL_ADMIN', 'idp:oauth-client:read'),
    ('idp-admin', 'IDP_LOCAL_ADMIN', 'idp:oauth-client:update'),
    ('idp-admin', 'IDP_LOCAL_ADMIN', 'idp:tenant:manage'),
    ('idp-admin', 'IDP_LOCAL_ADMIN', 'idp:tenant:read'),
    ('idp-admin', 'IDP_LOCAL_ADMIN', 'idp:resource-server:create'),
    ('idp-admin', 'IDP_LOCAL_ADMIN', 'idp:resource-server:grant'),
    ('idp-admin', 'IDP_LOCAL_ADMIN', 'idp:resource-server:key'),
    ('idp-admin', 'IDP_LOCAL_ADMIN', 'idp:resource-server:read'),
    ('idp-admin', 'IDP_LOCAL_ADMIN', 'idp:resource-server:status'),
    ('idp-admin', 'IDP_LOCAL_ADMIN', 'idp:resource-server:update'),
    ('idp-admin', 'IDP_LOCAL_ADMIN', 'idp:signing-key:activate'),
    ('idp-admin', 'IDP_LOCAL_ADMIN', 'idp:signing-key:publish'),
    ('idp-admin', 'IDP_LOCAL_ADMIN', 'idp:signing-key:read'),
    ('idp-admin', 'IDP_LOCAL_ADMIN', 'idp:signing-key:retire'),
    ('gateway-admin', 'GATEWAY_LOCAL_ADMIN', 'gateway:read'),
    ('gateway-admin', 'GATEWAY_LOCAL_ADMIN', 'gateway:applications:write'),
    ('gateway-admin', 'GATEWAY_LOCAL_ADMIN', 'gateway:catalog:write'),
    ('gateway-admin', 'GATEWAY_LOCAL_ADMIN', 'gateway:credentials:write'),
    ('gateway-admin', 'GATEWAY_LOCAL_ADMIN', 'gateway:drafts:write'),
    ('gateway-admin', 'GATEWAY_LOCAL_ADMIN', 'gateway:groups:write'),
    ('gateway-admin', 'GATEWAY_LOCAL_ADMIN', 'gateway:mcp:approve'),
    ('gateway-admin', 'GATEWAY_LOCAL_ADMIN', 'gateway:mcp:read'),
    ('gateway-admin', 'GATEWAY_LOCAL_ADMIN', 'gateway:mcp:runtime:read'),
    ('gateway-admin', 'GATEWAY_LOCAL_ADMIN', 'gateway:mcp:test'),
    ('gateway-admin', 'GATEWAY_LOCAL_ADMIN', 'gateway:mcp:write'),
    ('gateway-admin', 'GATEWAY_LOCAL_ADMIN', 'gateway:releases:write'),
    ('ddc-admin', 'DDC_LOCAL_ADMIN', 'DDC_READ'),
    ('ddc-admin', 'DDC_LOCAL_ADMIN', 'DDC_WRITE'),
    ('ddc-admin', 'DDC_LOCAL_ADMIN', 'DDC_PUBLISH'),
    ('ddc-admin', 'DDC_LOCAL_ADMIN', 'DDC_CACHE'),
    ('mock-backend', 'MOCK_LOCAL_ADMIN', 'mock:read'),
    ('mock-backend', 'MOCK_LOCAL_ADMIN', 'mock:admin'),
    ('mock-backend', 'MOCK_LOCAL_ADMIN', 'mcp:unified-local:tool:local_query:call'),
    ('mock-backend', 'MOCK_LOCAL_ADMIN', 'mcp:unified-local:tool:local_echo_task:call'),
    ('mock-backend', 'MOCK_LOCAL_ADMIN', 'mcp:unified-local:tool:local_echo_task:task:get'),
    ('mock-backend', 'MOCK_LOCAL_ADMIN', 'mcp:unified-local:tool:local_echo_task:task:update'),
    ('mock-backend', 'MOCK_LOCAL_ADMIN', 'mcp:unified-local:tool:local_echo_task:task:cancel'),
    ('mock-backend', 'MOCK_LOCAL_ADMIN', 'mcp:unified-local:tool:high_risk_query:call'),
    ('mock-backend', 'MOCK_LOCAL_ADMIN', 'mcp:unified-local:tool:stable.remote_echo:call'),
    ('mock-backend', 'MOCK_LOCAL_ADMIN', 'mcp:unified-local:tool:rc.remote_echo:call'),
    ('mock-backend', 'MOCK_LOCAL_ADMIN', 'mcp:unified-local:resource:local_status:read'),
    ('mock-backend', 'MOCK_LOCAL_ADMIN', 'mcp:unified-local:resource:stable.remote_text:read'),
    ('mock-backend', 'MOCK_LOCAL_ADMIN', 'mcp:unified-local:resource:local_item:read'),
    ('mock-backend', 'MOCK_LOCAL_ADMIN', 'mcp:unified-local:resource:qa_dashboard:read'),
    ('mock-backend', 'MOCK_LOCAL_ADMIN', 'mcp:unified-local:prompt:review_item:get'),
    ('mock-backend', 'MOCK_LOCAL_ADMIN', 'mcp:unified-local:prompt:rc.remote_summary:get'),
    ('mock-backend', 'MOCK_LOCAL_ENTRY', 'mock:read');

INSERT INTO rbac3_application (
    id, application_code, application_name, display_priority,
    status, version, created_at, created_by, updated_at, updated_by,
    ddc_application_id, ddc_business_id
)
SELECT
    seed.id, seed.application_code, seed.application_name,
    seed.display_priority, 'ACTIVE', 0, CURRENT_TIMESTAMP,
    'flyway-v10', CURRENT_TIMESTAMP, 'flyway-v10',
    seed.ddc_application_id, seed.ddc_business_id
FROM rbac3_builtin_application_seed seed
ON CONFLICT DO NOTHING;

INSERT INTO rbac3_permission (
    id, application_id, permission_code, permission_name,
    risk_level, status, description, version,
    created_at, created_by, updated_at, updated_by, source_type
)
SELECT
    8100000000000000000
        + row_number() OVER (ORDER BY seed.permission_code),
    application.id,
    seed.permission_code,
    seed.permission_code,
    CASE
        WHEN seed.permission_code = 'DDC_READ'
                OR seed.permission_code LIKE '%:read' THEN 'MEDIUM'
        WHEN seed.permission_code LIKE '%:admin'
                OR seed.permission_code LIKE '%:manage'
                OR seed.permission_code LIKE '%:activate'
                OR seed.permission_code LIKE '%:revoke' THEN 'CRITICAL'
        ELSE 'HIGH'
    END,
    'ACTIVE',
    'Built-in local platform capability',
    0, CURRENT_TIMESTAMP, 'flyway-v10',
    CURRENT_TIMESTAMP, 'flyway-v10', 'MANUAL'
FROM (
    SELECT DISTINCT application_code, permission_code
    FROM rbac3_builtin_role_permission_seed
) seed
JOIN rbac3_application application
  ON application.application_code = seed.application_code
ON CONFLICT DO NOTHING;

CREATE TEMPORARY TABLE rbac3_builtin_tenant_seed (
    tenant_id BIGINT PRIMARY KEY,
    identity_sub VARCHAR(200)
) ON COMMIT DROP;

DO $$
DECLARE
    tenant_ids TEXT := current_setting(
        'rbac3.bootstrap.tenant_ids', true);
    identity_sub TEXT := current_setting(
        'rbac3.bootstrap.identity_sub', true);
    tenant_value TEXT;
BEGIN
    IF tenant_ids IS NULL OR btrim(tenant_ids) = '' THEN
        RETURN;
    END IF;
    IF identity_sub IS NOT NULL AND (
            btrim(identity_sub) = ''
            OR length(identity_sub) > 200
            OR identity_sub <> btrim(identity_sub)) THEN
        RAISE EXCEPTION 'RBAC3 bootstrap identity subject is invalid';
    END IF;
    FOREACH tenant_value IN ARRAY regexp_split_to_array(tenant_ids, ',')
    LOOP
        IF tenant_value !~ '^[1-9][0-9]{0,18}$' THEN
            RAISE EXCEPTION 'RBAC3 bootstrap tenant id is invalid';
        END IF;
        INSERT INTO rbac3_builtin_tenant_seed (tenant_id, identity_sub)
        VALUES (tenant_value::BIGINT, identity_sub)
        ON CONFLICT DO NOTHING;
    END LOOP;
END
$$;

INSERT INTO rbac3_tenant_authorization_state (
    tenant_id, policy_version, version,
    created_at, created_by, updated_at, updated_by
)
SELECT
    tenant.tenant_id, 1, 0,
    CURRENT_TIMESTAMP, 'flyway-v10', CURRENT_TIMESTAMP, 'flyway-v10'
FROM rbac3_builtin_tenant_seed tenant
ON CONFLICT DO NOTHING;

INSERT INTO rbac3_tenant_application (
    id, tenant_id, application_id, status,
    valid_from, valid_to, source_type, source_id, reason, ticket_no,
    version, created_at, created_by, updated_at, updated_by
)
SELECT
    8200000000000000000
        + row_number() OVER (ORDER BY tenant.tenant_id, application.id),
    tenant.tenant_id, application.id, 'ACTIVE',
    CURRENT_TIMESTAMP, NULL, 'SYSTEM', 'flyway-v10',
    'Built-in local platform application', NULL,
    0, CURRENT_TIMESTAMP, 'flyway-v10', CURRENT_TIMESTAMP, 'flyway-v10'
FROM rbac3_builtin_tenant_seed tenant
CROSS JOIN rbac3_builtin_application_seed seed
JOIN rbac3_application application
  ON application.application_code = seed.application_code
ON CONFLICT DO NOTHING;

INSERT INTO rbac3_role (
    id, tenant_id, application_id, role_code, role_name,
    role_type, risk_level, privileged, status,
    landing_route_id, landing_priority, max_assignment_days,
    version, created_at, created_by, updated_at, updated_by
)
SELECT
    8300000000000000000
        + row_number() OVER (
            ORDER BY tenant.tenant_id, role.application_code, role.role_code),
    tenant.tenant_id, application.id,
    role.role_code, role.role_name,
    'MANAGEMENT', 'MEDIUM', FALSE, 'ACTIVE',
    NULL, role.display_priority, NULL,
    0, CURRENT_TIMESTAMP, 'flyway-v10', CURRENT_TIMESTAMP, 'flyway-v10'
FROM rbac3_builtin_tenant_seed tenant
CROSS JOIN rbac3_builtin_role_seed role
JOIN rbac3_application application
  ON application.application_code = role.application_code
ON CONFLICT DO NOTHING;

INSERT INTO rbac3_role_closure (
    tenant_id, application_id, ancestor_role_id, descendant_role_id, depth
)
SELECT
    role.tenant_id, role.application_id, role.id, role.id, 0
FROM rbac3_role role
JOIN rbac3_builtin_role_seed seed
  ON seed.role_code = role.role_code
 AND seed.application_code = (
        SELECT application.application_code
        FROM rbac3_application application
        WHERE application.id = role.application_id)
JOIN rbac3_builtin_tenant_seed tenant
  ON tenant.tenant_id = role.tenant_id
ON CONFLICT DO NOTHING;

INSERT INTO rbac3_user (
    id, tenant_id, status, auth_version, version,
    created_at, created_by, updated_at, updated_by, identity_sub
)
SELECT
    8500000000000000000
        + row_number() OVER (ORDER BY tenant.tenant_id),
    tenant.tenant_id, 'ACTIVE', 1, 0,
    CURRENT_TIMESTAMP, 'flyway-v10', CURRENT_TIMESTAMP, 'flyway-v10',
    tenant.identity_sub
FROM rbac3_builtin_tenant_seed tenant
WHERE tenant.identity_sub IS NOT NULL
ON CONFLICT DO NOTHING;

INSERT INTO rbac3_user_role_assignment (
    id, tenant_id, user_id, role_id,
    assignment_type, status, valid_from, valid_to,
    source_type, source_id, reason, ticket_no,
    version, created_at, created_by, updated_at, updated_by
)
SELECT
    8600000000000000000
        + row_number() OVER (ORDER BY user_row.tenant_id, role.id),
    user_row.tenant_id, user_row.id, role.id,
    'DIRECT', 'ACTIVE', CURRENT_TIMESTAMP, NULL,
    'DEVELOPMENT', 'flyway-v10',
    'Built-in local administrator assignment', NULL,
    0, CURRENT_TIMESTAMP, 'flyway-v10', CURRENT_TIMESTAMP, 'flyway-v10'
FROM rbac3_user user_row
JOIN rbac3_builtin_tenant_seed tenant
  ON tenant.tenant_id = user_row.tenant_id
 AND tenant.identity_sub = user_row.identity_sub
JOIN rbac3_role role
  ON role.tenant_id = user_row.tenant_id
JOIN rbac3_builtin_role_seed seed
  ON seed.role_code = role.role_code
ON CONFLICT DO NOTHING;

INSERT INTO rbac3_user_business_access (
    id, tenant_id, user_id, ddc_business_id,
    status, valid_from, valid_to,
    source_type, source_id, reason, ticket_no,
    version, created_at, created_by, updated_at, updated_by
)
SELECT
    8700000000000000000
        + row_number() OVER (
            ORDER BY seed.tenant_id, seed.user_id, seed.ddc_business_id),
    seed.tenant_id,
    seed.user_id,
    seed.ddc_business_id,
    'ACTIVE', CURRENT_TIMESTAMP, NULL,
    'SYSTEM', 'flyway-v11:' || seed.ddc_business_id,
    'Built-in local platform Business access', NULL,
    0, CURRENT_TIMESTAMP, 'flyway-v11',
    CURRENT_TIMESTAMP, 'flyway-v11'
FROM (
    SELECT DISTINCT
        assignment.tenant_id,
        assignment.user_id,
        CASE
            WHEN application.application_code = 'mock-backend'
                THEN 'identity'
            ELSE application.ddc_business_id
        END AS ddc_business_id
    FROM rbac3_user_role_assignment assignment
    JOIN rbac3_role role
      ON role.tenant_id = assignment.tenant_id
     AND role.id = assignment.role_id
    JOIN rbac3_application application
      ON application.id = role.application_id
    WHERE assignment.source_type = 'DEVELOPMENT'
      AND assignment.source_id = 'flyway-v10'
) seed
ON CONFLICT DO NOTHING;

INSERT INTO rbac3_permission (
    id, application_id, permission_code, permission_name,
    risk_level, status, description, version,
    created_at, created_by, updated_at, updated_by, source_type
)
SELECT
    8110000000000000001,
    application.id,
    'system:about:read',
    'system:about:read',
    'MEDIUM',
    'ACTIVE',
    'Read the current RBAC3 authorization context for the administration web client',
    0,
    CURRENT_TIMESTAMP,
    'flyway-v12',
    CURRENT_TIMESTAMP,
    'flyway-v12',
    'MANUAL'
FROM rbac3_application application
WHERE application.application_code = 'rbac3-admin'
ON CONFLICT DO NOTHING;
