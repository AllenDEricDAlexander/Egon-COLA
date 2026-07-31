package top.egon.cola.component.ddc.admin.repository;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class DdcV5MigrationTest {

    @Test
    void sqliteUpgradeCreatesBizAndEnvAndDetachesNamespaceEnv() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            execute(connection, script("db/sqlite/V1__create_ddc_schema.sql"));
            execute(connection, script("db/sqlite/V2__add_lease_and_sync_publish.sql"));
            execute(connection, script("db/sqlite/V3__add_instance_runtime_metadata.sql"));
            execute(connection, script("db/sqlite/V4__add_published_config_pointer.sql"));
            executeStatement(connection, """
                    insert into ddc_app (
                        id, app_code, app_name, owner, description, enabled, created_at, updated_at
                    ) values (
                        'app-1', 'orders', '订单服务', 'ops', '', 1, '2026-07-01 00:00:00', '2026-07-01 00:00:00'
                    )
                    """);
            // 同 (app_code, namespace) 不同 env 的两行：迁移后应只保留最早一行
            executeStatement(connection, """
                    insert into ddc_namespace (
                        id, app_code, env, namespace, description, enabled, created_at, updated_at
                    ) values
                        ('ns-1', 'orders', 'dev', 'default', '', 1, '2026-07-01 00:00:00', '2026-07-01 00:00:00'),
                        ('ns-2', 'orders', 'test', 'default', '', 1, '2026-07-02 00:00:00', '2026-07-02 00:00:00')
                    """);

            execute(connection, script(
                    "db/sqlite/V5__add_biz_env_and_detach_namespace_env.sql"
            ));

            assertThat(queryObject(connection, """
                    select biz_code from ddc_app where app_code = 'orders'
                    """)).isEqualTo("default");
            assertThat(queryLong(connection, """
                    select count(*) from ddc_namespace where app_code = 'orders'
                    """)).isEqualTo(1L);
            assertThat(queryObject(connection, """
                    select id from ddc_namespace where app_code = 'orders'
                    """)).isEqualTo("ns-1");
            assertThat(queryLong(connection, """
                    select count(*) from ddc_env
                    """)).isEqualTo(5L);
            assertThat(queryObject(connection, """
                    select env_code from ddc_env order by sort_order limit 1
                    """)).isEqualTo("dev");
            assertThat(queryLong(connection, """
                    select count(*) from ddc_biz where biz_code = 'default'
                    """)).isEqualTo(1L);
            // (app_code, namespace) 唯一约束生效
            assertThat(queryLong(connection, """
                    select count(*) from sqlite_master
                     where type = 'index' and name = 'uk_ddc_namespace_key'
                    """)).isEqualTo(1L);
        }
    }

    @Test
    void postgresqlMigrationContainsExpectedShape() throws Exception {
        String sql = script(
                "db/postgresql/V5__add_biz_env_and_detach_namespace_env.sql"
        ).toLowerCase();

        assertThat(sql).contains("create table ddc_biz");
        assertThat(sql).contains("create table ddc_env");
        assertThat(sql).contains("alter table ddc_app add column biz_code");
        assertThat(sql).contains("alter table ddc_app alter column biz_code set not null");
        assertThat(sql).contains("alter table ddc_namespace drop column env");
        assertThat(sql).contains(
                "create unique index uk_ddc_namespace_key on ddc_namespace(app_code, namespace)"
        );
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
