package top.egon.cola.archetype.source.service.infrastructure.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EvaluationMigrationTest {

    private static final String MASTER_DATA_LOCATION =
            "classpath:db/migration/sharding/master-data";
    private static final String SHARD_LOCATION = "classpath:db/migration/sharding/shard";

    @Test
    void shouldInitializeEquivalentMasterAndShardSchemasFromBaselineAndVersionedHistory(
            @TempDir Path temp) throws Exception {
        String baselineMasterDataUrl = migrate(
                "evaluation-baseline-master-data", MASTER_DATA_LOCATION);
        String baselineShardUrl = migrate("evaluation-baseline-shard", SHARD_LOCATION);

        Path versionedMasterDataLocation = copyVersionedMigrations(temp, "master-data");
        Path versionedShardLocation = copyVersionedMigrations(temp, "shard");
        String versionedMasterDataUrl = migrate(
                "evaluation-versioned-master-data", filesystemLocation(versionedMasterDataLocation));
        String versionedShardUrl = migrate(
                "evaluation-versioned-shard", filesystemLocation(versionedShardLocation));

        assertThat(appliedMigrationTypes(baselineMasterDataUrl))
                .as("master-data fresh schema 必须由一个 B baseline 建立")
                .containsExactly("TABLE", "SQL_BASELINE");
        assertThat(appliedMigrationTypes(baselineShardUrl))
                .as("shard fresh schema 必须由一个 B baseline 建立")
                .containsExactly("TABLE", "SQL_BASELINE");
        assertThat(businessTables(baselineMasterDataUrl))
                .containsExactly("evaluation_course");
        assertThat(businessTables(baselineShardUrl))
                .containsExactly(
                        "evaluation_course_schedule_0",
                        "evaluation_course_schedule_1",
                        "evaluation_exam_0",
                        "evaluation_exam_1",
                        "evaluation_exam_paper_0",
                        "evaluation_exam_paper_1",
                        "evaluation_score_0",
                        "evaluation_score_1");
        assertThat(schemaSnapshot(baselineMasterDataUrl))
                .as("master-data B schema 必须等价于 V-only schema")
                .isEqualTo(schemaSnapshot(versionedMasterDataUrl));
        assertThat(schemaSnapshot(baselineShardUrl))
                .as("shard B schema 必须等价于 V-only schema")
                .isEqualTo(schemaSnapshot(versionedShardUrl));

        migrateAndValidateAtLocation(versionedMasterDataUrl, MASTER_DATA_LOCATION);
        migrateAndValidateAtLocation(versionedShardUrl, SHARD_LOCATION);
    }

    private static Path copyVersionedMigrations(Path temp, String role) throws Exception {
        Path source = Path.of(Objects.requireNonNull(
                EvaluationMigrationTest.class.getClassLoader()
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

    private static String migrate(String database, String location) {
        String url = h2Url(database);
        migrateAndValidateAtLocation(url, location);
        return url;
    }

    private static void migrateAndValidateAtLocation(String url, String location) {
        Flyway flyway = Flyway.configure()
                .dataSource(url, "sa", "")
                .locations(location)
                .validateMigrationNaming(true)
                .load();
        flyway.migrate();
        flyway.validate();
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

    private static List<String> businessTables(String url) throws SQLException {
        List<String> tables = new ArrayList<>();
        String sql = """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name <> 'flyway_schema_history'
                ORDER BY table_name
                """;
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
                ResultSet resultSet = connection.createStatement().executeQuery(sql)) {
            while (resultSet.next()) {
                tables.add(resultSet.getString("table_name"));
            }
        }
        return tables;
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

    private static String h2Url(String database) {
        return "jdbc:h2:mem:" + database
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    }
}
