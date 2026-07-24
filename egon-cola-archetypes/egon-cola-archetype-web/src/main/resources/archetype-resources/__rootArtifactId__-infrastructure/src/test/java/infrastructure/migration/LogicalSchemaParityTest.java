package ${package}.infrastructure.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class LogicalSchemaParityTest {

    @Test
    void shouldKeepDefaultAndShardingModesLogicallyEquivalent() throws Exception {
        Map<String, List<String>> defaultSchema = migrateAndRead(
                "organization-parity-default", "classpath:db/migration/default");
        Map<String, List<String>> singleSchema = migrateAndRead(
                "organization-parity-single", "classpath:db/migration/sharding/single");
        Map<String, List<String>> shardZeroSchema = migrateAndRead(
                "organization-parity-shard-0", "classpath:db/migration/sharding/shard");
        Map<String, List<String>> shardOneSchema = migrateAndRead(
                "organization-parity-shard-1", "classpath:db/migration/sharding/shard");

        assertThat(shardOneSchema).isEqualTo(shardZeroSchema);
        Map<String, List<String>> shardingSchema = new TreeMap<>(singleSchema);
        mergeSchema(shardingSchema, shardZeroSchema);
        assertThat(shardingSchema).isEqualTo(defaultSchema);
    }

    private static Map<String, List<String>> migrateAndRead(
            String database, String location) throws Exception {
        String url = h2Url(database);
        Flyway flyway = Flyway.configure()
                .dataSource(url, "sa", "")
                .locations(location)
                .validateMigrationNaming(true)
                .load();
        flyway.migrate();
        flyway.validate();
        return logicalSchema(url);
    }

    private static Map<String, List<String>> logicalSchema(String url) throws SQLException {
        String sql = """
                SELECT table_name, column_name, data_type, is_nullable
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name <> 'flyway_schema_history'
                ORDER BY table_name, ordinal_position
                """;
        Map<String, List<String>> physicalSchema = new LinkedHashMap<>();
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             ResultSet resultSet = connection.createStatement().executeQuery(sql)) {
            while (resultSet.next()) {
                physicalSchema.computeIfAbsent(resultSet.getString("table_name"),
                                ignored -> new ArrayList<>())
                        .add(resultSet.getString("column_name")
                                + "|" + resultSet.getString("data_type")
                                + "|" + resultSet.getString("is_nullable"));
            }
        }

        Map<String, List<String>> logicalSchema = new TreeMap<>();
        physicalSchema.forEach((table, columns) ->
                mergeTable(logicalSchema, logicalTableName(table), List.copyOf(columns)));
        return logicalSchema;
    }

    private static String logicalTableName(String physicalTable) {
        return physicalTable.replaceFirst("_[01]$", "");
    }

    private static void mergeSchema(
            Map<String, List<String>> target, Map<String, List<String>> source) {
        source.forEach((table, columns) -> mergeTable(target, table, columns));
    }

    private static void mergeTable(
            Map<String, List<String>> schema, String table, List<String> columns) {
        List<String> existing = schema.putIfAbsent(table, columns);
        if (existing != null) {
            assertThat(columns)
                    .as("同一逻辑表的各物理分表必须具有相同列定义：%s", table)
                    .isEqualTo(existing);
        }
    }

    private static String h2Url(String database) {
        return "jdbc:h2:mem:" + database
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    }
}
