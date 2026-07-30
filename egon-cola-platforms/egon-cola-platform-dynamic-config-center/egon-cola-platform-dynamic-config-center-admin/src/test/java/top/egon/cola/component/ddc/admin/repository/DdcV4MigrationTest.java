package top.egon.cola.component.ddc.admin.repository;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class DdcV4MigrationTest {

    @Test
    void sqliteUpgradeBackfillsExistingRowsAndKeepsNewDraftPointerNullable()
            throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            execute(connection, script("db/sqlite/V1__create_ddc_schema.sql"));
            execute(connection, script("db/sqlite/V2__add_lease_and_sync_publish.sql"));
            execute(connection, script("db/sqlite/V3__add_instance_runtime_metadata.sql"));
            executeStatement(connection, """
                    insert into ddc_config_item (
                        id, app_code, env, namespace, config_key, config_value,
                        value_type, current_version, created_at, updated_at
                    ) values (
                        'config-1', 'orders', 'test', 'default', 'feature.flag', 'true',
                        'BOOLEAN', 3, '2026-07-26 00:00:00', '2026-07-26 00:00:00'
                    )
                    """);

            execute(connection, script(
                    "db/sqlite/V4__add_published_config_pointer.sql"
            ));

            assertThat(queryLong(connection, """
                    select published_version
                    from ddc_config_item
                    where id = 'config-1'
                    """)).isEqualTo(3L);
            assertThatCode(() -> executeStatement(connection, """
                    insert into ddc_config_item (
                        id, app_code, env, namespace, config_key, config_value,
                        value_type, current_version, created_at, updated_at
                    ) values (
                        'config-2', 'orders', 'test', 'default', 'new.draft', 'draft',
                        'STRING', 1, '2026-07-26 00:00:01', '2026-07-26 00:00:01'
                    )
                    """)).doesNotThrowAnyException();
            assertThat(queryObject(connection, """
                    select published_version
                    from ddc_config_item
                    where id = 'config-2'
                    """)).isNull();
        }
    }

    @Test
    void postgresqlMigrationKeepsPointerNullableAndBackfillsExistingRows()
            throws Exception {
        String sql = script(
                "db/postgresql/V4__add_published_config_pointer.sql"
        ).toLowerCase();

        assertThat(sql).contains(
                "alter table ddc_config_item add column published_version bigint"
        );
        assertThat(sql).contains(
                "update ddc_config_item set published_version = current_version"
        );
        assertThat(sql).doesNotContain("published_version bigint not null");
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
