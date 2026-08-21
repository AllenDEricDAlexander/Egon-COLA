package top.egon.cola.component.common.mybatis.integration;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusInnerInterceptorAutoConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.core.JdbcTemplate;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusAutoConfiguration;
import top.egon.cola.component.common.mybatis.support.TestBusinessMapper;
import top.egon.cola.component.common.mybatis.support.TestBusinessModel;
import top.egon.cola.component.common.mybatis.support.TestBusinessService;
import top.egon.cola.component.common.mybatis.support.TestTenantIdProvider;
import top.egon.cola.component.common.mybatis.support.TestUserIdProvider;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Real MyBatis/H2 proof for the official Mapper and technical Service paths.
 */
class EgonColaTenantIdSqlIntegrationTest {

    private static final ValidatorFactory VALIDATOR_FACTORY =
            Validation.buildDefaultValidatorFactory();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    EgonColaMybatisPlusAutoConfiguration.class,
                    MybatisPlusInnerInterceptorAutoConfiguration.class,
                    MybatisPlusAutoConfiguration.class))
            .withUserConfiguration(H2Configuration.class)
            .withBean(TestTenantIdProvider.class, TestTenantIdProvider::new)
            .withBean(TestUserIdProvider.class, TestUserIdProvider::new)
            .withBean(Validator.class, VALIDATOR_FACTORY::getValidator)
            .withBean(Clock.class, () -> Clock.fixed(
                    Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void officialCrudIsTenantScopedAndAuthoritativelyFilled() {
        contextRunner.run(context -> {
            TestTenantIdProvider tenant = context.getBean(TestTenantIdProvider.class);
            TestUserIdProvider user = context.getBean(TestUserIdProvider.class);
            TestBusinessService service = context.getBean(TestBusinessService.class);
            tenant.set(11L);
            user.set("alice");
            TestBusinessModel first = new TestBusinessModel()
                    .businessValues("tenant-11", null);
            first.setTenantId(22L);

            assertThat(service.save(first)).isTrue();
            assertThat(first.getTenantId()).isEqualTo(11L);
            assertThat(first.getCreateUserId()).isEqualTo("alice");
            assertThat(service.list()).extracting(TestBusinessModel::getTitle)
                    .containsExactly("tenant-11");

            tenant.set(22L);
            user.set("bob");
            TestBusinessModel second = new TestBusinessModel()
                    .businessValues("tenant-22", null);
            assertThat(service.save(second)).isTrue();

            tenant.set(11L);
            assertThat(service.count()).isEqualTo(1L);
            assertThat(service.getById(second.getId())).isNull();
            assertThat(service.getOptById(second.getId())).isEmpty();
        });
    }

    @Test
    void logicDeleteAndWideWritesAreScopedAndGuarded() {
        contextRunner.run(context -> {
            TestTenantIdProvider tenant = context.getBean(TestTenantIdProvider.class);
            TestUserIdProvider user = context.getBean(TestUserIdProvider.class);
            TestBusinessService service = context.getBean(TestBusinessService.class);
            tenant.set(11L);
            user.set("alice");
            TestBusinessModel model = new TestBusinessModel().businessValues("delete-me", null);
            assertThat(service.save(model)).isTrue();
            assertThat(service.removeById(model.getId())).isTrue();
            assertThat(service.getById(model.getId())).isNull();
            assertThatThrownBy(() -> service.update(new UpdateWrapper<TestBusinessModel>()
                    .set("title", "wide")))
                    .hasMessageContaining("BUSINESS_PREDICATE_REQUIRED");
        });
    }

    @Test
    void directMapperExplicitTenantMustMatchProvider() {
        contextRunner.run(context -> {
            TestTenantIdProvider tenant = context.getBean(TestTenantIdProvider.class);
            TestUserIdProvider user = context.getBean(TestUserIdProvider.class);
            TestBusinessMapper mapper = context.getBean(TestBusinessMapper.class);
            TestBusinessService service = context.getBean(TestBusinessService.class);
            tenant.set(11L);
            user.set("alice");
            assertThat(service.save(new TestBusinessModel().businessValues("same", null))).isTrue();
            assertThat(mapper.explicitTenant(11L)).hasSize(1);
            assertThatThrownBy(() -> mapper.explicitTenant(22L))
                    .hasMessageContaining("TENANT_CONTEXT_MISMATCH");
            assertThatThrownBy(() -> mapper.explicitTenant(null))
                    .hasMessageContaining("TENANT_CONTEXT_MISMATCH");
        });
    }

    @Test
    void protectedColumnsAndUnsupportedStatementsFailBeforeJdbc() {
        contextRunner.run(context -> {
            TestTenantIdProvider tenant = context.getBean(TestTenantIdProvider.class);
            TestUserIdProvider user = context.getBean(TestUserIdProvider.class);
            TestBusinessMapper mapper = context.getBean(TestBusinessMapper.class);
            TestBusinessService service = context.getBean(TestBusinessService.class);
            tenant.set(11L);
            user.set("alice");
            TestBusinessModel model = new TestBusinessModel().businessValues("guarded", null);
            assertThat(service.save(model)).isTrue();
            assertThatThrownBy(() -> mapper.forbiddenTenantMutation(model.getId(), 22L))
                    .hasMessageContaining("TENANT_COLUMN_MUTATION_FORBIDDEN");
            assertThatThrownBy(() -> mapper.forbiddenLogicDeleteMutation(model.getId(), true))
                    .hasMessageContaining("LOGIC_DELETE_COLUMN_MUTATION_FORBIDDEN");
            assertThatThrownBy(() -> mapper.unsupportedStatement())
                    .hasMessageContaining("SQL_SHAPE_UNSUPPORTED");
        });
    }

    @Test
    void ignoredGlobalTableBypassesTenantRewriteOnlyWhenConfigured() {
        contextRunner.withPropertyValues(
                        "egon.cola.component.mybatis-plus.tenant-id.ignored-tables[0]=test_global_record")
                .run(context -> {
                    TestTenantIdProvider tenant = context.getBean(TestTenantIdProvider.class);
                    TestBusinessMapper mapper = context.getBean(TestBusinessMapper.class);
                    tenant.clear();
                    new JdbcTemplate(context.getBean(DataSource.class))
                            .update("insert into test_global_record (id, payload) values (?, ?)", 1L, "global");
                    assertThat(mapper.globalRows()).hasSize(1);
                });
    }

    @Test
    void loadedResultAndOptimisticVersionAreValidated() {
        contextRunner.run(context -> {
            TestTenantIdProvider tenant = context.getBean(TestTenantIdProvider.class);
            TestUserIdProvider user = context.getBean(TestUserIdProvider.class);
            TestBusinessService service = context.getBean(TestBusinessService.class);
            tenant.set(11L);
            user.set("alice");
            TestBusinessModel model = new TestBusinessModel().businessValues("versioned", null);
            model.setVersion(1L);
            assertThat(service.save(model)).isTrue();
            TestBusinessModel loaded = service.getById(model.getId());
            loaded.setTitle("versioned-updated");
            assertThat(service.updateById(loaded)).isTrue();
            assertThat(loaded.getVersion()).isEqualTo(2L);

            new JdbcTemplate(context.getBean(DataSource.class))
                    .update("update test_business_record set title = '' where id = ?", model.getId());
            assertThatThrownBy(service::list)
                    .hasMessageContaining("title");
        });
    }

    @Configuration(proxyBeanMethods = false)
    @MapperScan(basePackageClasses = TestBusinessMapper.class)
    static class H2Configuration {
        @Bean
        DataSource dataSource() throws Exception {
            JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:testdb_" + System.nanoTime()
                    + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false");
            dataSource.setUser("sa");
            new ResourceDatabasePopulator(new ClassPathResource("schema.sql"))
                    .execute(dataSource);
            return dataSource;
        }

        @Bean
        TestBusinessService testBusinessService(
                top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils validation,
                top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider tenant,
                top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties properties) {
            return new TestBusinessService(validation, tenant, properties);
        }
    }
}
