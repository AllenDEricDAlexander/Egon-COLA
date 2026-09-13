package top.egon.cola.archetype.source.lightopen.infrastructure.migration;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The former manual-schema suite now checks the final model fixture; PostgreSQL execution is opt-in. */
class ManualSchemaIntegrationTest {
    @Test
    void finalFixtureHasTenantNullableDeletionAndVersionForAllLogicalTables() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:open_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        source.setUser("sa");
        try {
            new ResourceDatabasePopulator(new ClassPathResource("mybatis/h2-schema.sql")).execute(source);
            try (var connection = source.getConnection(); var statement = connection.createStatement();
                 var rows = statement.executeQuery("SELECT table_name, column_name, is_nullable FROM information_schema.columns WHERE table_schema='public' AND column_name IN ('tenant_id','deleted_at','version')")) {
                int count = 0;
                while (rows.next()) {
                    assertThat(rows.getString(1)).startsWith("light_");
                    assertThat(rows.getString(3)).isEqualTo(rows.getString(2).equals("deleted_at") ? "YES" : "NO");
                    count++;
                }
                assertThat(count).isEqualTo(24);
            }
        } finally {
            try (var connection = source.getConnection(); var statement = connection.createStatement()) { statement.execute("SHUTDOWN"); }
        }
    }
}
