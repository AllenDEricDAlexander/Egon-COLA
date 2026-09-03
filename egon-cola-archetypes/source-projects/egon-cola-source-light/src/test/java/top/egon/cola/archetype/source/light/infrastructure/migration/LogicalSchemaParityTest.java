package top.egon.cola.archetype.source.light.infrastructure.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LogicalSchemaParityTest {

    private static final String MASTER_DATA_LOCATION =
            "classpath:db/migration/sharding/master-data";
    private static final String SHARD_LOCATION = "classpath:db/migration/sharding/shard";

    @Test
    void shouldBuildEquivalentSchemaFromBaselineAndVersionedHistory(@TempDir Path temp)
            throws Exception {
        String baselineMasterDataUrl = migrate(
                "light-baseline-master-data", MASTER_DATA_LOCATION);
        String baselineShardUrl = migrate("light-baseline-shard", SHARD_LOCATION);

        Path versionedMasterDataLocation = copyVersionedMigrationsToTempLocation(
                temp, "master-data");
        Path versionedShardLocation = copyVersionedMigrationsToTempLocation(temp, "shard");
        String versionedMasterDataUrl = migrate(
                "light-versioned-master-data", filesystemLocation(versionedMasterDataLocation));
        String versionedShardUrl = migrate(
                "light-versioned-shard", filesystemLocation(versionedShardLocation));

        assertThat(appliedMigrationTypes(baselineMasterDataUrl))
                .as("master-data fresh schema 必须由一个 B baseline 建立")
                .containsExactly("TABLE", "SQL_BASELINE");
        assertThat(appliedMigrationTypes(baselineShardUrl))
                .as("shard fresh schema 必须由一个 B baseline 建立")
                .containsExactly("TABLE", "SQL_BASELINE");
        assertThat(schemaSnapshot(baselineMasterDataUrl))
                .as("master-data B schema 必须等价于 V-only schema")
                .isEqualTo(schemaSnapshot(versionedMasterDataUrl));
        assertThat(schemaSnapshot(baselineShardUrl))
                .as("shard B schema 必须等价于 V-only schema")
                .isEqualTo(schemaSnapshot(versionedShardUrl));

        validateAtLocation(versionedMasterDataUrl, MASTER_DATA_LOCATION);
        validateAtLocation(versionedShardUrl, SHARD_LOCATION);
    }

    @Test
    void shouldBuildOneCompleteLogicalSchemaFromMasterDataAndShards() throws Exception {
        Map<String, List<String>> masterDataSchema = migrateAndRead(
                "light-parity-master-data",
                MASTER_DATA_LOCATION);
        Map<String, List<String>> shardZeroSchema = migrateAndRead(
                "light-parity-shard-0", SHARD_LOCATION);
        Map<String, List<String>> shardOneSchema = migrateAndRead(
                "light-parity-shard-1", SHARD_LOCATION);

        assertThat(shardOneSchema).isEqualTo(shardZeroSchema);
        Map<String, List<String>> shardingSchema = new TreeMap<>(masterDataSchema);
        mergeSchema(shardingSchema, shardZeroSchema);
        assertThat(shardingSchema.keySet()).containsExactly(
                "class_course_schedules",
                "courses",
                "permissions",
                "role_permissions",
                "roles",
                "school_classes",
                "user_roles",
                "users");
    }

    private static Path copyVersionedMigrationsToTempLocation(Path temp, String role)
            throws Exception {
        Path source = Path.of(Objects.requireNonNull(
                LogicalSchemaParityTest.class.getClassLoader()
                        .getResource("db/migration/sharding/" + role),
                "migration resource for " + role).toURI());
        Path target = temp.resolve(role);
        Files.createDirectories(target);
        try (Stream<Path> paths = Files.list(source)) {
            for (Path migration : paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("V"))
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .toList()) {
                Files.copy(migration, target.resolve(migration.getFileName()),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return target;
    }

    private static String filesystemLocation(Path path) {
        return "filesystem:" + path.toAbsolutePath();
    }

    private static String migrate(String database, String location) throws Exception {
        String url = h2Url(database);
        Flyway flyway = Flyway.configure()
                .dataSource(url, "sa", "")
                .locations(location)
                .validateMigrationNaming(true)
                .load();
        flyway.migrate();
        flyway.validate();
        return url;
    }

    private static void validateAtLocation(String url, String location) {
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations(location)
                .validateMigrationNaming(true)
                .load()
                .validate();
    }

    private static List<String> appliedMigrationTypes(String url) throws SQLException {
        List<String> types = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
                ResultSet resultSet = connection.createStatement().executeQuery(
                        "SELECT type FROM flyway_schema_history ORDER BY installed_rank")) {
            while (resultSet.next()) {
                types.add(resultSet.getString("type"));
            }
        }
        return types;
    }

    private static Map<String, List<String>> migrateAndRead(
            String database, String location) throws Exception {
        return logicalSchema(migrate(database, location));
    }

    private static Map<String, List<String>> schemaSnapshot(String url) throws SQLException {
        Map<String, List<String>> snapshot = new TreeMap<>();
        TreeSet<String> tables = new TreeSet<>();
        Map<String, List<String>> constraintsByName = new TreeMap<>();

        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            String columnsSql = """
                    SELECT table_name, column_name, data_type, is_nullable,
                           ordinal_position, column_default
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND table_name <> 'flyway_schema_history'
                    ORDER BY table_name, ordinal_position
                    """;
            try (ResultSet resultSet = connection.createStatement().executeQuery(columnsSql)) {
                while (resultSet.next()) {
                    String table = resultSet.getString("table_name");
                    tables.add(table);
                    snapshot.computeIfAbsent("column:" + table, ignored -> new ArrayList<>())
                            .add(resultSet.getInt("ordinal_position")
                                    + "|" + resultSet.getString("column_name")
                                    + "|" + resultSet.getString("data_type")
                                    + "|" + resultSet.getString("is_nullable")
                                    + "|" + resultSet.getString("column_default"));
                }
            }

            String indexesSql = """
                    SELECT i.table_name, i.index_name, i.index_type_name,
                           ic.column_name, ic.ordinal_position
                    FROM information_schema.indexes i
                    JOIN information_schema.index_columns ic
                      ON i.index_schema = ic.index_schema
                     AND i.index_name = ic.index_name
                     AND i.table_name = ic.table_name
                    WHERE i.index_schema = 'public'
                      AND i.is_generated = FALSE
                    ORDER BY i.table_name, i.index_name, ic.ordinal_position
                    """;
            try (ResultSet resultSet = connection.createStatement().executeQuery(indexesSql)) {
                while (resultSet.next()) {
                    String key = "index:" + resultSet.getString("table_name")
                            + ":" + resultSet.getString("index_name");
                    snapshot.computeIfAbsent(key, ignored -> new ArrayList<>())
                            .add(resultSet.getString("index_type_name")
                                    + "|" + resultSet.getInt("ordinal_position")
                                    + "|" + resultSet.getString("column_name"));
                }
            }

            String constraintsSql = """
                    SELECT tc.table_name, tc.constraint_name, tc.constraint_type,
                           kcu.column_name, kcu.ordinal_position
                    FROM information_schema.table_constraints tc
                    LEFT JOIN information_schema.key_column_usage kcu
                      ON tc.constraint_schema = kcu.constraint_schema
                     AND tc.constraint_name = kcu.constraint_name
                     AND tc.table_name = kcu.table_name
                    WHERE tc.constraint_schema = 'public'
                      AND tc.table_name <> 'flyway_schema_history'
                    ORDER BY tc.table_name, tc.constraint_name, kcu.ordinal_position
                    """;
            try (ResultSet resultSet = connection.createStatement().executeQuery(constraintsSql)) {
                while (resultSet.next()) {
                    String key = resultSet.getString("table_name")
                            + "|" + resultSet.getString("constraint_type")
                            + "|" + resultSet.getString("constraint_name");
                    String column = resultSet.getString("column_name");
                    if (column != null) {
                        constraintsByName.computeIfAbsent(key, ignored -> new ArrayList<>())
                                .add(resultSet.getInt("ordinal_position") + "|" + column);
                    }
                }
            }

            for (Map.Entry<String, List<String>> entry : constraintsByName.entrySet()) {
                String[] keyParts = entry.getKey().split("\\|", 3);
                String columns = String.join(",", entry.getValue());
                snapshot.put("constraint:" + keyParts[0] + ":" + keyParts[1] + ":" + columns,
                        List.of(keyParts[1]));
            }

            for (String table : tables) {
                try (ResultSet resultSet = connection.createStatement().executeQuery(
                        "SELECT COUNT(*) FROM " + table)) {
                    resultSet.next();
                    snapshot.put("rows:" + table, List.of(Long.toString(resultSet.getLong(1))));
                }
            }
        }

        snapshot.replaceAll((key, values) -> {
            List<String> sortedValues = new ArrayList<>(values);
            sortedValues.sort(String::compareTo);
            return sortedValues;
        });
        return snapshot;
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
