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

class DdcV9MigrationTest {

    @Test
    void sqliteV9RestoresGloballyUniqueApplicationCodes() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            migrateSqliteThroughV8(connection);

            execute(connection, script(
                    "db/sqlite/V9__enforce_global_biz_app_codes.sql"
            ));

            assertThat(indexColumns(connection, "uk_ddc_biz_code"))
                    .containsExactly("biz_code");
            assertThat(indexColumns(connection, "uk_ddc_app_code"))
                    .containsExactly("app_code");
            assertThat(indexColumns(connection, "uk_ddc_app_biz_code"))
                    .isEmpty();

            seedBusiness(connection, "biz-a", "domain-a");
            seedBusiness(connection, "biz-b", "domain-b");
            seedApplication(connection, "app-a", "shared-app", "domain-a");

            assertThatThrownBy(() -> seedApplication(
                    connection,
                    "app-b",
                    "shared-app",
                    "domain-b"
            )).isInstanceOf(SQLException.class);
        }
    }

    @Test
    void postgresqlV9DefinesTheSameGlobalCodeConstraints() throws Exception {
        String sql = script(
                "db/postgresql/V9__enforce_global_biz_app_codes.sql"
        ).toLowerCase();

        assertThat(sql)
                .contains("drop index uk_ddc_app_biz_code")
                .contains("create unique index uk_ddc_app_code")
                .contains("on ddc_app(app_code)")
                .doesNotContain("on ddc_app(biz_code, app_code)");
    }

    private void migrateSqliteThroughV8(Connection connection) throws Exception {
        String[] migrations = {
                "V1__create_ddc_schema.sql",
                "V2__add_lease_and_sync_publish.sql",
                "V3__add_instance_runtime_metadata.sql",
                "V4__add_published_config_pointer.sql",
                "V5__add_biz_env_and_detach_namespace_env.sql",
                "V6__add_namespace_code.sql",
                "V7__add_namespace_env_app_visibility.sql",
                "V8__add_resource_admission_audit.sql"
        };
        for (String migration : migrations) {
            execute(connection, script("db/sqlite/" + migration));
        }
    }

    private void seedBusiness(
            Connection connection,
            String id,
            String code
    ) throws SQLException {
        executeStatement(connection, """
                insert into ddc_biz (
                    id, biz_code, biz_name, enabled, created_at, updated_at
                ) values (
                    '%s', '%s', '%s', 1,
                    '2026-08-18 00:00:00', '2026-08-18 00:00:00'
                )
                """.formatted(id, code, code));
    }

    private void seedApplication(
            Connection connection,
            String id,
            String appCode,
            String bizCode
    ) throws SQLException {
        executeStatement(connection, """
                insert into ddc_app (
                    id, app_code, biz_code, app_name, enabled,
                    created_at, updated_at
                ) values (
                    '%s', '%s', '%s', '%s', 1,
                    '2026-08-18 00:00:00', '2026-08-18 00:00:00'
                )
                """.formatted(id, appCode, bizCode, appCode));
    }

    private List<String> indexColumns(
            Connection connection,
            String index
    ) throws SQLException {
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

    private void execute(Connection connection, String sql) throws SQLException {
        for (String statement : sql.split(";")) {
            if (!statement.isBlank()) {
                executeStatement(connection, statement);
            }
        }
    }

    private void executeStatement(Connection connection, String sql)
            throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
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
