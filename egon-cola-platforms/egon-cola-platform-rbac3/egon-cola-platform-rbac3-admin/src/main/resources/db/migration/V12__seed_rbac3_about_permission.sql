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

INSERT INTO rbac3_role_permission (
    id, tenant_id, application_id, role_id, permission_id,
    valid_from, valid_to, status, version,
    created_at, created_by, updated_at, updated_by
)
SELECT
    8410000000000000001,
    role.tenant_id,
    role.application_id,
    role.id,
    permission.id,
    CURRENT_TIMESTAMP,
    NULL,
    'ACTIVE',
    0,
    CURRENT_TIMESTAMP,
    'flyway-v12',
    CURRENT_TIMESTAMP,
    'flyway-v12'
FROM rbac3_role role
JOIN rbac3_permission permission
  ON permission.application_id = role.application_id
 AND permission.permission_code = 'system:about:read'
WHERE role.role_code = 'RBAC3_LOCAL_ADMIN'
  AND role.application_id = permission.application_id
ON CONFLICT DO NOTHING;
