package top.egon.cola.archetype.source.web.support;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;
import top.egon.cola.component.common.mybatis.routing.EgonColaWriteTargetResolver;
import top.egon.cola.component.common.mybatis.routing.EgonColaRouteResult;
import top.egon.cola.component.common.mybatis.routing.EgonColaPhysicalTargetBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Fast business tests use isolated logical tables; real PG/SS tests must not inherit this fixture. */
@TestPropertySource(properties = {"egon.cola.component.mybatis-plus.ddl.enabled=false", "spring.sql.init.mode=never",
        "egon.cola.component.id.machine-id=0"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class PersistenceTestSupport {
    @TestBean(name = "dataSource", methodName = "isolatedDataSource", enforceOverride = true)
    protected DataSource testDataSource;
    @TestBean(name = "egonColaWriteTargetResolver", methodName = "plainResolver", enforceOverride = true)
    protected EgonColaWriteTargetResolver resolver;
    @TestBean(name = "egonColaRoutingProfiles", methodName = "isolatedProfiles", enforceOverride = true)
    protected Map<String, EgonColaRoutingProfileBO> routingProfiles;
    @TestBean(name = "egonColaShardingRouteFingerprint", methodName = "isolatedFingerprint", enforceOverride = true)
    protected String routeFingerprint;

    static DataSource isolatedDataSource() {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:web_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=PostgreSQL;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false");
        source.setUser("sa");
        new ResourceDatabasePopulator(new ClassPathResource("mybatis/h2-schema.sql")).execute(source);
        return source;
    }

    static EgonColaWriteTargetResolver plainResolver() {
        return query -> new EgonColaRouteResult(List.of(new EgonColaPhysicalTargetBO("test", "public", query.logicalTable())), "a".repeat(64));
    }

    static Map<String, EgonColaRoutingProfileBO> isolatedProfiles() {
        return Map.of();
    }

    static String isolatedFingerprint() {
        return "f".repeat(64);
    }

    @AfterEach
    void closeOwnDatabase() {
        new JdbcTemplate(testDataSource).execute("SHUTDOWN");
    }
}
