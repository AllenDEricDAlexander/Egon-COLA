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
