package ${package}.infrastructure.migration;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Test-only explicit executor for the operator-owned manual schema scripts.
 */
final class ManualSchemaTestSupport {

    private ManualSchemaTestSupport() {
    }

    static void executeManually(DataSource dataSource, String... resources) throws SQLException {
        if (dataSource == null || resources == null || resources.length == 0) {
            throw new IllegalArgumentException("data source and manual resources are required");
        }
        try (Connection connection = dataSource.getConnection()) {
            for (String resource : resources) {
                if (resource == null || resource.isBlank()) {
                    throw new IllegalArgumentException("manual resource must not be blank");
                }
                ScriptUtils.executeSqlScript(connection, new ClassPathResource(resource));
            }
        }
    }
}
