package top.egon.cola.archetype.source.web.infrastructure.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LogicalSchemaParityTest {

    private static final String MASTER_DATA_LOCATION =
            "classpath:db/migration/sharding/master-data";
    private static final String SHARD_LOCATION = "classpath:db/migration/sharding/shard";

    @Test
    void shouldMatchVersionedHistorySchema(@TempDir Path temp) throws Exception {
        String baselineMasterDataUrl = h2Url("organization-history-baseline-master-data");
        String baselineShardUrl = h2Url("organization-history-baseline-shard");
        migrate(baselineMasterDataUrl, MASTER_DATA_LOCATION);
        migrate(baselineShardUrl, SHARD_LOCATION);

        Path versionedMasterDataLocation = copyVersionedMigrationsToTempLocation(temp, "master-data");
        Path versionedShardLocation = copyVersionedMigrationsToTempLocation(temp, "shard");
        String versionedMasterDataUrl = h2Url("organization-history-versioned-master-data");
        String versionedShardUrl = h2Url("organization-history-versioned-shard");
        migrate(versionedMasterDataUrl, filesystemLocation(versionedMasterDataLocation));
        migrate(versionedShardUrl, filesystemLocation(versionedShardLocation));

        assertThat(logicalSchema(baselineMasterDataUrl))
                .as("master-data B schema 必须等价于 V-only schema")
                .isEqualTo(logicalSchema(versionedMasterDataUrl));
        assertThat(logicalSchema(baselineShardUrl))
                .as("shard B schema 必须等价于 V-only schema")
                .isEqualTo(logicalSchema(versionedShardUrl));

        migrate(versionedMasterDataUrl, MASTER_DATA_LOCATION);
        migrate(versionedShardUrl, SHARD_LOCATION);
    }

    @Test
    void shouldBuildOneCompleteLogicalSchemaFromMasterDataAndShards() throws Exception {
        Map<String, List<String>> masterDataSchema = migrateAndRead(
                "organization-parity-master-data",
                "classpath:db/migration/sharding/master-data");
        Map<String, List<String>> shardZeroSchema = migrateAndRead(
                "organization-parity-shard-0", "classpath:db/migration/sharding/shard");
        Map<String, List<String>> shardOneSchema = migrateAndRead(
                "organization-parity-shard-1", "classpath:db/migration/sharding/shard");

        assertThat(shardOneSchema).isEqualTo(shardZeroSchema);
        Map<String, List<String>> shardingSchema = new TreeMap<>(masterDataSchema);
        mergeSchema(shardingSchema, shardZeroSchema);
        assertThat(shardingSchema.keySet()).containsExactly(
                "grades",
                "permissions",
                "role_permissions",
                "roles",
                "school_class_users",
                "school_classes",
                "user_roles",
                "users");
    }

    @Test
    void shouldRejectMembershipWhoseGradeDoesNotMatchItsSchoolClass() throws Exception {
        String url = h2Url("organization-membership-foreign-key");
        migrate(url, "classpath:db/migration/sharding/shard");
        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            connection.createStatement().executeUpdate("""
                    INSERT INTO school_classes_0(
                        id, name, grade_name, grade_id, status, create_time, tenant_id)
                    VALUES (
                        1001,
                        'Class A',
                        'Grade A',
                        2001,
                        'ACTIVE',
                        CURRENT_TIMESTAMP,
                        1)
                    """);

            assertThatThrownBy(() -> connection.createStatement().executeUpdate("""
                    INSERT INTO school_class_users_0(
                        id, tenant_id, grade_id, user_id, school_class_id, create_time)
                    VALUES (
                        3001,
                        1,
                        9999,
                        4001,
                        1001,
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
