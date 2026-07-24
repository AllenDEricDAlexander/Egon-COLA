#set( $symbol_dollar = '$' )
package ${package}.infrastructure.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariDataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.apache.shardingsphere.infra.metadata.database.schema.model.ShardingSphereColumn;
import org.apache.shardingsphere.infra.metadata.database.schema.model.ShardingSphereTable;
import org.apache.shardingsphere.mode.manager.ContextManager;
import org.junit.jupiter.api.Test;

class ReadwriteRoutingIntegrationTest {

    @Test
    void shouldRouteQueriesAndWritesAccordingToTransactionBoundary() throws Exception {
        DataSource primary = dataSource("organization-routing-primary");
        DataSource replica = dataSource("organization-routing-replica");
        initializeRouteProbe(primary, "primary");
        initializeRouteProbe(replica, "replica");

        DataSource logical = YamlShardingSphereDataSourceFactory.createDataSource(
                Map.of("single_primary", primary, "single_replica_0", replica),
                readwriteRule());
        try {
            registerTables(logical, "organization_route_probe", routeProbeTable());

            assertThat(queryMarker(logical)).isEqualTo("replica");

            execute(logical, "INSERT INTO route_probe(id, marker) VALUES (2, 'write')");
            assertThat(queryCount(primary, "SELECT COUNT(*) FROM route_probe WHERE id = 2"))
                    .isOne();
            assertThat(queryCount(replica, "SELECT COUNT(*) FROM route_probe WHERE id = 2"))
                    .isZero();

            try (Connection connection = logical.getConnection()) {
                connection.setAutoCommit(false);
                assertThat(queryMarker(connection)).isEqualTo("primary");
                connection.rollback();
            }
        } finally {
            close(logical);
        }
    }

    @Test
    void shouldWriteClassAndMembershipToOnePhysicalDataSource() throws Exception {
        Map<String, DataSource> physical = new LinkedHashMap<>();
        physical.put("shard_0", dataSource("organization-local-tx-shard-0"));
        physical.put("shard_1", dataSource("organization-local-tx-shard-1"));
        physical.values().forEach(ReadwriteRoutingIntegrationTest::initializeClassTables);

        DataSource logical = YamlShardingSphereDataSourceFactory.createDataSource(
                physical, classShardingRule());
        String gradeId = "019ba346-0000-7000-8000-000000000201";
        String schoolClassId = "019ba346-0000-7000-8000-000000000202";
        String membershipId = "019ba346-0000-7000-8000-000000000203";
        try {
            registerTables(
                    logical,
                    "organization_local_tx",
                    schoolClassTable(),
                    schoolClassUserTable());
            try (Connection connection = logical.getConnection()) {
                connection.setAutoCommit(false);
                execute(connection, "INSERT INTO school_classes(id, grade_id) VALUES ('"
                        + schoolClassId + "', '" + gradeId + "')");
                execute(connection, "INSERT INTO school_class_users(id, grade_id)"
                        + " VALUES ('" + membershipId + "', '" + gradeId + "')");
                connection.commit();
            }

            assertThat(writtenDataSources(
                    physical,
                    List.of("school_classes", "school_class_users"),
                    List.of(schoolClassId, membershipId)))
                    .hasSize(1);
        } finally {
            close(logical);
        }
    }

