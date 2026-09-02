package top.egon.cola.archetype.source.web.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class OrganizationFlywayMigrationTest {

    @Test
    void migratesFreshMasterDataSchemaWithoutShardedTables() throws Exception {
        DataSource dataSource = TestDataSources.h2PostgreSqlMode("organization-master-data");

        migrate(dataSource, "classpath:db/migration/sharding/master-data");

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
