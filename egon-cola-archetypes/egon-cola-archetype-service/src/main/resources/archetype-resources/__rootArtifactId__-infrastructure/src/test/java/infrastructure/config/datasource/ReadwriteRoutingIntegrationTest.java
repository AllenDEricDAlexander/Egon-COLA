#set( $symbol_dollar = '$' )
package ${package}.infrastructure.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.apache.shardingsphere.sharding.exception.audit.DMLWithoutShardingKeyException;
import org.junit.jupiter.api.Test;

class ReadwriteRoutingIntegrationTest {

    @Test
    void shouldKeepNoneTableOnMasterDataAndHonorTransactionReadRouting()
            throws Exception {
        Map<String, DataSource> physical = new LinkedHashMap<>();
        physical.put("master_data_primary", dataSource("evaluation-routing-master-primary"));
        physical.put("master_data_replica_0", dataSource("evaluation-routing-master-replica"));
        physical.put("shard_0_primary", dataSource("evaluation-routing-shard-0-primary"));
        physical.put("shard_0_replica_0", dataSource("evaluation-routing-shard-0-replica"));
        physical.put("shard_1_primary", dataSource("evaluation-routing-shard-1-primary"));
        physical.put("shard_1_replica_0", dataSource("evaluation-routing-shard-1-replica"));
        initializeCourses(physical.get("master_data_primary"), "primary");
        initializeCourses(physical.get("master_data_replica_0"), "replica");

        DataSource logical = YamlShardingSphereDataSourceFactory.createDataSource(
                physical, readwriteRule());
        try {
            registerTables(logical, "evaluation_route_probe", courseTable());

            assertThat(queryMarker(logical)).isEqualTo("replica");

            execute(logical, "INSERT INTO evaluation_course(id, marker) VALUES (2, 'write')");
            assertThat(queryCount(
                    physical.get("master_data_primary"),
                    "SELECT COUNT(*) FROM evaluation_course WHERE id = 2"))
                    .isOne();
            assertThat(queryCount(
                    physical.get("master_data_replica_0"),
                    "SELECT COUNT(*) FROM evaluation_course WHERE id = 2"))
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
    void shouldRejectShardedUpdateWithoutShardingCondition() throws Exception {
        Map<String, DataSource> physical = new LinkedHashMap<>();
        physical.put("shard_0", dataSource("evaluation-audit-shard-0"));
        physical.put("shard_1", dataSource("evaluation-audit-shard-1"));
        physical.values().forEach(ReadwriteRoutingIntegrationTest::initializeEvaluationTables);

        DataSource logical = YamlShardingSphereDataSourceFactory.createDataSource(
                physical, evaluationShardingRule());
        try {
            registerTables(
                    logical,
                    "evaluation_local_tx",
                    courseScheduleTable(),
                    examTable(),
                    examPaperTable(),
                    scoreTable());

            assertThatThrownBy(() -> execute(
                            logical,
                            "UPDATE evaluation_course_schedule SET id = id"))
                    .isInstanceOf(DMLWithoutShardingKeyException.class)
                    .satisfies(failure -> assertThat(failure.getMessage())
                            .containsIgnoringCase("sharding"));
        } finally {
            close(logical);
        }
    }

    @Test
    void shouldKeepScheduleAndExamFamilyWritesOnOneNodePerTransaction() throws Exception {
        Map<String, DataSource> physical = new LinkedHashMap<>();
        physical.put("shard_0", dataSource("evaluation-local-tx-shard-0"));
        physical.put("shard_1", dataSource("evaluation-local-tx-shard-1"));
        physical.values().forEach(ReadwriteRoutingIntegrationTest::initializeEvaluationTables);

        DataSource logical = YamlShardingSphereDataSourceFactory.createDataSource(
                physical, evaluationShardingRule());
        Long courseId = 1001L;
        Long scheduleId = 3001L;
        Long examId = 4001L;
        Long paperId = 5001L;
        Long scoreId = 7001L;
        try {
            registerTables(
                    logical,
                    "evaluation_local_tx",
                    courseScheduleTable(),
                    examTable(),
                    examPaperTable(),
                    scoreTable());

            try (Connection connection = logical.getConnection()) {
                connection.setAutoCommit(false);
                execute(connection, "INSERT INTO evaluation_course_schedule(id, course_id, tenant_id) VALUES ("
                        + scheduleId + ", " + courseId + ", 2001)");
                connection.commit();
            }
            assertThat(writtenDataSources(
                    physical,
                    List.of("evaluation_course_schedule"),
                    List.of(scheduleId)))
                    .hasSize(1);

            try (Connection connection = logical.getConnection()) {
                connection.setAutoCommit(false);
                execute(connection, "INSERT INTO evaluation_exam(id, tenant_id) VALUES (" + examId + ", 2001)");
                execute(connection, "INSERT INTO evaluation_exam_paper(id, exam_id, tenant_id) VALUES ("
                        + paperId + ", " + examId + ", 2001)");
                execute(connection, "INSERT INTO evaluation_score(id, exam_id, tenant_id) VALUES ("
                        + scoreId + ", " + examId + ", 2001)");
                connection.commit();
            }
            assertThat(writtenDataSources(
                    physical,
                    List.of("evaluation_exam", "evaluation_exam_paper", "evaluation_score"),
                    List.of(examId, paperId, scoreId)))
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

    private static void initializeCourses(DataSource dataSource, String marker) {
        executeUnchecked(
                dataSource,
                "CREATE TABLE evaluation_course(id INTEGER PRIMARY KEY, marker VARCHAR(32))");
        executeUnchecked(
                dataSource,
                "INSERT INTO evaluation_course(id, marker) VALUES (1, '" + marker + "')");
    }

    private static void initializeEvaluationTables(DataSource dataSource) {
        for (int suffix = 0; suffix < 2; suffix++) {
            executeUnchecked(
                    dataSource,
                    "CREATE TABLE evaluation_course_schedule_" + suffix
                            + "(id BIGINT PRIMARY KEY, course_id BIGINT NOT NULL, tenant_id BIGINT NOT NULL)");
            executeUnchecked(
                    dataSource,
                    "CREATE TABLE evaluation_exam_" + suffix + "(id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL)");
            executeUnchecked(
                    dataSource,
                    "CREATE TABLE evaluation_exam_paper_" + suffix
                            + "(id BIGINT PRIMARY KEY, exam_id BIGINT NOT NULL, tenant_id BIGINT NOT NULL)");
            executeUnchecked(
                    dataSource,
                    "CREATE TABLE evaluation_score_" + suffix
                            + "(id BIGINT PRIMARY KEY, exam_id BIGINT NOT NULL, tenant_id BIGINT NOT NULL)");
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

    private static ShardingSphereTable courseTable() {
        return table(
                "evaluation_course",
                column("id", Types.INTEGER, true, false),
                column("marker", Types.VARCHAR, false, true));
    }

    private static ShardingSphereTable courseScheduleTable() {
        return table(
                "evaluation_course_schedule",
                column("id", Types.BIGINT, true, false),
                column("course_id", Types.BIGINT, false, false),
                column("tenant_id", Types.BIGINT, false, false));
    }

    private static ShardingSphereTable examTable() {
        return table("evaluation_exam", column("id", Types.BIGINT, true, false),
                column("tenant_id", Types.BIGINT, false, false));
    }

    private static ShardingSphereTable examPaperTable() {
        return table(
                "evaluation_exam_paper",
                column("id", Types.BIGINT, true, false),
                column("exam_id", Types.BIGINT, false, false),
                column("tenant_id", Types.BIGINT, false, false));
    }

    private static ShardingSphereTable scoreTable() {
        return table(
                "evaluation_score",
                column("id", Types.BIGINT, true, false),
                column("exam_id", Types.BIGINT, false, false),
                column("tenant_id", Types.BIGINT, false, false));
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
            Collection<?> ids) throws SQLException {
        Set<String> result = new LinkedHashSet<>();
        for (Map.Entry<String, DataSource> entry : dataSources.entrySet()) {
            int count = 0;
            for (String tablePrefix : tablePrefixes) {
                for (int suffix = 0; suffix < 2; suffix++) {
                    for (Object id : ids) {
                        count += queryCount(
                                entry.getValue(),
                                "SELECT COUNT(*) FROM " + tablePrefix + "_" + suffix
                                        + " WHERE id = " + id);
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
                        "SELECT marker FROM evaluation_course WHERE id = 1")) {
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
                databaseName: evaluation_route_probe
                rules:
                  - !READWRITE_SPLITTING
                    dataSourceGroups:
                      master_data:
                        writeDataSourceName: master_data_primary
                        readDataSourceNames:
                          - master_data_replica_0
                        transactionalReadQueryStrategy: PRIMARY
                        loadBalancerName: round_robin
                      shard_0:
                        writeDataSourceName: shard_0_primary
                        readDataSourceNames:
                          - shard_0_replica_0
                        transactionalReadQueryStrategy: PRIMARY
                        loadBalancerName: round_robin
                      shard_1:
                        writeDataSourceName: shard_1_primary
                        readDataSourceNames:
                          - shard_1_replica_0
                        transactionalReadQueryStrategy: PRIMARY
                        loadBalancerName: round_robin
                    loadBalancers:
                      round_robin:
                        type: ROUND_ROBIN
                  - !SHARDING
                    tables:
                      evaluation_course:
                        actualDataNodes: master_data.evaluation_course
                        databaseStrategy:
                          none:
                        tableStrategy:
                          none:
                props:
                  sql-show: false
                """.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static byte[] evaluationShardingRule() {
        return """
                databaseName: evaluation_local_tx
                rules:
                  - !SHARDING
                    tables:
                      evaluation_course_schedule:
                        actualDataNodes: shard_$->{0..1}.evaluation_course_schedule_$->{0..1}
                        databaseStrategy:
                          standard:
                            shardingColumn: tenant_id
                            shardingAlgorithmName: tenant_long_database_bucket
                        tableStrategy:
                          standard:
                            shardingColumn: tenant_id
                            shardingAlgorithmName: tenant_long_table_bucket
                        auditStrategy:
                          auditorNames:
                            - sharding_key_required_auditor
                          allowHintDisable: false
                      evaluation_exam:
                        actualDataNodes: shard_$->{0..1}.evaluation_exam_$->{0..1}
                        databaseStrategy:
                          standard:
                            shardingColumn: tenant_id
                            shardingAlgorithmName: tenant_long_database_bucket
                        tableStrategy:
                          standard:
                            shardingColumn: tenant_id
                            shardingAlgorithmName: tenant_long_table_bucket
                        auditStrategy:
                          auditorNames:
                            - sharding_key_required_auditor
                          allowHintDisable: false
                      evaluation_exam_paper:
                        actualDataNodes: shard_$->{0..1}.evaluation_exam_paper_$->{0..1}
                        databaseStrategy:
                          standard:
                            shardingColumn: tenant_id
                            shardingAlgorithmName: tenant_long_database_bucket
                        tableStrategy:
                          standard:
                            shardingColumn: tenant_id
                            shardingAlgorithmName: tenant_long_table_bucket
                        auditStrategy:
                          auditorNames:
                            - sharding_key_required_auditor
                          allowHintDisable: false
                      evaluation_score:
                        actualDataNodes: shard_$->{0..1}.evaluation_score_$->{0..1}
                        databaseStrategy:
                          standard:
                            shardingColumn: tenant_id
                            shardingAlgorithmName: tenant_long_database_bucket
                        tableStrategy:
                          standard:
                            shardingColumn: tenant_id
                            shardingAlgorithmName: tenant_long_table_bucket
                        auditStrategy:
                          auditorNames:
                            - sharding_key_required_auditor
                          allowHintDisable: false
                    bindingTables:
                      - evaluation_exam,evaluation_exam_paper,evaluation_score
                    shardingAlgorithms:
                      tenant_long_database_bucket:
                        type: CLASS_BASED
                        props:
                          strategy: STANDARD
                          algorithmClassName: ${package}.infrastructure.config.datasource.LongTenantShardingAlgorithm
                          target: database
                          node-count: 4
                          node-map: 0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1
                      tenant_long_table_bucket:
                        type: CLASS_BASED
                        props:
                          strategy: STANDARD
                          algorithmClassName: ${package}.infrastructure.config.datasource.LongTenantShardingAlgorithm
                          target: table
                          node-count: 4
                          node-map: 0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1
                    auditors:
                      sharding_key_required_auditor:
                        type: DML_SHARDING_CONDITIONS
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