    private static DataSource dataSource(String database) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setPoolName(database);
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setJdbcUrl("jdbc:h2:mem:" + database
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private static void initializeRouteProbe(DataSource dataSource, String marker) {
        executeUnchecked(
                dataSource,
                "CREATE TABLE route_probe(id INTEGER PRIMARY KEY, marker VARCHAR(32))");
        executeUnchecked(
                dataSource,
                "INSERT INTO route_probe(id, marker) VALUES (1, '" + marker + "')");
    }

    private static void initializeClassTables(DataSource dataSource) {
        for (int suffix = 0; suffix < 2; suffix++) {
            executeUnchecked(
                    dataSource,
                    "CREATE TABLE school_classes_" + suffix
                            + "(id VARCHAR(36) PRIMARY KEY, grade_id VARCHAR(36) NOT NULL)");
            executeUnchecked(
                    dataSource,
                    "CREATE TABLE school_class_users_" + suffix
                            + "(id VARCHAR(36) PRIMARY KEY, grade_id VARCHAR(36) NOT NULL)");
        }
    }

    /**
     * H2 is used only as a lightweight route probe and is not a supported
     * ShardingSphere storage type, so the test supplies the minimal logical metadata.
     */
    private static void registerTables(
            DataSource logical,
            String databaseName,
            ShardingSphereTable... tables) throws ReflectiveOperationException {
        Field field = logical.getClass().getDeclaredField("contextManager");
        field.setAccessible(true);
        ContextManager manager = (ContextManager) field.get(logical);
        var database = manager.getDatabase(databaseName);
        var schema = database.getAllSchemas().stream()
                .filter(candidate -> candidate.getName().equals(databaseName))
                .findFirst()
                .orElseThrow();
        for (ShardingSphereTable table : tables) {
            schema.putTable(table);
        }
    }

    private static ShardingSphereTable routeProbeTable() {
        return table(
                "route_probe",
                column("id", Types.INTEGER, true, false),
                column("marker", Types.VARCHAR, false, true));
    }

    private static ShardingSphereTable schoolClassTable() {
        return table(
                "school_classes",
                column("id", Types.VARCHAR, true, false),
                column("grade_id", Types.VARCHAR, false, false));
    }

    private static ShardingSphereTable schoolClassUserTable() {
        return table(
                "school_class_users",
                column("id", Types.VARCHAR, true, false),
                column("grade_id", Types.VARCHAR, false, false));
    }

    private static ShardingSphereTable table(
            String name,
            ShardingSphereColumn... columns) {
        return new ShardingSphereTable(
                name,
                List.of(columns),
                List.of(),
                List.of());
    }

    private static ShardingSphereColumn column(
            String name,
            int type,
            boolean primaryKey,
            boolean nullable) {
        return new ShardingSphereColumn(
                name,
                type,
                primaryKey,
                false,
                type == Types.VARCHAR,
                true,
                false,
                nullable);
    }

    private static Set<String> writtenDataSources(
            Map<String, DataSource> dataSources,
            Collection<String> tablePrefixes,
            Collection<String> ids) throws SQLException {
        Set<String> result = new LinkedHashSet<>();
        for (Map.Entry<String, DataSource> entry : dataSources.entrySet()) {
            int count = 0;
            for (String tablePrefix : tablePrefixes) {
                for (int suffix = 0; suffix < 2; suffix++) {
                    for (String id : ids) {
                        count += queryCount(
                                entry.getValue(),
                                "SELECT COUNT(*) FROM " + tablePrefix + "_" + suffix
                                        + " WHERE id = '" + id + "'");
                    }
                }
            }
            if (count > 0) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    private static String queryMarker(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return queryMarker(connection);
        }
    }

    private static String queryMarker(Connection connection) throws SQLException {
        try (var statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT marker FROM route_probe WHERE id = 1")) {
            assertThat(result.next()).isTrue();
            return result.getString(1);
        }
    }

    private static int queryCount(DataSource dataSource, String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                var statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            return result.getInt(1);
        }
    }

    private static void executeUnchecked(DataSource dataSource, String sql) {
        try {
            execute(dataSource, sql);
        } catch (SQLException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static void execute(DataSource dataSource, String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            execute(connection, sql);
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static byte[] readwriteRule() {
        return """
                databaseName: organization_route_probe
                rules:
                  - !READWRITE_SPLITTING
                    dataSourceGroups:
                      single:
                        writeDataSourceName: single_primary
                        readDataSourceNames:
                          - single_replica_0
                        transactionalReadQueryStrategy: PRIMARY
                        loadBalancerName: round_robin
                    loadBalancers:
                      round_robin:
                        type: ROUND_ROBIN
                  - !SINGLE
                    tables:
                      - single.route_probe
                    defaultDataSource: single
                props:
                  sql-show: false
                """.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static byte[] classShardingRule() {
        return """
                databaseName: organization_local_tx
                rules:
                  - !SHARDING
                    tables:
                      school_classes:
                        actualDataNodes: shard_$->{0..1}.school_classes_$->{0..1}
                        databaseStrategy:
                          standard:
                            shardingColumn: grade_id
                            shardingAlgorithmName: uuid_v7_database_bucket
                        tableStrategy:
                          standard:
                            shardingColumn: grade_id
                            shardingAlgorithmName: uuid_v7_table_bucket
                      school_class_users:
                        actualDataNodes: shard_$->{0..1}.school_class_users_$->{0..1}
                        databaseStrategy:
                          standard:
                            shardingColumn: grade_id
                            shardingAlgorithmName: uuid_v7_database_bucket
                        tableStrategy:
                          standard:
                            shardingColumn: grade_id
                            shardingAlgorithmName: uuid_v7_table_bucket
                    bindingTables:
                      - school_classes,school_class_users
                    shardingAlgorithms:
                      uuid_v7_database_bucket:
                        type: CLASS_BASED
                        props:
                          strategy: STANDARD
                          algorithmClassName: ${package}.infrastructure.config.datasource.UuidV7BucketShardingAlgorithm
                          target: database
                          mapping-version: 1
                          node-count: 4
                          node-map: 0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1
                      uuid_v7_table_bucket:
                        type: CLASS_BASED
                        props:
                          strategy: STANDARD
                          algorithmClassName: ${package}.infrastructure.config.datasource.UuidV7BucketShardingAlgorithm
                          target: table
                          mapping-version: 1
                          node-count: 4
                          node-map: 0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1
                props:
                  sql-show: false
                """.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static void close(DataSource dataSource) throws Exception {
        if (dataSource instanceof AutoCloseable closeable) {
            closeable.close();
        }
    }
}
