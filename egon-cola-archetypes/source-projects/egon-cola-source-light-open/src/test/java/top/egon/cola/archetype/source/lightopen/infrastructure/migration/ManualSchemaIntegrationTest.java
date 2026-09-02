package top.egon.cola.archetype.source.lightopen.infrastructure.migration;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ManualSchemaIntegrationTest {

    private static final String MASTER_SQL =
            "db/manual/postgresql/master-data/001__create_light_master_data_schema.sql";
    private static final String MASTER_MIGRATION =
            "db/manual/postgresql/master-data/003__migrate_light_master_data_to_egon_model.sql";
    private static final String SHARD_SQL =
            "db/manual/postgresql/shard/002__create_light_sharded_schema.sql";
    private static final String SHARD_MIGRATION =
            "db/manual/postgresql/shard/004__migrate_light_sharded_to_tenant_model.sql";

    @Test
    void applies_master_schema_and_manual_common_model_migration() throws Exception {
        DataSource dataSource = dataSource("manual-light-fresh");
        assertThat(tableNames(dataSource)).isEmpty();
        ManualSchemaTestSupport.executeManually(dataSource, MASTER_SQL, MASTER_MIGRATION);
        assertThat(tableNames(dataSource)).containsExactlyInAnyOrder(
                "light_courses", "light_permissions", "light_role_permissions", "light_roles", "light_user_roles", "light_users");
        assertThat(columnType(dataSource, "light_users", "tenant_id"))
                .isEqualToIgnoringCase("BIGINT");
        assertThat(columnType(dataSource, "light_users", "is_deleted"))
                .isEqualToIgnoringCase("BOOLEAN");
    }

    @Test
    void applies_shard_schema_and_manual_tenant_migration_in_suffix_parity() throws Exception {
        DataSource shardZero = dataSource("manual-light-shard-0");
        DataSource shardOne = dataSource("manual-light-shard-1");
        ManualSchemaTestSupport.executeManually(shardZero, SHARD_SQL, SHARD_MIGRATION);
        ManualSchemaTestSupport.executeManually(shardOne, SHARD_SQL, SHARD_MIGRATION);
        assertThat(tableNames(shardZero)).containsExactlyInAnyOrder(
                "light_class_course_schedules_0", "light_class_course_schedules_1",
                "light_school_classes_0", "light_school_classes_1");
        assertThat(columnDefinitions(shardZero)).isEqualTo(columnDefinitions(shardOne));
        assertThat(columnType(shardZero, "light_school_classes_0", "tenant_id"))
                .isEqualToIgnoringCase("BIGINT");
        assertThat(columnType(shardZero, "light_class_course_schedules_0", "school_class_id"))
                .isEqualToIgnoringCase("BIGINT");
    }

    private static DataSource dataSource(String name) {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + name
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private static List<String> tableNames(DataSource dataSource) throws SQLException {
        String sql = "SELECT table_name FROM information_schema.tables "
                + "WHERE table_schema = 'public' ORDER BY table_name";
        List<String> names = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                ResultSet result = connection.createStatement().executeQuery(sql)) {
            while (result.next()) {
                names.add(result.getString(1));
            }
        }
        return names;
    }

    private static Map<String, List<String>> columnDefinitions(DataSource dataSource)
            throws SQLException {
        String sql = "SELECT table_name, column_name, data_type, is_nullable "
                + "FROM information_schema.columns WHERE table_schema = 'public' "
                + "ORDER BY table_name, ordinal_position";
        Map<String, List<String>> definitions = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection();
                ResultSet result = connection.createStatement().executeQuery(sql)) {
            while (result.next()) {
                definitions.computeIfAbsent(result.getString("table_name"), ignored -> new ArrayList<>())
                        .add(result.getString("column_name") + "|"
                                + result.getString("data_type") + "|"
                                + result.getString("is_nullable"));
            }
        }
        return definitions;
    }

    private static String columnType(DataSource dataSource, String table, String column)
            throws SQLException {
        String sql = "SELECT data_type FROM information_schema.columns "
                + "WHERE table_schema = 'public' AND table_name = ? AND column_name = ?";
        try (Connection connection = dataSource.getConnection();
                var statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                return result.getString(1);
            }
        }
    }
}
