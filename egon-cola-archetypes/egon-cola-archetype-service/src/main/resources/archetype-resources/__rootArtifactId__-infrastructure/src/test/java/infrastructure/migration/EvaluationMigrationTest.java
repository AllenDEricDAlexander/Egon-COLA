#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class EvaluationMigrationTest {

    @Test
    void shouldInitializeCompleteDefaultSchema() throws Exception {
        String url = h2Url("evaluation-default-migration");

        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration/default")
                .validateMigrationNaming(true)
                .load()
                .migrate();

        assertEquals(5, countBusinessTables(url));
    }

    @Test
    void shouldInitializeSingleAndShardSchemasIndependently() throws Exception {
        String singleUrl = h2Url("evaluation-single-migration");
        String shardUrl = h2Url("evaluation-shard-migration");

        migrate(singleUrl, "classpath:db/migration/sharding/single");
        migrate(shardUrl, "classpath:db/migration/sharding/shard");

        assertEquals(1, countBusinessTables(singleUrl));
        assertEquals(8, countBusinessTables(shardUrl));
    }

    private static void migrate(String url, String location) {
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations(location)
                .validateMigrationNaming(true)
                .load()
                .migrate();
    }

    private static int countBusinessTables(String url) throws Exception {
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name NOT IN ('flyway_schema_history')
                """;
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }

    private static String h2Url(String database) {
        return "jdbc:h2:mem:" + database
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    }
}
