package top.egon.cola.platform.rbac3.admin.repository;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIfEnvironmentVariable(named = "RBAC3_IT_POSTGRES_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "RBAC3_IT_POSTGRES_USER", matches = ".+")
@EnabledIfEnvironmentVariable(named = "RBAC3_IT_POSTGRES_PASSWORD_FILE", matches = ".+")
class Rbac3FlywayPostgresqlIT {

    private static final Pattern SAFE_SCHEMA = Pattern.compile(
            "rbac3_it_[a-z0-9]+"
    );

    @Test
    void migratesIdempotentlyAndRejectsCrossBoundaryFacts() throws Exception {
        String url = requiredEnvironment("RBAC3_IT_POSTGRES_URL");
        String user = requiredEnvironment("RBAC3_IT_POSTGRES_USER");
        String password = readPasswordFile();
        String schema = generatedSchema();
        boolean schemaCreated = false;

        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            createSchema(connection, schema);
            schemaCreated = true;
            Flyway flyway = Flyway.configure()
                    .dataSource(url, user, password)
                    .defaultSchema(schema)
                    .schemas(schema)
                    .table("flyway_schema_history_rbac3")
                    .locations("classpath:db/migration")
                    .load();

            assertThat(flyway.migrate().migrationsExecuted).isEqualTo(5);
            assertThat(flyway.migrate().migrationsExecuted).isZero();
            assertThat(flyway.validateWithResult().validationSuccessful).isTrue();

            insertBoundaryFixtures(connection, schema);
            assertCrossTenantAssignmentRejected(connection, schema);
            assertCrossApplicationInheritanceRejected(connection, schema);
            assertCrossApplicationServicePermissionRejected(connection, schema);
            assertApiMappingIdentityRejected(connection, schema);
            assertCrossTenantPositionAutoAssignmentRejected(connection, schema);
            assertImmutableFactsRejected(connection, schema);
            assertAuditIsAppendOnly(connection, schema);
        } finally {
            if (ownsGeneratedSchema(schema, schemaCreated)) {
                dropGeneratedSchema(url, user, password, schema);
            }
        }
    }

    private void insertBoundaryFixtures(Connection connection, String schema)
            throws SQLException {
        execute(connection, schema, """
                insert into rbac3_tenant (
                    id, code, name, status, policy_version, settings,
                    version, created_at, created_by, updated_at, updated_by
                ) values
                    (1, 'tenant-one', 'Tenant One', 'ACTIVE', 0, '{}'::jsonb,
                     0, now(), 'it', now(), 'it'),
                    (2, 'tenant-two', 'Tenant Two', 'ACTIVE', 0, '{}'::jsonb,
                     0, now(), 'it', now(), 'it');

                insert into rbac3_application (
                    id, tenant_id, ddc_application_id, ddc_business_id,
                    application_code, application_name,
                    display_priority, status, version,
                    created_at, created_by, updated_at, updated_by
                ) values
                    (10, 1, 'ddc-app-one', 'ddc-biz-one', 'app-one', 'App One', 100, 'ACTIVE', 0,
                     now(), 'it', now(), 'it'),
                    (11, 1, 'ddc-app-other', 'ddc-biz-one', 'app-other', 'App Other', 200, 'ACTIVE', 0,
                     now(), 'it', now(), 'it'),
                    (20, 2, 'ddc-app-two', 'ddc-biz-two', 'app-two', 'App Two', 100, 'ACTIVE', 0,
                     now(), 'it', now(), 'it');

                insert into rbac3_user (
                    id, tenant_id, identity_sub, status, auth_version, version,
                    created_at, created_by, updated_at, updated_by
                ) values
                    (100, 1, 'user-one', 'ACTIVE', 0, 0,
                     now(), 'it', now(), 'it'),
                    (200, 2, 'user-two', 'ACTIVE', 0, 0,
                     now(), 'it', now(), 'it');

                insert into rbac3_role (
                    id, tenant_id, application_id, role_code, role_name,
                    role_type, risk_level, privileged, status, landing_priority,
                    version, created_at, created_by, updated_at, updated_by
                ) values
                    (1000, 1, 10, 'ROLE_ONE', 'Role One', 'PUBLIC', 'LOW', false,
                     'ACTIVE', 1000, 0, now(), 'it', now(), 'it'),
                    (1100, 1, 11, 'ROLE_OTHER', 'Role Other', 'PUBLIC', 'LOW', false,
                     'ACTIVE', 1000, 0, now(), 'it', now(), 'it'),
                    (2000, 2, 20, 'ROLE_TWO', 'Role Two', 'PUBLIC', 'LOW', false,
                     'ACTIVE', 1000, 0, now(), 'it', now(), 'it');

                insert into rbac3_permission (
                    id, tenant_id, application_id, permission_code,
                    permission_name, risk_level, status, version,
                    created_at, created_by, updated_at, updated_by
                ) values
                    (5000, 1, 10, 'permission:one', 'Permission One',
                     'LOW', 'ACTIVE', 0, now(), 'it', now(), 'it'),
                    (5100, 1, 11, 'permission:other', 'Permission Other',
                     'LOW', 'ACTIVE', 0, now(), 'it', now(), 'it');

                insert into rbac3_service_principal (
                    id, tenant_id, service_code, application_code, application_id,
                    display_name,
                    status, allowed_envs, allowed_namespaces, version,
                    created_at, created_by, updated_at, updated_by
                ) values
                    (6000, 1, 'service-one', 'app-one', 10, 'Service One',
                     'ACTIVE', '[]'::jsonb, '[]'::jsonb, 0,
                     now(), 'it', now(), 'it'),
                    (6001, 1, 'service-other', 'app-other', 11, 'Service Other',
                     'ACTIVE', '[]'::jsonb, '[]'::jsonb, 0,
                     now(), 'it', now(), 'it');

                insert into rbac3_service_permission (
                    id, tenant_id, application_id, principal_id, permission_id,
                    valid_from, version,
                    created_at, created_by, updated_at, updated_by
                ) values (
                    6100, 1, 10, 6000, 5000, now(), 0,
                    now(), 'it', now(), 'it'
                );

                insert into rbac3_resource_manifest (
                    id, tenant_id, application_id, schema_version,
                    artifact_version, build_id, manifest_version, checksum,
                    status, payload, validation_result, received_at, version,
                    created_at, created_by, updated_at, updated_by
                ) values (
                    7000, 1, 10, 1, '1.0.0', 'build-one', 1,
                    'sha256:manifest-one', 'PENDING_VALIDATION', '{}'::jsonb,
                    '{}'::jsonb, now(), 0, now(), 'it', now(), 'it'
                );

                insert into rbac3_resource (
                    id, tenant_id, application_id, resource_type,
                    resource_code, resource_name, status, mechanical_facts,
                    display_metadata, version,
                    created_at, created_by, updated_at, updated_by
                ) values
                    (7100, 1, 10, 'API', 'api-one', 'API One', 'ACTIVE',
                     '{}'::jsonb, '{}'::jsonb, 0,
                     now(), 'it', now(), 'it'),
                    (7101, 1, 10, 'API', 'api-two', 'API Two', 'ACTIVE',
                     '{}'::jsonb, '{}'::jsonb, 0,
                     now(), 'it', now(), 'it'),
                    (7102, 1, 10, 'ACTION', 'action-one', 'Action One', 'ACTIVE',
                     '{}'::jsonb, '{}'::jsonb, 0,
                     now(), 'it', now(), 'it');

                insert into rbac3_permission_resource (
                    id, tenant_id, application_id, permission_id, resource_id,
                    resource_type, definition_set_id, gateway_operation_id,
                    mapping_version, status, version,
                    created_at, created_by, updated_at, updated_by
                ) values (
                    7200, 1, 10, 5000, 7100, 'API', 'definition-one',
                    'operation-one', 1, 'ACTIVE', 0,
                    now(), 'it', now(), 'it'
                );

                insert into rbac3_directory_snapshot (
                    id, tenant_id, provider_code, snapshot_version, checksum,
                    status, generated_at, received_at, payload, counts, version,
                    created_at, created_by, updated_at, updated_by
                ) values (
                    8000, 2, 'directory-two', 1, 'sha256:snapshot-two',
                    'ACTIVE', now(), now(), '{}'::jsonb, '{}'::jsonb, 0,
                    now(), 'it', now(), 'it'
                );

                insert into rbac3_org_unit (
                    id, tenant_id, snapshot_id, source_type, unit_type, code, name, path,
                    depth, status, valid_from, version,
                    created_at, created_by, updated_at, updated_by
                ) values (
                    8100, 2, 8000, 'DIRECTORY_SNAPSHOT', 'ORG', 'org-two', 'Org Two', '/org-two',
                    0, 'ACTIVE', now(), 0, now(), 'it', now(), 'it'
                );

                insert into rbac3_position (
                    id, tenant_id, snapshot_id, source_type, code, name, org_unit_id,
                    status, valid_from, version,
                    created_at, created_by, updated_at, updated_by
                ) values (
                    8200, 2, 8000, 'DIRECTORY_SNAPSHOT', 'position-two', 'Position Two', 8100,
                    'ACTIVE', now(), 0, now(), 'it', now(), 'it'
                );

                insert into rbac3_audit_log (
                    id, tenant_id, event_type, outcome, severity, actor_type,
                    actor_id, target_type, target_id, request_id, trace_id,
                    payload_checksum, created_at
                ) values (
                    9000, 1, 'RBAC3_IT', 'SUCCESS', 'INFO', 'SERVICE',
                    'rbac3-it', 'TENANT', '1', 'request-it', 'trace-it',
                    'sha256:fixture', now()
                )
                """);
    }

    private void assertCrossTenantAssignmentRejected(
            Connection connection,
            String schema
    ) {
        assertThatThrownBy(() -> execute(connection, schema, """
                insert into rbac3_user_role_assignment (
                    id, tenant_id, user_id, role_id, assignment_type, status,
                    valid_from, source_type, source_id, version,
                    created_at, created_by, updated_at, updated_by
                ) values (
                    3000, 1, 200, 1000, 'DIRECT', 'ACTIVE', now(),
                    'MANUAL', 'it', 0, now(), 'it', now(), 'it'
                )
                """))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("foreign key");
    }

    private void assertCrossApplicationServicePermissionRejected(
            Connection connection,
            String schema
    ) {
        assertThatThrownBy(() -> execute(connection, schema, """
                insert into rbac3_service_permission (
                    id, tenant_id, application_id, principal_id, permission_id,
                    valid_from, version,
                    created_at, created_by, updated_at, updated_by
                ) values (
                    6200, 1, 11, 6000, 5100, now(), 0,
                    now(), 'it', now(), 'it'
                )
                """))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("foreign key");

        assertThatThrownBy(() -> execute(connection, schema, """
                insert into rbac3_service_permission (
                    id, tenant_id, application_id, principal_id, permission_id,
                    valid_from, version,
                    created_at, created_by, updated_at, updated_by
                ) values (
                    6202, 1, 10, 6001, 5000, now(), 0,
                    now(), 'it', now(), 'it'
                )
                """))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("foreign key");

        assertThatThrownBy(() -> execute(connection, schema, """
                insert into rbac3_service_permission (
                    id, tenant_id, application_id, principal_id, permission_id,
                    valid_from, version,
                    created_at, created_by, updated_at, updated_by
                ) values (
                    6201, 1, 10, 6000, 5100, now(), 0,
                    now(), 'it', now(), 'it'
                )
                """))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("foreign key");
    }

    private void assertApiMappingIdentityRejected(
            Connection connection,
            String schema
    ) {
        assertThatThrownBy(() -> execute(connection, schema, """
                insert into rbac3_permission_resource (
                    id, tenant_id, application_id, permission_id, resource_id,
                    resource_type, mapping_version, status, version,
                    created_at, created_by, updated_at, updated_by
                ) values (
                    7201, 1, 10, 5000, 7101, 'API', 2, 'ACTIVE', 0,
                    now(), 'it', now(), 'it'
                )
                """))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("check constraint");

        assertThatThrownBy(() -> execute(connection, schema, """
                insert into rbac3_permission_resource (
                    id, tenant_id, application_id, permission_id, resource_id,
                    resource_type, definition_set_id, gateway_operation_id,
                    mapping_version, status, version,
                    created_at, created_by, updated_at, updated_by
                ) values (
                    7204, 1, 10, 5000, 7102, 'ACTION', 'definition-one',
                    'operation-one', 2, 'ACTIVE', 0,
                    now(), 'it', now(), 'it'
                )
                """))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("check constraint");

        assertThatThrownBy(() -> execute(connection, schema, """
                insert into rbac3_permission_resource (
                    id, tenant_id, application_id, permission_id, resource_id,
                    resource_type, definition_set_id, mapping_version,
                    status, version, created_at, created_by, updated_at, updated_by
                ) values (
                    7202, 1, 10, 5000, 7101, 'API', 'definition-one', 2,
                    'ACTIVE', 0, now(), 'it', now(), 'it'
                )
                """))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("check constraint");

        assertThatThrownBy(() -> execute(connection, schema, """
                insert into rbac3_permission_resource (
                    id, tenant_id, application_id, permission_id, resource_id,
                    resource_type, definition_set_id, gateway_operation_id,
                    mapping_version, status, version,
                    created_at, created_by, updated_at, updated_by
                ) values (
                    7203, 1, 10, 5000, 7101, 'API', 'definition-one',
                    'operation-one', 1, 'ACTIVE', 0,
                    now(), 'it', now(), 'it'
                )
                """))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("unique constraint");
    }

    private void assertCrossTenantPositionAutoAssignmentRejected(
            Connection connection,
            String schema
    ) {
        assertThatThrownBy(() -> execute(connection, schema, """
                insert into rbac3_auto_assignment_rule (
                    id, tenant_id, rule_code, match_type, match_ref_id, role_id,
                    status, valid_from, version,
                    created_at, created_by, updated_at, updated_by
                ) values (
                    8300, 1, 'cross-tenant-position', 'POSITION', 8200, 1000,
                    'ACTIVE', now(), 0, now(), 'it', now(), 'it'
                )
                """))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("foreign key");
    }

    private void assertImmutableFactsRejected(
            Connection connection,
            String schema
    ) {
        assertImmutableUpdateRejected(
                connection, schema,
                "update rbac3_directory_snapshot set checksum = 'changed' where id = 8000"
        );
        assertImmutableUpdateRejected(
                connection, schema,
                "update rbac3_resource_manifest set checksum = 'changed' where id = 7000"
        );
        assertImmutableUpdateRejected(
                connection, schema,
                "update rbac3_permission set permission_code = 'changed' where id = 5000"
        );
        assertImmutableUpdateRejected(
                connection, schema,
                "update rbac3_resource set mechanical_facts = '{\"changed\":true}'::jsonb where id = 7100"
        );
        assertImmutableUpdateRejected(
                connection, schema,
                "update rbac3_role set role_code = 'ROLE_CHANGED' where id = 1000"
        );
        assertImmutableUpdateRejected(
                connection, schema,
                "update rbac3_role set privileged = true where id = 1000"
        );
        assertImmutableUpdateRejected(
                connection, schema,
                "update rbac3_permission_resource set gateway_operation_id = 'changed' where id = 7200"
        );
    }

    private void assertImmutableUpdateRejected(
            Connection connection,
            String schema,
            String sql
    ) {
        assertThatThrownBy(() -> execute(connection, schema, sql))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("immutable");
    }

    private void assertCrossApplicationInheritanceRejected(
            Connection connection,
            String schema
    ) {
        assertThatThrownBy(() -> execute(connection, schema, """
                insert into rbac3_role_inheritance (
                    id, tenant_id, application_id, senior_role_id, junior_role_id,
                    version, created_at, created_by, updated_at, updated_by
                ) values (
                    4000, 1, 10, 1000, 1100,
                    0, now(), 'it', now(), 'it'
                )
                """))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("foreign key");
    }

    private void assertAuditIsAppendOnly(Connection connection, String schema) {
        assertThatThrownBy(() -> execute(
                connection,
                schema,
                "update rbac3_audit_log set outcome = 'FAILURE' where id = 9000"
        )).isInstanceOf(SQLException.class)
                .hasMessageContaining("append-only");
    }

    private void createSchema(Connection connection, String schema)
            throws SQLException {
        requireSafeSchema(schema);
        try (Statement statement = connection.createStatement()) {
            statement.execute("create schema " + schema);
        }
    }

    private void dropGeneratedSchema(
            String url,
            String user,
            String password,
            String schema
    ) throws SQLException {
        requireSafeSchema(schema);
        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement()) {
            statement.execute("drop schema if exists " + schema + " cascade");
        }
    }

    private void execute(Connection connection, String schema, String sql)
            throws SQLException {
        requireSafeSchema(schema);
        try (Statement statement = connection.createStatement()) {
            statement.execute("set search_path to " + schema);
            statement.execute(sql);
        }
    }

    private String generatedSchema() {
        String suffix = UUID.randomUUID().toString()
                .replace("-", "")
                .toLowerCase(Locale.ROOT);
        String schema = "rbac3_it_" + suffix;
        requireSafeSchema(schema);
        return schema;
    }

    static boolean ownsGeneratedSchema(String schema, boolean schemaCreated) {
        requireSafeSchema(schema);
        return schemaCreated;
    }

    static void requireSafeSchema(String schema) {
        if (!SAFE_SCHEMA.matcher(schema).matches()) {
            throw new IllegalArgumentException("unsafe RBAC3 integration schema");
        }
    }

    private String readPasswordFile() throws IOException {
        String file = requiredEnvironment("RBAC3_IT_POSTGRES_PASSWORD_FILE");
        Path path = Path.of(file);
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException(
                    "RBAC3 PostgreSQL password file is not a regular file"
            );
        }
        return Files.readString(path, StandardCharsets.UTF_8).stripTrailing();
    }

    private String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("missing required environment: " + name);
        }
        return value;
    }
}
