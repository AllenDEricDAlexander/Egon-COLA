package ${package}.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class OrganizationFlywayMigrationTest {

    @Test
    void migratesFreshDefaultSchema() throws Exception {
        DataSource dataSource = TestDataSources.h2PostgreSqlMode("organization-default");

        migrate(dataSource, "classpath:db/migration/default");

        try (Connection connection = dataSource.getConnection()) {
            assertThat(existingTables(connection)).contains(
                "users", "roles", "permissions", "user_roles", "role_permissions",
                "grades", "school_classes", "school_class_users", "flyway_schema_history");
            assertThat(count(connection, "roles")).isEqualTo(1);
            assertThat(count(connection, "permissions")).isEqualTo(1);
            assertUuidV7(singleValue(connection, "SELECT id FROM roles"));
            assertUuidV7(singleValue(connection, "SELECT id FROM permissions"));
        }
    }

    @Test
    void migratesFreshSingleSchemaWithoutShardedTables() throws Exception {
        DataSource dataSource = TestDataSources.h2PostgreSqlMode("organization-single");

        migrate(dataSource, "classpath:db/migration/sharding/single");

        try (Connection connection = dataSource.getConnection()) {
            assertThat(existingTables(connection)).contains(
                "users", "roles", "permissions", "user_roles", "role_permissions",
                "grades", "flyway_schema_history");
            assertThat(existingTables(connection))
                .doesNotContain("school_classes", "school_class_users");
        }
    }

    @Test
    void migratesFreshShardSchemaWithoutSingleTables() throws Exception {
        DataSource dataSource = TestDataSources.h2PostgreSqlMode("organization-shard");

        migrate(dataSource, "classpath:db/migration/sharding/shard");

        try (Connection connection = dataSource.getConnection()) {
            assertThat(existingTables(connection)).contains(
                "school_classes_0", "school_classes_1",
                "school_class_users_0", "school_class_users_1",
                "flyway_schema_history");
            assertThat(existingTables(connection))
                .doesNotContain("users", "roles", "permissions", "grades");
        }
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

    private static List<String> existingTables(Connection connection) throws Exception {
        try (ResultSet resultSet = connection.getMetaData()
                .getTables(null, connection.getSchema(), null, new String[] {"TABLE"})) {
            java.util.ArrayList<String> tables = new java.util.ArrayList<>();
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

    private static void assertUuidV7(String value) {
        assertThat(value).hasSize(36);
        assertThat(UUID.fromString(value).version()).isEqualTo(7);
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
