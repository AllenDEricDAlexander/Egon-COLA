package top.egon.cola.archetype.source.web.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class OrganizationFlywayMigrationTest {

    private static final String MASTER_DATA_LOCATION =
            "classpath:db/migration/sharding/master-data";
    private static final String SHARD_LOCATION = "classpath:db/migration/sharding/shard";

    @Test
    void shouldMigrateEquivalentFreshMasterAndShardSchemasFromBaselineAndVersionedHistory(
            @TempDir Path temp) throws Exception {
        DataSource baselineMasterData = TestDataSources.h2PostgreSqlMode(
                "organization-baseline-master-data");
        DataSource baselineShard = TestDataSources.h2PostgreSqlMode("organization-baseline-shard");
        migrate(baselineMasterData, MASTER_DATA_LOCATION);
        migrate(baselineShard, SHARD_LOCATION);

        Path versionedMasterDataLocation = copyVersionedMigrations(temp, "master-data");
        Path versionedShardLocation = copyVersionedMigrations(temp, "shard");
        DataSource versionedMasterData = TestDataSources.h2PostgreSqlMode(
                "organization-versioned-master-data");
        DataSource versionedShard = TestDataSources.h2PostgreSqlMode(
                "organization-versioned-shard");
        migrate(versionedMasterData, filesystemLocation(versionedMasterDataLocation));
        migrate(versionedShard, filesystemLocation(versionedShardLocation));

        assertThat(appliedMigrationTypes(baselineMasterData))
                .as("master-data fresh schema 必须由一个 B baseline 建立")
                .containsExactly("TABLE", "SQL_BASELINE");
        assertThat(appliedMigrationTypes(baselineShard))
                .as("shard fresh schema 必须由一个 B baseline 建立")
                .containsExactly("TABLE", "SQL_BASELINE");
        assertThat(schemaSnapshot(baselineMasterData))
                .as("master-data B schema 必须等价于 V-only schema")
                .isEqualTo(schemaSnapshot(versionedMasterData));
        assertThat(schemaSnapshot(baselineShard))
                .as("shard B schema 必须等价于 V-only schema")
                .isEqualTo(schemaSnapshot(versionedShard));
        assertThat(seedSnapshot(baselineMasterData))
                .as("master-data B seeds 必须等价于 V-only seeds")
                .isEqualTo(seedSnapshot(versionedMasterData));
        assertThat(seedSnapshot(baselineMasterData))
                .containsEntry("roles", List.of("1001|STUDENT|Student|ACTIVE|1|migration|migration|0"))
                .containsEntry("permissions",
                        List.of("2001|CLASS_READ|Read school class|API|ACTIVE|1|migration|migration|0"));

        assertThat(existingTables(baselineMasterData.getConnection()))
                .contains("users", "roles", "permissions", "user_roles", "role_permissions", "grades")
                .doesNotContain("school_classes_0", "school_class_users_0");
        assertThat(existingTables(baselineShard.getConnection()))
                .contains("school_classes_0", "school_classes_1",
                        "school_class_users_0", "school_class_users_1")
                .doesNotContain("users", "roles", "permissions", "grades");

        assertMismatchedGradeIsRejected(baselineShard);
        assertMismatchedGradeIsRejected(versionedShard);
        migrate(versionedMasterData, MASTER_DATA_LOCATION);
        migrate(versionedShard, SHARD_LOCATION);
    }

    @Test
    void migratesFreshMasterDataSchemaWithoutShardedTables() throws Exception {
        DataSource dataSource = TestDataSources.h2PostgreSqlMode("organization-master-data");

        migrate(dataSource, MASTER_DATA_LOCATION);

        try (Connection connection = dataSource.getConnection()) {
            assertThat(existingTables(connection)).contains(
                    "users", "roles", "permissions", "user_roles", "role_permissions",
                    "grades", "flyway_schema_history");
            assertThat(existingTables(connection))
                    .doesNotContain("school_classes", "school_class_users");
            assertThat(count(connection, "roles")).isEqualTo(1);
            assertThat(count(connection, "permissions")).isEqualTo(1);
            assertThat(singleValue(connection, "SELECT id FROM roles")).isEqualTo("1001");
            assertThat(singleValue(connection, "SELECT id FROM permissions")).isEqualTo("2001");
            assertThat(singleValue(connection, "SELECT tenant_id FROM roles")).isEqualTo("1");
        }
    }

    @Test
    void migratesFreshShardSchemaWithoutMasterDataTables() throws Exception {
        DataSource dataSource = TestDataSources.h2PostgreSqlMode("organization-shard");

        migrate(dataSource, SHARD_LOCATION);

        try (Connection connection = dataSource.getConnection()) {
            assertThat(existingTables(connection)).contains(
                    "school_classes_0", "school_classes_1",
                    "school_class_users_0", "school_class_users_1",
                    "flyway_schema_history");
            assertThat(existingTables(connection))
                    .doesNotContain("users", "roles", "permissions", "grades");
        }
    }

    private static Path copyVersionedMigrations(Path temp, String role) throws Exception {
        Path source = Path.of(Objects.requireNonNull(
                OrganizationFlywayMigrationTest.class.getClassLoader()
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

    private static void migrate(DataSource dataSource, String location) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(location)
                .validateMigrationNaming(true)
                .load();
        flyway.migrate();
        flyway.validate();
    }

    private static List<String> appliedMigrationTypes(DataSource dataSource) throws SQLException {
        List<String> types = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                ResultSet resultSet = connection.createStatement().executeQuery(
                        "SELECT type FROM flyway_schema_history ORDER BY installed_rank")) {
            while (resultSet.next()) {
                types.add(resultSet.getString("type"));
            }
        }
        return types;
    }

    private static Map<String, List<String>> schemaSnapshot(DataSource dataSource)
            throws SQLException {
        Map<String, List<String>> snapshot = new TreeMap<>();
        TreeSet<String> tables = new TreeSet<>();
        Map<String, List<String>> constraintsByName = new TreeMap<>();

        try (Connection connection = dataSource.getConnection()) {
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

    private static Map<String, List<String>> seedSnapshot(DataSource dataSource)
            throws SQLException {
        Map<String, List<String>> seeds = new TreeMap<>();
        try (Connection connection = dataSource.getConnection()) {
            seeds.put("roles", queryRows(connection, """
                    SELECT id, code, name, status, tenant_id, create_user_id,
                           update_user_id, is_deleted
                    FROM roles ORDER BY id
                    """));
            seeds.put("permissions", queryRows(connection, """
                    SELECT id, code, name, type, status, tenant_id, create_user_id,
                           update_user_id, is_deleted
                    FROM permissions ORDER BY id
                    """));
        }
        return seeds;
    }

    private static List<String> queryRows(Connection connection, String sql) throws SQLException {
        List<String> rows = new ArrayList<>();
        try (ResultSet resultSet = connection.createStatement().executeQuery(sql)) {
            int columnCount = resultSet.getMetaData().getColumnCount();
            while (resultSet.next()) {
                List<String> values = new ArrayList<>();
                for (int index = 1; index <= columnCount; index++) {
                    values.add(resultSet.getString(index));
                }
                rows.add(String.join("|", values));
            }
        }
        return rows;
    }

    private static void assertMismatchedGradeIsRejected(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().executeUpdate("""
                    INSERT INTO school_classes_0(
                        id, name, grade_name, grade_id, status, create_time, tenant_id)
                    VALUES (
                        1001, 'Class A', 'Grade A', 2001, 'ACTIVE', CURRENT_TIMESTAMP, 1)
                    """);

            assertThatThrownBy(() -> connection.createStatement().executeUpdate("""
                    INSERT INTO school_class_users_0(
                        id, tenant_id, grade_id, user_id, school_class_id, create_time)
                    VALUES (3001, 1, 9999, 4001, 1001, CURRENT_TIMESTAMP)
                    """))
                    .isInstanceOf(SQLException.class);
        }
    }

    private static List<String> existingTables(Connection connection) throws Exception {
        try (ResultSet resultSet = connection.getMetaData()
                .getTables(null, connection.getSchema(), null, new String[] {"TABLE"})) {
            ArrayList<String> tables = new ArrayList<>();
            while (resultSet.next()) {
                tables.add(resultSet.getString("TABLE_NAME").toLowerCase());
            }
            return tables;
        }
    }

    private static int count(Connection connection, String table) throws Exception {
        try (ResultSet resultSet = connection.createStatement()
                .executeQuery("SELECT COUNT(*) FROM " + table)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private static String singleValue(Connection connection, String sql) throws Exception {
        try (ResultSet resultSet = connection.createStatement().executeQuery(sql)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getString(1);
        }
    }
}

final class TestDataSources {

    private TestDataSources() {
    }

    static DataSource h2PostgreSqlMode(String name) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:" + name
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
