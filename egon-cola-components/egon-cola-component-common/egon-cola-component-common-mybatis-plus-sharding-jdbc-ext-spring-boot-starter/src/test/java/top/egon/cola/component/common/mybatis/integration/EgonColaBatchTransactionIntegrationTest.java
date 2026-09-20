package top.egon.cola.component.common.mybatis.integration;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusInnerInterceptorAutoConfiguration;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusAutoConfiguration;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.business.EgonColaUserIdProvider;
import top.egon.cola.component.common.mybatis.handler.EgonColaMetaObjectHandler;
import top.egon.cola.component.common.mybatis.support.TestBusinessModel;
import top.egon.cola.component.common.mybatis.support.TestBusinessRepository;
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
            TestBusinessRepository service = context.getBean(TestBusinessRepository.class);
            JdbcTemplate jdbc = new JdbcTemplate(context.getBean(DataSource.class));
            tenant.set(41L);
            user.set("batch-user");
            List<TestBusinessModel> models = java.util.stream.IntStream.range(0, 5)
                    .mapToObj(index -> new TestBusinessModel().businessValues("batch-" + index, null))
                    .toList();
            Boolean saved = transaction(context).execute(status -> service.saveBatch(models, 2));
            assertThat(saved).isTrue();
            assertThat(countRows(jdbc, 41L)).isEqualTo(5);

            TestBusinessModel first = new TestBusinessModel().businessValues("duplicate-a", null);
            first.setId(700L);
            TestBusinessModel second = new TestBusinessModel().businessValues("duplicate-b", null);
            second.setId(701L);
            second.setPayload("x".repeat(1025));
            assertThatThrownBy(() -> transaction(context).execute(status -> service.saveBatch(List.of(first, second), 1)))
                    .isInstanceOf(RuntimeException.class);
            assertThat(countRows(jdbc, 41L)).isEqualTo(5);
        });
    }

    @Test
    void tenantContextDriftRollsBackWholeBatch() {
        runnerWithTestTenant().withUserConfiguration(DriftingHandlerConfiguration.class)
                .run(context -> {
                    context.getBean(TestTenantIdProvider.class).set(41L);
                    TestUserIdProvider user = context.getBean(TestUserIdProvider.class);
                    TestBusinessRepository service = context.getBean(TestBusinessRepository.class);
                    JdbcTemplate jdbc = new JdbcTemplate(context.getBean(DataSource.class));
                    user.set("drift-user");
                    assertThatThrownBy(() -> transaction(context).execute(status -> service.saveBatch(List.of(
                            new TestBusinessModel().businessValues("drift-a", null),
                            new TestBusinessModel().businessValues("drift-b", null)), 1)))
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
            TestBusinessRepository service = context.getBean(TestBusinessRepository.class);
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

    private static TransactionTemplate transaction(org.springframework.context.ApplicationContext context) {
        return new TransactionTemplate(context.getBean(DataSourceTransactionManager.class));
    }

    @Test
    void anOuterCommandFailureAfterBatchFlushStillRollsBack() {
        runnerWithTestTenant().run(context -> {
            context.getBean(TestTenantIdProvider.class).set(41L);
            context.getBean(TestUserIdProvider.class).set("batch-user");
            TestBusinessRepository repository = context.getBean(TestBusinessRepository.class);
            assertThatThrownBy(() -> transaction(context).execute(status -> {
                repository.saveBatch(List.of(new TestBusinessModel().businessValues("first", null)), 1);
                throw new IllegalStateException("OUTER_COMMAND_FAILED");
            })).hasMessage("OUTER_COMMAND_FAILED");
            assertThat(countRows(new JdbcTemplate(context.getBean(DataSource.class)), 41L)).isZero();
        });
    }

    @Test
    void nonemptyBatchRequiresAnActualTransactionOnTheSameDatasource() {
        runnerWithTestTenant().run(context -> {
            context.getBean(TestTenantIdProvider.class).set(41L);
            context.getBean(TestUserIdProvider.class).set("batch-user");
            TestBusinessRepository repository = context.getBean(TestBusinessRepository.class);
            assertThatThrownBy(() -> repository.saveBatch(List.of(new TestBusinessModel().businessValues("first", null))))
                    .hasMessageContaining("BATCH_TRANSACTION_REQUIRED");
            assertThat(countRows(new JdbcTemplate(context.getBean(DataSource.class)), 41L)).isZero();
        });
    }

    @Test
    void aVersionConflictInALaterUpdateChunkRollsBackEarlierRowsEvenWhenCaught() {
        runnerWithTestTenant().run(context -> {
            context.getBean(TestTenantIdProvider.class).set(41L);
            context.getBean(TestUserIdProvider.class).set("batch-user");
            TestBusinessRepository repository = context.getBean(TestBusinessRepository.class);
            TestBusinessModel first = new TestBusinessModel().businessValues("first", null);
            TestBusinessModel second = new TestBusinessModel().businessValues("second", null);
            repository.save(first);
            repository.save(second);
            TestBusinessModel a = repository.getById(first.getId());
            TestBusinessModel stale = repository.getById(second.getId());
            TestBusinessModel winner = repository.getById(second.getId());
            winner.setTitle("winner");
            repository.updateById(winner);
            a.setTitle("must-rollback");
            stale.setTitle("stale");
            assertThatThrownBy(() -> transaction(context).execute(status -> {
                try {
                    repository.updateBatchById(List.of(a, stale), 1);
                } catch (IllegalStateException conflict) {
                    assertThat(conflict).hasMessage("BATCH_VERSION_CONFLICT_OR_WRITE_FAILED");
                }
                return null;
            })).isInstanceOf(org.springframework.transaction.UnexpectedRollbackException.class);
            assertThat(repository.getById(first.getId()).getTitle()).isEqualTo("first");
            assertThat(repository.getById(second.getId()).getTitle()).isEqualTo("winner");
        });
    }

    @Test
    void batchDeleteSkipsMissingIdsAndUpdatesEverySelectedVersion() {
        runnerWithTestTenant().run(context -> {
            context.getBean(TestTenantIdProvider.class).set(41L);
            context.getBean(TestUserIdProvider.class).set("batch-user");
            TestBusinessRepository repository = context.getBean(TestBusinessRepository.class);
            TestBusinessModel row = new TestBusinessModel().businessValues("delete", null);
            repository.save(row);
            Boolean removed = transaction(context).execute(status -> repository.removeByIds(List.of(row.getId(), row.getId(), 99L)));
            assertThat(removed).isTrue();
            assertThat(repository.getById(row.getId())).isNull();
            Long version = new JdbcTemplate(context.getBean(DataSource.class)).queryForObject(
                    "SELECT version FROM test_business_record WHERE id=?", Long.class, row.getId());
            assertThat(version).isEqualTo(1L);
        });
    }

    @Test
    void aTransactionOnAnotherDatasourceCannotAuthorizeThisRepositoryBatch() {
        runnerWithTestTenant().run(context -> {
            context.getBean(TestTenantIdProvider.class).set(41L);
            context.getBean(TestUserIdProvider.class).set("batch-user");
            TestBusinessRepository repository = context.getBean(TestBusinessRepository.class);
            var other = new org.h2.jdbcx.JdbcDataSource();
            other.setURL("jdbc:h2:mem:other_" + System.nanoTime());
            var otherTransaction = new TransactionTemplate(new DataSourceTransactionManager(other));
            assertThatThrownBy(() -> otherTransaction.execute(status -> repository.saveBatch(
                    List.of(new TestBusinessModel().businessValues("must-not-write", null)))))
                    .hasMessageContaining("BATCH_TRANSACTION_REQUIRED");
            assertThat(countRows(new JdbcTemplate(context.getBean(DataSource.class)), 41L)).isZero();
        });
    }

    private static boolean saveInThread(TestBusinessRepository service,
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
                "select count(*) from test_business_record where tenant_id = ? and deleted_at IS NULL",
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

    @Configuration(proxyBeanMethods = false)
    static class DriftingHandlerConfiguration {
        @Bean
        EgonColaMetaObjectHandler driftingMetaObjectHandler(EgonColaUserIdProvider userIdProvider, Clock clock) {
            return new DriftingMetaObjectHandler(userIdProvider, clock);
        }
    }

    /**
     * Publishes a different tenant while the batch is still in flight, so the snapshot check must fail.
     */
    private static final class DriftingMetaObjectHandler extends EgonColaMetaObjectHandler {

        private DriftingMetaObjectHandler(EgonColaUserIdProvider userIdProvider, Clock clock) {
            super(userIdProvider, clock);
        }

        @Override
        protected void afterInsertFill(MetaObject metaObject) {
            MDC.put(EgonColaTenantIdProvider.DEFAULT_MDC_KEY, "42");
        }
    }
}
