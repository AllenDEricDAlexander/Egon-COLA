package ${package}.starter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

/**
 * Explicit H2 schema fixture for tests; production startup never calls this helper.
 */
final class OrganizationManualSchemaTestSupport {

    public static final class Initializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            createShardingSchema(context.getEnvironment());
        }
    }

    private OrganizationManualSchemaTestSupport() {
    }

    static void createShardingSchema(Environment environment) {
        String master = environment.getProperty(
                "app.sharding.physical-data-sources[0].jdbc-url",
                "jdbc:h2:mem:${rootArtifactId}-test-master-data;MODE=PostgreSQL;"
                        + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
        String shard0 = environment.getProperty(
                "app.sharding.physical-data-sources[1].jdbc-url",
                "jdbc:h2:mem:${rootArtifactId}-test-shard-0;MODE=PostgreSQL;"
                        + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
        String shard1 = environment.getProperty(
                "app.sharding.physical-data-sources[2].jdbc-url",
                "jdbc:h2:mem:${rootArtifactId}-test-shard-1;MODE=PostgreSQL;"
                        + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
        createSchema(master, shard0, shard1);
    }

    private static void createSchema(String master, String shard0, String shard1) {
        try (Connection connection = DriverManager.getConnection(master, "sa", "")) {
            executeMasterSchema(connection);
        } catch (SQLException failure) {
            throw new IllegalStateException("Unable to create explicit test master schema", failure);
        }
        createShardSchema(shard0);
        createShardSchema(shard1);
    }

    private static void createShardSchema(String url) {
        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            executeShardSchema(connection);
        } catch (SQLException failure) {
            throw new IllegalStateException("Unable to create explicit test shard schema", failure);
        }
    }

    private static void executeMasterSchema(Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS users ("
                    + "id BIGINT PRIMARY KEY, name VARCHAR(120) NOT NULL, "
                    + "email VARCHAR(160) NOT NULL UNIQUE, status VARCHAR(32) NOT NULL, "
                    + "created_at TIMESTAMP NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS roles ("
                    + "id BIGINT PRIMARY KEY, code VARCHAR(64) NOT NULL UNIQUE, "
                    + "name VARCHAR(120) NOT NULL, status VARCHAR(32) NOT NULL, "
                    + "created_at TIMESTAMP NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS permissions ("
                    + "id BIGINT PRIMARY KEY, code VARCHAR(64) NOT NULL UNIQUE, "
                    + "name VARCHAR(120) NOT NULL, type VARCHAR(32) NOT NULL, "
                    + "status VARCHAR(32) NOT NULL, created_at TIMESTAMP NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS user_roles ("
                    + "id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL, role_id BIGINT NOT NULL, "
                    + "created_at TIMESTAMP NOT NULL, UNIQUE (user_id, role_id))");
            statement.execute("CREATE TABLE IF NOT EXISTS role_permissions ("
                    + "id BIGINT PRIMARY KEY, role_id BIGINT NOT NULL, permission_id BIGINT NOT NULL, "
                    + "created_at TIMESTAMP NOT NULL, UNIQUE (role_id, permission_id))");
            statement.execute("CREATE TABLE IF NOT EXISTS grades ("
                    + "id BIGINT PRIMARY KEY, code VARCHAR(160) NOT NULL UNIQUE, "
                    + "name VARCHAR(120) NOT NULL, status VARCHAR(32) NOT NULL, "
                    + "created_at TIMESTAMP NOT NULL)");
            for (String table : new String[] {
                    "users", "roles", "permissions", "user_roles", "role_permissions", "grades"}) {
                statement.execute("ALTER TABLE " + table
                        + " ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP");
                statement.execute("ALTER TABLE " + table
                        + " ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT 1");
                statement.execute("ALTER TABLE " + table
                        + " ADD COLUMN IF NOT EXISTS create_user_id VARCHAR(128) DEFAULT 'test'");
                statement.execute("ALTER TABLE " + table
                        + " ADD COLUMN IF NOT EXISTS create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
                statement.execute("ALTER TABLE " + table
                        + " ADD COLUMN IF NOT EXISTS update_user_id VARCHAR(128) DEFAULT 'test'");
                statement.execute("ALTER TABLE " + table
                        + " ADD COLUMN IF NOT EXISTS update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
                statement.execute("ALTER TABLE " + table
                        + " ADD COLUMN IF NOT EXISTS is_deleted SMALLINT DEFAULT 0");
            }
        }
    }

    private static void executeShardSchema(Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            for (int slot = 0; slot < 2; slot++) {
                statement.execute("CREATE TABLE IF NOT EXISTS school_classes_" + slot + " ("
                        + "id BIGINT PRIMARY KEY, name VARCHAR(120) NOT NULL, "
                        + "grade_name VARCHAR(120) NOT NULL, grade_id BIGINT NOT NULL, "
                        + "status VARCHAR(32) NOT NULL, created_at TIMESTAMP NOT NULL, "
                        + "UNIQUE (grade_id, name), UNIQUE (grade_id, id))");
                statement.execute("CREATE TABLE IF NOT EXISTS school_class_users_" + slot + " ("
                        + "id BIGINT PRIMARY KEY, grade_id BIGINT NOT NULL, user_id BIGINT NOT NULL, "
                        + "school_class_id BIGINT NOT NULL, created_at TIMESTAMP NOT NULL, "
                        + "UNIQUE (grade_id, school_class_id, user_id))");
                for (String table : new String[] {"school_classes_" + slot, "school_class_users_" + slot}) {
                    statement.execute("ALTER TABLE " + table
                            + " ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP");
                    statement.execute("ALTER TABLE " + table
                            + " ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT 1");
                    statement.execute("ALTER TABLE " + table
                            + " ADD COLUMN IF NOT EXISTS create_user_id VARCHAR(128) DEFAULT 'test'");
                    statement.execute("ALTER TABLE " + table
                            + " ADD COLUMN IF NOT EXISTS create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
                    statement.execute("ALTER TABLE " + table
                            + " ADD COLUMN IF NOT EXISTS update_user_id VARCHAR(128) DEFAULT 'test'");
                    statement.execute("ALTER TABLE " + table
                            + " ADD COLUMN IF NOT EXISTS update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
                    statement.execute("ALTER TABLE " + table
                            + " ADD COLUMN IF NOT EXISTS is_deleted SMALLINT DEFAULT 0");
                }
            }
        }
    }
}
