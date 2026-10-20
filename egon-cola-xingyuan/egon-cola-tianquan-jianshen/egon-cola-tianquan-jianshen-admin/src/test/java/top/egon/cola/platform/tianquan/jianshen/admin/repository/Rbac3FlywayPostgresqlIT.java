package top.egon.cola.platform.tianquan.jianshen.admin.repository;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/** PostgreSQL proof for fresh current-schema initialization and repeatable migration. */
@EnabledIfEnvironmentVariable(named = "TIANQUAN_JIANSHEN_IT_POSTGRES_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "TIANQUAN_JIANSHEN_IT_POSTGRES_USER", matches = ".+")
@EnabledIfEnvironmentVariable(named = "TIANQUAN_JIANSHEN_IT_POSTGRES_PASSWORD_FILE", matches = ".+")
class Rbac3FlywayPostgresqlIT {

    private static final int BASELINE_VERSION = 14;
    private static final Pattern SAFE_SCHEMA = Pattern.compile("rbac3_it_[a-z0-9]+");

    @Test
    void initializesCurrentSchemaFromBaselineWithoutReplayingLegacyMigrations() throws Exception {
        String url = requiredEnvironment("TIANQUAN_JIANSHEN_IT_POSTGRES_URL");
        String user = requiredEnvironment("TIANQUAN_JIANSHEN_IT_POSTGRES_USER");
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
                    .target(Integer.toString(BASELINE_VERSION))
                    .initSql("SET rbac3.bootstrap.tenant_ids = '1001,1002'; "
                            + "SET rbac3.bootstrap.identity_sub = '9001'")
                    .locations("classpath:db/migration")
                    .load();

            assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
            assertThat(flyway.migrate().migrationsExecuted).isZero();
            assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
            assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("14");
            assertThat(flyway.info().current().getScript()).isEqualTo("B14__create_current_rbac3_schema.sql");

            assertThat(tableExists(connection, schema, "rbac3_role_resource_grant")).isTrue();
            assertThat(tableExists(connection, schema, "rbac3_resource_api_binding")).isTrue();
            assertThat(tableExists(connection, schema, "rbac3_role_permission")).isFalse();
            assertThat(tableExists(connection, schema, "rbac3_permission_resource")).isFalse();
            assertThat(columnExists(connection, schema, "rbac3_resource",
                    "suggested_permission_code")).isTrue();
            assertThat(columnExists(connection, schema, "rbac3_role_resource_grant",
                    "resource_id")).isTrue();
            assertThat(columnExists(connection, schema, "rbac3_resource_api_binding",
                    "source_resource_id")).isTrue();
            assertThat(tableExists(connection, schema, "rbac3_tenant")).isFalse();
            assertThat(tableExists(connection, schema, "rbac3_session")).isFalse();
            assertThat(tableExists(connection, schema, "rbac3_user_credential")).isFalse();
            assertThat(queryBoolean(connection, "select count(*) = 2 from " + schema
                    + ".rbac3_tenant_authorization_state where tenant_id in (1001,1002)"))
                    .isTrue();
            assertThat(queryBoolean(connection, "select count(*) = 2 from " + schema
                    + ".rbac3_user where identity_sub = '9001'"))
                    .isTrue();
        } finally {
            if (ownsGeneratedSchema(schema, schemaCreated)) {
                dropGeneratedSchema(url, user, password, schema);
            }
        }
    }

    @Test
    void workerFunctionsSkipLockedRowsAndReleaseTheirLocksOnRollback() throws Exception {
        String url = requiredEnvironment("TIANQUAN_JIANSHEN_IT_POSTGRES_URL");
        String user = requiredEnvironment("TIANQUAN_JIANSHEN_IT_POSTGRES_USER");
        String password = readPasswordFile();
        String schema = generatedSchema();
        boolean schemaCreated = false;
        try (Connection first = DriverManager.getConnection(url, user, password);
             Connection second = DriverManager.getConnection(url, user, password)) {
            createSchema(first, schema);
            schemaCreated = true;
            Flyway flyway = Flyway.configure().dataSource(url, user, password)
                    .defaultSchema(schema).schemas(schema).table("flyway_schema_history_rbac3")
                    .target("15").locations("classpath:db/migration")
                    .initSql("SET rbac3.bootstrap.tenant_ids = '1001,1002'; "
                            + "SET rbac3.bootstrap.identity_sub = '9001'")
                    .load();
            assertThat(flyway.migrate().migrationsExecuted).isEqualTo(2);
            assertThat(flyway.migrate().migrationsExecuted).isZero();
            assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
            for (Connection connection : new Connection[]{first, second}) {
                execute(connection, "set search_path to \"" + schema + "\"");
            }
            execute(first, """
                    insert into rbac3_authorization_mutation
                        (mutation_id, tenant_id, scope_type, command_id, status,
                         created_at, created_by, updated_at, updated_by)
                    values (1, 1001, 'TENANT', 'worker-claim-1', 'COMMITTED',
                            now(), 'claim-test', now(), 'claim-test'),
                           (2, 1001, 'TENANT', 'worker-claim-2', 'COMMITTED',
                            now(), 'claim-test', now(), 'claim-test')
                    """);
            execute(first, """
                    insert into rbac3_user_role_assignment
                        (id, tenant_id, user_id, role_id, assignment_type, status, valid_from, valid_to,
                         source_type, source_id, created_at, created_by, updated_at, updated_by)
                    select seed.id, 1001, user_row.id, role_row.id, 'DIRECT', 'PENDING',
                           now() - interval '2 hours', now() - interval '1 hour',
                           'MANUAL', 'worker-claim-' || seed.id, now(), 'claim-test', now(), 'claim-test'
                      from (values (111::bigint), (112::bigint)) seed(id)
                      cross join (select min(id) id from rbac3_user where tenant_id = 1001) user_row
                      cross join (select min(id) id from rbac3_role where tenant_id = 1001) role_row
                    """);
            first.setAutoCommit(false);
            second.setAutoCommit(false);
            execute(second, "set local statement_timeout = '2s'");
            assertThat(claimedMutation(first)).isEqualTo(1L);
            assertThat(claimedMutation(second)).isEqualTo(2L);
            first.rollback();
            assertThat(claimedMutation(first)).isEqualTo(1L);
            first.rollback();
            second.rollback();
            assertThat(claimedAssignment(first)).isEqualTo(111L);
            execute(second, "set local statement_timeout = '2s'");
            assertThat(claimedAssignment(second)).isEqualTo(112L);
            first.rollback();
            assertThat(claimedAssignment(first)).isEqualTo(111L);
            first.rollback();
            second.rollback();
        } finally {
            if (ownsGeneratedSchema(schema, schemaCreated)) {
                dropGeneratedSchema(url, user, password, schema);
            }
        }
    }

    private static long claimedMutation(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("select rbac3_claim_recoverable_mutations(1)")) {
            assertThat(rows.next()).isTrue();
            long id = rows.getLong(1);
            assertThat(rows.next()).isFalse();
            return id;
        }
    }

    private static long claimedAssignment(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "select rbac3_claim_due_assignments('PENDING', current_timestamp, 1)")) {
            assertThat(rows.next()).isTrue();
            long id = rows.getLong(1);
            assertThat(rows.next()).isFalse();
            return id;
        }
    }

    private static boolean tableExists(Connection connection, String schema, String table)
            throws SQLException {
        return queryBoolean(connection, """
                select exists (
                    select 1 from information_schema.tables
                     where table_schema = '%s' and table_name = '%s'
                )
                """.formatted(schema, table));
    }

    private static boolean columnExists(
            Connection connection,
            String schema,
            String table,
            String column) throws SQLException {
        return queryBoolean(connection, """
                select exists (
                    select 1 from information_schema.columns
                     where table_schema = '%s'
                       and table_name = '%s'
                       and column_name = '%s'
                )
                """.formatted(schema, table, column));
    }

    private static boolean queryBoolean(Connection connection, String sql)
            throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getBoolean(1);
        }
    }

    private static void createSchema(Connection connection, String schema) throws SQLException {
        execute(connection, "create schema \"" + schema + "\"");
    }

    private static void dropGeneratedSchema(
            String url,
            String user,
            String password,
            String schema) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            execute(connection, "drop schema if exists \"" + schema + "\" cascade");
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String generatedSchema() {
        String schema = "rbac3_it_" + UUID.randomUUID().toString()
                .replace("-", "").toLowerCase();
        if (!SAFE_SCHEMA.matcher(schema).matches()) {
            throw new IllegalStateException("generated schema is unsafe");
        }
        return schema;
    }

    static boolean ownsGeneratedSchema(String schema, boolean schemaCreated) {
        if (!SAFE_SCHEMA.matcher(schema).matches()) {
            throw new IllegalArgumentException("unsafe Tianquan-Jianshen integration schema");
        }
        return schemaCreated && SAFE_SCHEMA.matcher(schema).matches();
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value.trim();
    }

    private static String readPasswordFile() throws IOException {
        Path path = Path.of(requiredEnvironment("TIANQUAN_JIANSHEN_IT_POSTGRES_PASSWORD_FILE"));
        return Files.readString(path, StandardCharsets.UTF_8).trim();
    }
}
