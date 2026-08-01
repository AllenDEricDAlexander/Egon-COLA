package top.egon.cola.component.ddc.admin.repository;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcV7MigrationTest {

    @Test
    void sqliteV7BuildsBizNamespaceAndEnvironmentBindings() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            migrateThroughV4(connection);
            seedAppNamespace(connection, false);
            migrateV5AndV6(connection);

            execute(connection, script(
                    "db/sqlite/V7__add_namespace_env_app_visibility.sql"
            ));

            assertThat(queryLong(connection,
                    "select count(*) from ddc_namespace_env_app"))
                    .isEqualTo(5L);
            assertThat(queryObject(connection, """
                    select biz_code from ddc_namespace
                     where namespace_code = 'default'
                    """)).isEqualTo("default");
            assertThat(indexColumns(connection, "uk_ddc_namespace_env_app"))
                    .containsExactly("namespace_id", "env_code", "app_id");
            assertThat(indexColumns(connection, "uk_ddc_app_biz_code"))
                    .containsExactly("biz_code", "app_code");
            assertThat(indexColumns(connection, "uk_ddc_config_item_physical"))
                    .containsExactly("biz_code", "env", "app_code", "config_key");
        }
    }

    @Test
    void sqliteV7RejectsDuplicatePhysicalConfigAcrossLegacyNamespaces()
            throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            migrateThroughV4(connection);
            seedAppNamespace(connection, true);
            migrateV5AndV6(connection);

            assertThatThrownBy(() -> execute(connection, script(
                    "db/sqlite/V7__add_namespace_env_app_visibility.sql"
            ))).isInstanceOf(SQLException.class);
        }
    }

    @Test
    void postgresqlV7ContainsTheSameAssociationAndPhysicalConstraints()
            throws Exception {
        String sql = script(
                "db/postgresql/V7__add_namespace_env_app_visibility.sql"
        ).toLowerCase();

        assertThat(sql)
                .contains("create table ddc_namespace_env_app")
                .contains("uk_ddc_namespace_env_app")
                .contains("namespace_id, env_code, app_id")
                .contains("uk_ddc_app_biz_code")
                .contains("biz_code, app_code")
                .contains("uk_ddc_config_item_physical")
                .contains("biz_code, env, app_code, config_key");
    }

    private void migrateThroughV4(Connection connection) throws Exception {
        for (int version = 1; version <= 4; version++) {
            execute(connection, script("db/sqlite/V" + version + migrationName(version)));
        }
    }

    private String migrationName(int version) {
        return switch (version) {
            case 1 -> "__create_ddc_schema.sql";
            case 2 -> "__add_lease_and_sync_publish.sql";
            case 3 -> "__add_instance_runtime_metadata.sql";
            case 4 -> "__add_published_config_pointer.sql";
            default -> throw new IllegalArgumentException("unsupported version");
        };
    }

    private void migrateV5AndV6(Connection connection) throws Exception {
        execute(connection, script(
                "db/sqlite/V5__add_biz_env_and_detach_namespace_env.sql"
        ));
        execute(connection, script("db/sqlite/V6__add_namespace_code.sql"));
    }

    private void seedAppNamespace(Connection connection, boolean duplicateConfig)
            throws SQLException {
        executeStatement(connection, """
                insert into ddc_app (
                    id, app_code, app_name, enabled, created_at, updated_at
                ) values (
                    'app-orders', 'orders', '订单服务', 1,
                    '2026-08-01 00:00:00', '2026-08-01 00:00:00'
                )
                """);
        executeStatement(connection, """
                insert into ddc_namespace (
                    id, app_code, env, namespace, enabled, created_at, updated_at
                ) values (
                    'ns-default', 'orders', 'dev', 'default', 1,
                    '2026-08-01 00:00:00', '2026-08-01 00:00:00'
                )
                """);
        seedConfig(connection, "config-a", "default");
        if (duplicateConfig) {
            executeStatement(connection, """
                    insert into ddc_namespace (
                        id, app_code, env, namespace, enabled, created_at, updated_at
                    ) values (
                        'ns-ops', 'orders', 'dev', 'ops', 1,
                        '2026-08-01 00:00:00', '2026-08-01 00:00:00'
                    )
                    """);
            seedConfig(connection, "config-b", "ops");
        }
    }

    private void seedConfig(Connection connection, String id, String namespace)
            throws SQLException {
        executeStatement(connection, """
                insert into ddc_config_item (
                    id, app_code, env, namespace, config_key, config_value,
                    value_type, current_version, enabled, deleted,
                    created_at, updated_at
                ) values (
                    '%s', 'orders', 'dev', '%s', 'feature.enabled', 'true',
                    'BOOLEAN', 1, 1, 0,
                    '2026-08-01 00:00:00', '2026-08-01 00:00:00'
                )
                """.formatted(id, namespace));
    }

    private void execute(Connection connection, String sql) throws SQLException {
        for (String statement : sql.split(";")) {
            if (!statement.isBlank()) {
                executeStatement(connection, statement);
            }
        }
    }

    private void executeStatement(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long queryLong(Connection connection, String sql) throws SQLException {
        return ((Number) queryObject(connection, sql)).longValue();
    }

    private Object queryObject(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            return result.getObject(1);
        }
    }

    private List<String> indexColumns(Connection connection, String index)
            throws SQLException {
        List<String> columns = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "pragma index_info('" + index + "')")) {
            while (result.next()) {
                columns.add(result.getString("name"));
            }
        }
        return columns;
    }

    private String script(String path) throws Exception {
        try (var input = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("missing migration: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
