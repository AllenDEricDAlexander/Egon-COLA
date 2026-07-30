package top.egon.cola.platform.rbac3.admin.infrastructure.persistence;

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

            assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
            assertThat(flyway.migrate().migrationsExecuted).isZero();
            assertThat(flyway.validateWithResult().validationSuccessful).isTrue();

            insertBoundaryFixtures(connection, schema);
            assertCrossTenantAssignmentRejected(connection, schema);
            assertCrossApplicationInheritanceRejected(connection, schema);
            assertAuditIsAppendOnly(connection, schema);
        } finally {
            if (schemaCreated) {
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
                    id, tenant_id, application_code, application_name,
                    display_priority, status, version,
                    created_at, created_by, updated_at, updated_by
                ) values
                    (10, 1, 'app-one', 'App One', 100, 'ACTIVE', 0,
                     now(), 'it', now(), 'it'),
                    (11, 1, 'app-other', 'App Other', 200, 'ACTIVE', 0,
                     now(), 'it', now(), 'it'),
                    (20, 2, 'app-two', 'App Two', 100, 'ACTIVE', 0,
                     now(), 'it', now(), 'it');

                insert into rbac3_user (
                    id, tenant_id, username, normalized_username, display_name,
                    status, auth_version, directory_snapshot_version, version,
                    created_at, created_by, updated_at, updated_by
                ) values
                    (100, 1, 'user-one', 'user-one', 'User One', 'ACTIVE', 0, 0, 0,
                     now(), 'it', now(), 'it'),
                    (200, 2, 'user-two', 'user-two', 'User Two', 'ACTIVE', 0, 0, 0,
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

    private void requireSafeSchema(String schema) {
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
