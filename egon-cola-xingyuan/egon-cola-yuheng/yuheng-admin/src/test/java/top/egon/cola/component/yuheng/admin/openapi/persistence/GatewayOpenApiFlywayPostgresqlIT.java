package top.egon.cola.component.yuheng.admin.openapi.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import top.egon.cola.component.yuheng.admin.openapi.domain.enums.GatewayOpenApiSyncStateEnum;
import top.egon.cola.component.yuheng.admin.openapi.domain.po.GatewayOpenApiSnapshotPO;
import top.egon.cola.component.yuheng.admin.openapi.domain.po.GatewayOpenApiSyncPO;
import top.egon.cola.component.yuheng.admin.openapi.repository.jdbc.JdbcGatewayOpenApiSnapshotRepository;
import top.egon.cola.component.yuheng.admin.openapi.repository.jdbc.JdbcGatewayOpenApiSyncRepository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(
        named = "YUHENG_OPENAPI_TEST_POSTGRES_URL",
        matches = ".+"
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GatewayOpenApiFlywayPostgresqlIT {

    private static final Instant NOW = Instant.parse(
            "2026-08-26T03:00:00Z"
    );

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
    void migratesV1ThroughV12AndPersistsAggregateSnapshotLinksAndCas()
            throws SQLException {
        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, user, password)
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
        assertThat(flyway.info().applied()).hasSize(12);
        assertThat(flyway.info().current().getVersion().getVersion())
                .isEqualTo("12");
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

        JdbcTemplate jdbc = jdbc();
        insertParents(jdbc);
        JdbcGatewayOpenApiSnapshotRepository snapshots =
                new JdbcGatewayOpenApiSnapshotRepository(
                        jdbc,
                        new ObjectMapper()
                );
        GatewayOpenApiSnapshotPO orders = snapshot(
                "snapshot-orders",
                "orders",
                'a',
                'b'
        );
        GatewayOpenApiSnapshotPO inventory = snapshot(
                "snapshot-inventory",
                "inventory",
                'c',
                'd'
        );
        snapshots.insertOrReuse(orders);
        snapshots.insertOrReuse(inventory);
        assertThat(snapshots.insertOrReuse(orders).id())
                .isEqualTo(orders.id());
        assertThat(snapshots.linkAllToDefinitionSet(
                List.of(orders.id(), inventory.id()),
                "set-http-1"
        )).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM gateway_openapi_snapshot "
                        + "WHERE definition_set_id = ?",
                Integer.class,
                "set-http-1"
        )).isEqualTo(2);

        JdbcGatewayOpenApiSyncRepository sync =
                new JdbcGatewayOpenApiSyncRepository(jdbc);
        GatewayOpenApiSyncPO syncRow = syncRow("sync-orders", "orders");
        assertThat(sync.upsertDiscovered(syncRow).id())
                .isEqualTo("sync-orders");
        assertThat(sync.upsertDiscovered(syncRow("sync-orders-retry", "orders"))
                .id())
                .isEqualTo("sync-orders");
        assertThat(sync.upsertDiscovered(syncRow("sync-inventory", "inventory"))
                .id())
                .isEqualTo("sync-inventory");
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM gateway_openapi_sync_state",
                Integer.class
        )).isEqualTo(2);
        assertThat(sync.claim("sync-orders", 0, NOW)).isTrue();
        assertThat(sync.claim("sync-orders", 0, NOW)).isFalse();
        assertThat(sync.transition(
                "sync-orders",
                1,
                GatewayOpenApiSyncStateEnum.FETCHING,
                GatewayOpenApiSyncStateEnum.VALIDATING,
                NOW
        )).isTrue();
        assertThat(sync.transition(
                "sync-orders",
                1,
                GatewayOpenApiSyncStateEnum.FETCHING,
                GatewayOpenApiSyncStateEnum.VALIDATING,
                NOW
        )).isFalse();
    }

    private GatewayOpenApiSnapshotPO snapshot(
            String id,
            String group,
            char documentHash,
            char canonicalHash) {
        return new GatewayOpenApiSnapshotPO(
                id,
                "app-1",
                null,
                "build-1",
                "5.3.3",
                group,
                "3.1.0",
                String.valueOf(documentHash).repeat(64),
                String.valueOf(canonicalHash).repeat(64),
                Map.of("openapi", "3.1.0"),
                "VALID",
                List.of(),
                1,
                1,
                "instance-1",
                NOW,
                NOW,
                NOW
        );
    }

    private GatewayOpenApiSyncPO syncRow(String id, String group) {
        return new GatewayOpenApiSyncPO(
                id,
                "app-1",
                "build-1",
                "5.3.3",
                group,
                "orders-service",
                "default",
                "1.0.0",
                GatewayOpenApiSyncStateEnum.DISCOVERED,
                null,
                null,
                null,
                0,
                null,
                null,
                NOW,
                null,
                null,
                null,
                0,
                NOW
        );
    }

    private void insertParents(JdbcTemplate jdbc) {
        Timestamp timestamp = Timestamp.from(NOW);
        jdbc.update("""
                INSERT INTO gateway_application(
                    id, biz_code, application_code, display_name, env,
                    namespace, revision, deleted, created_at, created_by,
                    updated_at, updated_by
                ) VALUES ('app-1', 'trade', 'orders', 'Orders', 'TEST',
                    'default', 0, FALSE, ?, 'test', ?, 'test')
                """, timestamp, timestamp);
        jdbc.update("""
                INSERT INTO gateway_definition_set(
                    id, application_id, report_id, build_id, protocol,
                    fingerprint, complete_set, status, operation_count,
                    accepted_count, conflict_count, received_at, completed_at
                ) VALUES ('set-http-1', 'app-1', 'report-1', 'build-1',
                    'HTTP', ?, TRUE, 'VERIFIED', 2, 2, 0, ?, ?)
                """, "e".repeat(64), timestamp, timestamp);
    }

    private JdbcTemplate jdbc() {
        String separator = jdbcUrl.contains("?") ? "&" : "?";
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                jdbcUrl + separator + "currentSchema=" + schema,
                user,
                password
        );
        return new JdbcTemplate(dataSource);
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
