package ${package}.infrastructure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizationFlywayMigrationTest {

    private final DataSource dataSource = TestDataSources.h2PostgreSqlMode("migration-contract");

    @Test
    void migratesLegacyTeachingRowsWithoutDeletingDuplicatesOrOrphans() throws Exception {
        Flyway.configure().dataSource(dataSource).target("1").load().migrate();
        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().executeUpdate(
                "INSERT INTO users VALUES ('u-1','Mario','mario@example.com','ACTIVE',CURRENT_TIMESTAMP)");
            connection.createStatement().executeUpdate(
                "INSERT INTO school_classes VALUES ('c-1','Class A','Grade One',CURRENT_TIMESTAMP)");
            connection.createStatement().executeUpdate(
                "INSERT INTO school_classes VALUES ('c-2','Class A','Grade One',CURRENT_TIMESTAMP)");
            connection.createStatement().executeUpdate(
                "INSERT INTO school_class_users(user_id,school_class_id,created_at) "
                    + "VALUES ('u-1','c-1',CURRENT_TIMESTAMP)");
            connection.createStatement().executeUpdate(
                "INSERT INTO school_class_users(user_id,school_class_id,created_at) "
                    + "VALUES ('missing-user','missing-class',CURRENT_TIMESTAMP)");
        }

        Flyway flyway = Flyway.configure().dataSource(dataSource).load();
        flyway.migrate();
        flyway.validate();

        try (Connection connection = dataSource.getConnection()) {
            ResultSet grade = connection.createStatement().executeQuery(
                "SELECT id,code,status FROM grades WHERE id='legacy:Grade One'");
            assertThat(grade.next()).isTrue();
            assertThat(grade.getString("code")).isEqualTo("Grade One");
            assertThat(grade.getString("status")).isEqualTo("ACTIVE");
            assertThat(count(connection, "school_classes")).isEqualTo(2);
            assertThat(count(connection, "school_class_users")).isEqualTo(2);
            assertThat(count(connection, "roles")).isEqualTo(1);
            assertThat(count(connection, "permissions")).isEqualTo(1);
        }
    }

    private static int count(Connection connection, String table) throws Exception {
        try (ResultSet resultSet = connection.createStatement().executeQuery("SELECT COUNT(*) FROM " + table)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}

final class TestDataSources {

    private TestDataSources() {
    }

    static DataSource h2PostgreSqlMode(String name) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:" + name + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
