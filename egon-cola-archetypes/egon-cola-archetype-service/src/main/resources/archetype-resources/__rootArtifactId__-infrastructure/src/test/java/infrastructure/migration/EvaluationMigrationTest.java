#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EvaluationMigrationTest {

    @Test
    void shouldMigrateValidV1ResultsWithoutChangingLegacyRows() throws Exception {
        String url = "jdbc:h2:mem:migration-valid;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        migrateToV1(url);
        insertLegacyData(url, 90);

        Flyway.configure().dataSource(url, "sa", "").load().migrate();

        assertEquals(1, count(url, "select count(*) from exam_result where id='result-1'"));
        assertEquals(1, count(url, "select count(*) from score where id='result-1'"));
        assertEquals(1, count(url, "select count(*) from exam where id='legacy-exam-result-1'"));
        assertEquals(1, count(url, "select count(*) from course where code='LEGACY-course-1'"));
    }

    @Test
    void shouldRejectInvalidLegacyScoreWithoutDeletingV1Row() throws Exception {
        String url = "jdbc:h2:mem:migration-invalid;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        migrateToV1(url);
        insertLegacyData(url, 101);

        assertThrows(FlywayException.class,
                () -> Flyway.configure().dataSource(url, "sa", "").load().migrate());
        assertEquals(1, count(url, "select count(*) from exam_result where id='result-1'"));
    }

    private static void migrateToV1(String url) {
        Flyway.configure().dataSource(url, "sa", "").target("1").load().migrate();
    }

    private static void insertLegacyData(String url, int score) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("insert into course values "
                    + "('course-1','Math',3,'ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
            statement.executeUpdate("insert into exam_result values "
                    + "('result-1','course-1','student-1'," + score
                    + ",'RECORDED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        }
    }

    private static int count(String url, String sql) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }
}
