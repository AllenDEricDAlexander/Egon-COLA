package top.egon.cola.component.common.mybatis.integration;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionTemplate;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.handler.EgonColaMetaObjectHandler;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaModelValidationInterceptor;
import top.egon.cola.component.common.mybatis.model.EgonColaIdentifierGenerator;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;
import top.egon.cola.component.common.mybatis.support.TestBusinessMapper;
import top.egon.cola.component.common.mybatis.support.TestBusinessModel;
import top.egon.cola.component.common.mybatis.support.TestBusinessRepository;
import top.egon.cola.component.common.mybatis.support.TestTenantIdProvider;
import top.egon.cola.component.common.mybatis.support.TestUserIdProvider;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "EGON_MP_PG_MODEL_TEST", matches = "true")
class EgonColaLogicDeletePostgreSqlTest {

    private static final ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();
    private static final ValidationUtils VALIDATION_UTILS = new ValidationUtils(VALIDATORS.getValidator());

    @Test
    void versionedLogicalDeleteStoresUtcTimestampAndKeepsTenantIsolation() throws Exception {
        try (PostgreFixtureBO fixture = fixture()) {
            var repository = fixture.repository();
            TestBusinessModel row = new TestBusinessModel().businessValues("active", "payload");
            assertThat(repository.save(row)).isTrue();
            assertThat(row.getId()).isPositive();
            assertThat(row.getVersion()).isZero();
            assertThat(fixture.jdbc().queryForObject("SELECT deleted_at FROM test_business_record WHERE id=?", LocalDateTime.class, row.getId())).isNull();
            fixture.tenant().set(42L);
            assertThat(repository.getById(row.getId())).isNull();
            assertThat(repository.removeById(row.getId())).isFalse();
            fixture.tenant().set(41L);
            assertThat(repository.removeById(row.getId())).isTrue();
            assertThat(repository.getById(row.getId())).isNull();
            assertThat(fixture.jdbc().queryForObject("SELECT version FROM test_business_record WHERE id=?", Long.class, row.getId())).isEqualTo(1L);
            assertThat(fixture.jdbc().queryForObject("SELECT deleted_at FROM test_business_record WHERE id=?", LocalDateTime.class, row.getId())).isNotNull();
            assertThat(repository.removeById(row.getId())).isFalse();
        }
    }

    static PostgreFixtureBO fixture() throws Exception {
        PGSimpleDataSource admin = source();
        String schema = "egon_mp_" + UUID.randomUUID().toString().replace("-", "");
        new JdbcTemplate(admin).execute("CREATE SCHEMA " + schema);
        try {
            PGSimpleDataSource scoped = source();
            scoped.setCurrentSchema(schema);
            new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(scoped);
            TestTenantIdProvider tenant = new TestTenantIdProvider();
            tenant.set(41L);
            TestUserIdProvider user = new TestUserIdProvider();
            user.set("pg-test-user");
            if (EgonColaModelValidationUtils.current() == null) {
                EgonColaModelValidationUtils.initialize(VALIDATION_UTILS, "EgonColaLogicDeletePostgreSqlTest");
            }
            MybatisPlusInterceptor plugins = new MybatisPlusInterceptor();
            plugins.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
                @Override
                public Expression getTenantId() { return new LongValue(EgonColaTenantIdProvider.currentTenantId()); }
            }));
            plugins.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
            GlobalConfig global = new GlobalConfig();
            global.setDbConfig(new GlobalConfig.DbConfig());
            global.setMetaObjectHandler(new EgonColaMetaObjectHandler(user, Clock.systemUTC()));
            SnowflakeIdGenerator.initialize(0L, Duration.ofMillis(5));
            global.setIdentifierGenerator(new EgonColaIdentifierGenerator());
            MybatisConfiguration configuration = new MybatisConfiguration();
            configuration.setMapUnderscoreToCamelCase(true);
            MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
            factory.setDataSource(scoped);
            factory.setConfiguration(configuration);
            factory.setGlobalConfig(global);
            factory.setPlugins(plugins, new EgonColaModelValidationInterceptor());
            factory.setMapperLocations(new ClassPathResource("mybatis/TestBusinessMapper.xml"));
            SqlSessionTemplate template = new SqlSessionTemplate(factory.getObject());
            TestBusinessRepository repository = new TestBusinessRepository(
                    template.getMapper(TestBusinessMapper.class), new EgonColaMybatisPlusProperties());
            return new PostgreFixtureBO(admin, schema, repository, tenant,
                    new JdbcTemplate(scoped), new TransactionTemplate(new DataSourceTransactionManager(scoped)));
        } catch (Exception failure) {
            new JdbcTemplate(admin).execute("DROP SCHEMA " + schema + " CASCADE");
            throw failure;
        }
    }

    private static PGSimpleDataSource source() {
        PGSimpleDataSource source = new PGSimpleDataSource();
        source.setURL(System.getenv("EGON_MP_PG_URL"));
        source.setUser(System.getenv("EGON_MP_PG_USER"));
        source.setPassword(System.getenv("EGON_MP_PG_PASSWORD"));
        return source;
    }

    record PostgreFixtureBO(PGSimpleDataSource admin, String schema,
                            TestBusinessRepository repository, TestTenantIdProvider tenant,
                            JdbcTemplate jdbc, TransactionTemplate transaction) implements AutoCloseable {
        @Override
        public void close() {
            // Cleanup is confined to the UUID schema created by the explicitly enabled test.
            new JdbcTemplate(admin).execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }
}
