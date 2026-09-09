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

INSERT INTO rbac3_role_permission (
    id, tenant_id, application_id, role_id, permission_id,
    valid_from, valid_to, status, version,
    created_at, created_by, updated_at, updated_by
)
SELECT
    8400000000000000000
        + row_number() OVER (
            ORDER BY role.tenant_id, role.id, permission.id),
    role.tenant_id, role.application_id, role.id, permission.id,
    CURRENT_TIMESTAMP, NULL, 'ACTIVE', 0,
    CURRENT_TIMESTAMP, 'flyway-v10', CURRENT_TIMESTAMP, 'flyway-v10'
FROM rbac3_builtin_role_permission_seed mapping
JOIN rbac3_application application
  ON application.application_code = mapping.application_code
JOIN rbac3_role role
  ON role.application_id = application.id
 AND role.role_code = mapping.role_code
JOIN rbac3_builtin_tenant_seed tenant
  ON tenant.tenant_id = role.tenant_id
JOIN rbac3_permission permission
  ON permission.application_id = application.id
 AND permission.permission_code = mapping.permission_code
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
