package top.egon.cola.archetype.source.web.infrastructure;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.FileSystemResource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

/** Historical test name retained; verifies the final managed model fixture, not Flyway execution. */
class OrganizationFlywayMigrationTest {
    @Test
    void finalFixtureKeepsTechnicalColumnsAndCatalogSeeds() throws Exception {
        Path fixture;
        try (var paths = Files.list(Path.of(".."))) {
            fixture = paths.filter(p -> p.getFileName().toString().endsWith("-starter")).findFirst().orElseThrow()
                    .resolve("src/test/resources/mybatis/h2-schema.sql");
        }
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:organization_" + UUID.randomUUID().toString().replace("-", "") + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        source.setUser("sa");
        try {
            new ResourceDatabasePopulator(new FileSystemResource(fixture)).execute(source);
            var jdbc = new org.springframework.jdbc.core.JdbcTemplate(source);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND column_name IN ('tenant_id','deleted_at','version')", Integer.class)).isEqualTo(24);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND column_name='tenant_id' AND is_nullable='NO'", Integer.class)).isEqualTo(8);
            assertThat(jdbc.queryForObject("SELECT code FROM roles WHERE tenant_id=1 AND id=1001 AND deleted_at IS NULL", String.class)).isEqualTo("STUDENT");
            assertThat(jdbc.queryForObject("SELECT code FROM permissions WHERE tenant_id=1 AND id=2001 AND deleted_at IS NULL", String.class)).isEqualTo("CLASS_READ");
        } finally {
            try (var connection = source.getConnection(); var statement = connection.createStatement()) { statement.execute("SHUTDOWN"); }
        }
    }
}
