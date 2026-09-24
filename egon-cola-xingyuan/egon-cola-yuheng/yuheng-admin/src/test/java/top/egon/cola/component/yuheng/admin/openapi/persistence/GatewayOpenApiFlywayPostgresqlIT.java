package top.egon.cola.component.yuheng.admin.openapi.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 中文说明：{@code GatewayOpenApiFlywayPostgresqlIT} 在真实 Postgres 上固化 openapi 两张表的迁移侧契约：
 * V1..V13 全量应用、{@code gateway_openapi_snapshot} 与 {@code gateway_openapi_sync_state} 就位，
 * 并且门面存储赖以判定“复用既有意图”“按业务键定位”“到期可认领”的库级约束仍然存在——
 * 快照四列不可变契约 {@code uk_gateway_openapi_snapshot_contract}、journal 三列业务键
 * {@code uk_gateway_openapi_sync_key}、以及只覆盖 {@code claim} 所接受状态的 partial index
 * {@code idx_gateway_openapi_sync_due}。
 * English summary: This integration test pins the migration-side contract of the two openapi tables on a real
 * Postgres instance: V1..V13 apply cleanly, {@code gateway_openapi_snapshot} and {@code gateway_openapi_sync_state}
 * exist, and the database constraints the facade stores rely on to decide “reuse the existing business intent”,
 * “locate the row by business key” and “this row is due and claimable” are still there — the four-column immutable
 * snapshot contract {@code uk_gateway_openapi_snapshot_contract}, the three-column journal key
 * {@code uk_gateway_openapi_sync_key} and the partial index {@code idx_gateway_openapi_sync_due} that covers exactly
 * the states {@code claim} accepts.
 *
 * 用法 / Usage: 仅在显式提供 {@code YUHENG_OPENAPI_TEST_POSTGRES_URL} 时运行；适配器读写语义（CAS 谓词、影响 0 行
 * 不判成功、到期次序、观察列回写）已由 {@code GatewayPublicationMpTest} 在受守卫 MP 边界上验证，本类只补齐库级
 * 不变量那一半。/ Run only when {@code YUHENG_OPENAPI_TEST_POSTGRES_URL} is provided; the adapter read/write semantics
 * (CAS predicates, a zero-row effect never reporting success, due ordering, observation write-back) are verified on the
 * guarded MP boundary by {@code GatewayPublicationMpTest}, so this class covers only the database-side half.
 */
@EnabledIfEnvironmentVariable(
        named = "YUHENG_OPENAPI_TEST_POSTGRES_URL",
        matches = ".+"
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GatewayOpenApiFlywayPostgresqlIT {

    private final String jdbcUrl = requiredEnvironment(
            "YUHENG_OPENAPI_TEST_POSTGRES_URL"
    );
    private final String user = requiredEnvironment(
            "YUHENG_OPENAPI_TEST_POSTGRES_USER"
    );
    private final String password = requiredEnvironment(
            "YUHENG_OPENAPI_TEST_POSTGRES_PASSWORD"
    );
    private final String schema = "gateway_openapi_"
            + UUID.randomUUID().toString().replace("-", "");

    @BeforeAll
    void createSchema() throws SQLException {
        execute("CREATE SCHEMA " + schema);
    }

    @AfterAll
    void dropSchema() throws SQLException {
        execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
    }

    @Test
    void migratesV1ThroughV13AndKeepsOpenApiPersistenceContracts()
            throws SQLException {
        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, user, password)
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
        assertThat(flyway.info().applied()).hasSize(13);
        assertThat(flyway.info().current().getVersion().getVersion())
                .isEqualTo("13");
        assertThat(tableNames()).contains(
                "gateway_openapi_snapshot",
                "gateway_openapi_sync_state"
        );
        assertThat(indexNames()).contains(
                "uk_gateway_openapi_snapshot_contract",
                "idx_gateway_openapi_snapshot_definition",
                "uk_gateway_openapi_sync_key",
                "idx_gateway_openapi_sync_due",
                "idx_gateway_openapi_sync_app"
        );
        assertThat(indexIsUnique("idx_gateway_openapi_snapshot_definition"))
                .isFalse();

        assertThat(indexIsUnique("uk_gateway_openapi_snapshot_contract"))
                .isTrue();
        assertThat(indexDefinition("uk_gateway_openapi_snapshot_contract"))
                .contains("gateway_openapi_snapshot")
                .contains("(application_id, build_id, openapi_group, "
                        + "canonical_sha256)");

        assertThat(indexIsUnique("uk_gateway_openapi_sync_key")).isTrue();
        assertThat(indexDefinition("uk_gateway_openapi_sync_key"))
                .contains("gateway_openapi_sync_state")
                .contains("(application_id, build_id, openapi_group)");

        assertThat(indexIsUnique("idx_gateway_openapi_sync_due")).isFalse();
        assertThat(indexDefinition("idx_gateway_openapi_sync_due"))
                .contains("(next_retry_at, id)")
                .contains("WHERE")
                .contains(
                        "DISCOVERED",
                        "FETCH_FAILED",
                        "INGEST_FAILED",
                        "STALE"
                );
    }

    private Set<String> tableNames() throws SQLException {
        return values("""
                SELECT table_name
                  FROM information_schema.tables
                 WHERE table_schema = '%s'
                """.formatted(schema));
    }

    private Set<String> indexNames() throws SQLException {
        return values("""
                SELECT indexname
                  FROM pg_indexes
                 WHERE schemaname = '%s'
                """.formatted(schema));
    }

    private boolean indexIsUnique(String indexName) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT i.indisunique
                       FROM pg_index i
                       JOIN pg_class c ON c.oid = i.indexrelid
                       JOIN pg_namespace n ON n.oid = c.relnamespace
                      WHERE n.nspname = ? AND c.relname = ?
                     """)) {
            statement.setString(1, schema);
            statement.setString(2, indexName);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalStateException(
                            "index not found: " + indexName
                    );
                }
                return result.getBoolean(1);
            }
        }
    }

    private String indexDefinition(String indexName) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT indexdef
                       FROM pg_indexes
                      WHERE schemaname = ? AND indexname = ?
                     """)) {
            statement.setString(1, schema);
            statement.setString(2, indexName);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalStateException(
                            "index not found: " + indexName
                    );
                }
                return result.getString(1).replaceAll("\\s+", " ");
            }
        }
    }

    private Set<String> values(String sql) throws SQLException {
        try (Connection connection = connection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            Set<String> values = new HashSet<>();
            while (result.next()) {
                values.add(result.getString(1));
            }
            return Set.copyOf(values);
        }
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = connection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, user, password);
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }
}
