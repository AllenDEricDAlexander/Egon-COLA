package ${package}.infrastructure.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void shouldRejectMembershipWhoseGradeDoesNotMatchItsSchoolClass() throws Exception {
        String url = h2Url("organization-membership-foreign-key");
        migrate(url, "classpath:db/migration/sharding/shard");
        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            connection.createStatement().executeUpdate("""
                    INSERT INTO school_classes_0(
                        id, name, grade_name, grade_id, status, created_at)
                    VALUES (
                        '019ba346-0000-7000-8000-000000000101',
                        'Class A',
                        'Grade A',
                        '019ba346-0000-7000-8000-000000000102',
                        'ACTIVE',
                        CURRENT_TIMESTAMP)
                    """);

            assertThatThrownBy(() -> connection.createStatement().executeUpdate("""
                    INSERT INTO school_class_users_0(
                        id, grade_id, user_id, school_class_id, created_at)
                    VALUES (
                        '019ba346-0000-7000-8000-000000000103',
                        '019ba346-0000-7000-8000-000000000104',
                        '019ba346-0000-7000-8000-000000000105',
                        '019ba346-0000-7000-8000-000000000101',
                        CURRENT_TIMESTAMP)
                    """))
                    .isInstanceOf(SQLException.class);
        }
    }

    private static Map<String, List<String>> migrateAndRead(
            String database, String location) throws Exception {
        String url = h2Url(database);
        migrate(url, location);
        return logicalSchema(url);
    }

    private static void migrate(String url, String location) {
        Flyway flyway = Flyway.configure()
                .dataSource(url, "sa", "")
                .locations(location)
                .validateMigrationNaming(true)
                .load();
        flyway.migrate();
        flyway.validate();
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
        return physicalTable.replaceFirst("_[0-9]+$", "");
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
