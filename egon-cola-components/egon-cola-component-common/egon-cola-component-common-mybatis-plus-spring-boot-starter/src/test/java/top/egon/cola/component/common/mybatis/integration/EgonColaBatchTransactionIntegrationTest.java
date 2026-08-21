package top.egon.cola.component.common.mybatis.integration;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusInnerInterceptorAutoConfiguration;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusAutoConfiguration;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.support.TestBusinessModel;
import top.egon.cola.component.common.mybatis.support.TestBusinessService;
import top.egon.cola.component.common.mybatis.support.TestTenantIdProvider;
import top.egon.cola.component.common.mybatis.support.TestUserIdProvider;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Real transaction, rollback, context-drift, and concurrent tenant proof.
 */
class EgonColaBatchTransactionIntegrationTest {

    private static final ValidatorFactory VALIDATOR_FACTORY =
            Validation.buildDefaultValidatorFactory();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void validBatchCommitsAndMiddleDatabaseFailureRollsBack() {
        runnerWithTestTenant().run(context -> {
            TestTenantIdProvider tenant = context.getBean(TestTenantIdProvider.class);
            TestUserIdProvider user = context.getBean(TestUserIdProvider.class);
            TestBusinessService service = context.getBean(TestBusinessService.class);
            JdbcTemplate jdbc = new JdbcTemplate(context.getBean(DataSource.class));
            tenant.set(41L);
            user.set("batch-user");
            List<TestBusinessModel> models = java.util.stream.IntStream.range(0, 5)
                    .mapToObj(index -> new TestBusinessModel().businessValues("batch-" + index, null))
                    .toList();
            assertThat(service.saveBatch(models, 2)).isTrue();
            assertThat(countRows(jdbc, 41L)).isEqualTo(5);

            TestBusinessModel first = new TestBusinessModel().businessValues("duplicate-a", null);
            first.setId(700L);
            TestBusinessModel second = new TestBusinessModel().businessValues("duplicate-b", null);
            second.setId(700L);
            assertThatThrownBy(() -> service.saveBatch(List.of(first, second), 1))
                    .isInstanceOf(RuntimeException.class);
            assertThat(countRows(jdbc, 41L)).isEqualTo(5);
        });
    }

    @Test
    void providerDriftRollsBackWholeBatch() {
        DriftingTenantProvider drifting = new DriftingTenantProvider();
        runner().withBean(EgonColaTenantIdProvider.class, () -> drifting)
                .run(context -> {
                    TestUserIdProvider user = context.getBean(TestUserIdProvider.class);
                    TestBusinessService service = context.getBean(TestBusinessService.class);
                    JdbcTemplate jdbc = new JdbcTemplate(context.getBean(DataSource.class));
                    user.set("drift-user");
                    assertThatThrownBy(() -> service.saveBatch(List.of(
                            new TestBusinessModel().businessValues("drift-a", null),
                            new TestBusinessModel().businessValues("drift-b", null)), 1))
                            .hasMessageContaining("TENANT_CONTEXT_MISMATCH");
                    assertThat(countRows(jdbc, 41L)).isZero();
                    assertThat(countRows(jdbc, 42L)).isZero();
                });
    }

    @Test
    void concurrentThreadLocalContextsRemainDisjoint() throws Exception {
        runnerWithTestTenant().run(context -> {
            TestTenantIdProvider tenant = context.getBean(TestTenantIdProvider.class);
            TestUserIdProvider user = context.getBean(TestUserIdProvider.class);
            TestBusinessService service = context.getBean(TestBusinessService.class);
            JdbcTemplate jdbc = new JdbcTemplate(context.getBean(DataSource.class));
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Future<Boolean> first = executor.submit(() -> saveInThread(service, tenant, user, 51L));
                Future<Boolean> second = executor.submit(() -> saveInThread(service, tenant, user, 52L));
                assertThat(first.get()).isTrue();
                assertThat(second.get()).isTrue();
            } finally {
                executor.shutdownNow();
            }
            assertThat(countRows(jdbc, 51L)).isEqualTo(1);
            assertThat(countRows(jdbc, 52L)).isEqualTo(1);
        });
    }

    private static boolean saveInThread(TestBusinessService service,
                                        TestTenantIdProvider tenant,
                                        TestUserIdProvider user,
                                        long tenantId) {
        tenant.set(tenantId);
        user.set("user-" + tenantId);
        return service.save(new TestBusinessModel().businessValues("thread-" + tenantId, null));
    }

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        EgonColaMybatisPlusAutoConfiguration.class,
                        MybatisPlusInnerInterceptorAutoConfiguration.class,
                        MybatisPlusAutoConfiguration.class))
                .withUserConfiguration(EgonColaTenantIdSqlIntegrationTest.H2Configuration.class,
                        TransactionConfiguration.class)
                .withBean(TestUserIdProvider.class, TestUserIdProvider::new)
                .withBean(Validator.class, VALIDATOR_FACTORY::getValidator)
                .withBean(Clock.class, () -> Clock.fixed(
                        Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
    }

    private ApplicationContextRunner runnerWithTestTenant() {
        return runner().withBean(TestTenantIdProvider.class, TestTenantIdProvider::new);
    }

    private static int countRows(JdbcTemplate jdbc, long tenantId) {
        Integer count = jdbc.queryForObject(
                "select count(*) from test_business_record where tenant_id = ? and is_deleted = false",
                Integer.class, tenantId);
        return count == null ? 0 : count;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    static class TransactionConfiguration {
        @Bean
        DataSourceTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }

    private static final class DriftingTenantProvider implements EgonColaTenantIdProvider {
        private int reads;

        @Override
        public Long currentTenantId() {
            reads++;
            return reads == 1 ? 41L : 42L;
        }
    }
}
