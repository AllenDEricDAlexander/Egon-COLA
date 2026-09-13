package top.egon.cola.archetype.source.web.infrastructure.config.datasource;

import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.apache.shardingsphere.infra.hint.HintManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/** Opt-in proof against an explicitly supplied PostgreSQL primary and streaming replica. */
@EnabledIfSystemProperty(named = "egon.pg.readwrite", matches = "true")
class PostgreSqlReadwriteIntegrationTest {
    private DriverManagerDataSource primary;
    private DriverManagerDataSource replica;
    private DataSource logical;
    private String schema;
    private boolean created;

    @BeforeEach
    void prepareIsolatedSchema() throws Exception {
        primary = physical("EGON_TEST_PG_PRIMARY_URL");
        replica = physical("EGON_TEST_PG_REPLICA_URL");
        try (var connection = primary.getConnection()) {
            assertThat(recovery(connection)).isFalse();
        }
        try (var connection = replica.getConnection()) {
            assertThat(recovery(connection)).isTrue();
        }
        schema = "egon_rw_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = primary.getConnection(); var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
            created = true;
            connection.setSchema(schema);
            ScriptUtils.executeSqlScript(connection, new org.springframework.core.io.support.EncodedResource(new ClassPathResource("sharding/pgsql-routing-fixture.sql"), java.nio.charset.StandardCharsets.UTF_8), false, false, "--", ScriptUtils.EOF_STATEMENT_SEPARATOR, "/*", "*/");
        }
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        boolean visible = false;
        while (Instant.now().isBefore(deadline)) {
            try (var connection = replica.getConnection(); var statement = connection.prepareStatement("SELECT to_regclass(?) IS NOT NULL")) {
                statement.setQueryTimeout(2);
                statement.setString(1, schema + ".routing_probe");
                try (var rows = statement.executeQuery()) { rows.next(); visible = rows.getBoolean(1); }
            }
            if (visible) { break; }
            Thread.sleep(100);
        }
        assertThat(visible).as("the configured replica must replay the test schema; no replica is provisioned by this test").isTrue();
        primary.setUrl(primary.getUrl() + (primary.getUrl().contains("?") ? "&" : "?") + "currentSchema=" + schema);
        replica.setUrl(replica.getUrl() + (replica.getUrl().contains("?") ? "&" : "?") + "currentSchema=" + schema);
        String yaml = """
                databaseName: readwrite_contract
                rules:
                  - !READWRITE_SPLITTING
                    dataSourceGroups:
                      probe:
                        writeDataSourceName: primary
                        readDataSourceNames: [replica]
                        transactionalReadQueryStrategy: PRIMARY
                        loadBalancerName: round_robin
                    loadBalancers:
                      round_robin:
                        type: ROUND_ROBIN
                  - !SINGLE
                    tables: [probe.%s.routing_probe]
                transaction:
                  defaultType: LOCAL
                props:
                  sql-show: false
                """.formatted(schema);
        logical = YamlShardingSphereDataSourceFactory.createDataSource(
                Map.of("primary", primary, "replica", replica), yaml.getBytes(StandardCharsets.UTF_8));
    }

    @AfterEach
    void removeOnlyOwnedSchema() throws Exception {
        try {
            if (logical instanceof AutoCloseable closeable) { closeable.close(); }
        } finally {
            if (created) {
                try (var connection = primary.getConnection(); var statement = connection.createStatement()) {
                    statement.execute("DROP SCHEMA " + schema + " CASCADE");
                }
            }
        }
    }

    @Test
    void routesOrdinaryReadsToReplicaAndTransactionalReadsToPrimary() throws Exception {
        try (var connection = logical.getConnection()) {
            assertThat(probe(connection, false)).isTrue();
        }
        try (var connection = logical.getConnection()) {
            connection.setAutoCommit(false);
            try {
                assertThat(probe(connection, false)).isFalse();
                assertThat(probe(connection, true)).isFalse();
                try (var statement = connection.prepareStatement("UPDATE routing_probe SET payload = 'rolled-back', version = version + 1 WHERE tenant_id = 41 AND id = 1 AND version = 0 AND deleted_at IS NULL")) {
                    assertThat(statement.executeUpdate()).isEqualTo(1);
                    assertThat(statement.executeUpdate()).isZero();
                }
            } finally { connection.rollback(); }
        }
        try (var connection = primary.getConnection(); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT payload, version FROM routing_probe WHERE tenant_id = 41 AND id = 1")) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getString(1)).isEqualTo("active-a");
            assertThat(rows.getLong(2)).isZero();
        }
    }

    @Test
    void lockingAndStrongReadsUsePrimaryAndHintDoesNotLeak() throws Exception {
        try (var hint = HintManager.getInstance()) {
            hint.setWriteRouteOnly();
            try (var connection = logical.getConnection()) { assertThat(probe(connection, false)).isFalse(); }
        }
        try (var connection = logical.getConnection()) { assertThat(probe(connection, false)).isTrue(); }
        try (var connection = logical.getConnection()) { assertThat(probe(connection, true)).isFalse(); }
    }

    @Test
    void unavailableReplicaFailsWithoutPromotingOrSilentlyReadingPrimary() throws Exception {
        org.mockito.Mockito.doThrow(new SQLException("replica unavailable"))
                .when(replica).getConnection();
        try {
            assertThatThrownBy(() -> {
                try (var connection = logical.getConnection()) { probe(connection, false); }
            }).isInstanceOf(SQLException.class);
            try (var hint = HintManager.getInstance()) {
                hint.setWriteRouteOnly();
                try (var connection = logical.getConnection()) { assertThat(probe(connection, false)).isFalse(); }
            }
        } finally { org.mockito.Mockito.reset(replica); }
    }

    private static boolean probe(Connection connection, boolean locking) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT pg_is_in_recovery() FROM routing_probe WHERE tenant_id = ? AND id = ? AND deleted_at IS NULL" + (locking ? " FOR UPDATE" : ""))) {
            statement.setQueryTimeout(5);
            statement.setLong(1, 41);
            statement.setLong(2, 1);
            try (var rows = statement.executeQuery()) {
                assertThat(rows.next()).isTrue();
                return rows.getBoolean(1);
            }
        }
    }

    private static boolean recovery(Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.setQueryTimeout(5);
            try (var rows = statement.executeQuery("SELECT pg_is_in_recovery()")) { rows.next(); return rows.getBoolean(1); }
        }
    }

    private static DriverManagerDataSource physical(String key) {
        String url = System.getenv(key);
        assertThat(url).as(key).startsWith("jdbc:postgresql:").doesNotContain("currentSchema=");
        String username = System.getenv("EGON_TEST_PG_USER");
        String password = System.getenv("EGON_TEST_PG_PASSWORD");
        assertThat(username).as("EGON_TEST_PG_USER").isNotBlank();
        assertThat(password).as("EGON_TEST_PG_PASSWORD must be explicitly supplied").isNotNull();
        return org.mockito.Mockito.spy(new DriverManagerDataSource(url, username, password));
    }
}
