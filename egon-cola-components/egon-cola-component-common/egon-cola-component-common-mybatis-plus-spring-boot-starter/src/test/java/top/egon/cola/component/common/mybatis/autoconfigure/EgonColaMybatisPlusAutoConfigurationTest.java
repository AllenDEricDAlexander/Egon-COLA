package top.egon.cola.component.common.mybatis.autoconfigure;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.apache.ibatis.plugin.Interceptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.apache.ibatis.reflection.MetaObject;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.business.EgonColaUserIdProvider;
import top.egon.cola.component.common.mybatis.handler.EgonColaMetaObjectHandler;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaModelValidationInterceptor;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaTenantIdGuardInnerInterceptor;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaLocalWriteGuardInnerInterceptor;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaOriginalSqlGuardInterceptor;
import top.egon.cola.component.common.mybatis.model.EgonColaIdentifierGenerator;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EgonColaMybatisPlusAutoConfigurationTest {

    private static final ValidatorFactory VALIDATOR_FACTORY =
            Validation.buildDefaultValidatorFactory();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void bootMetadataRegistersOnlyTheEgonColaAutoConfiguration() {
        assertThat(ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()))
                .contains(EgonColaMybatisPlusAutoConfiguration.class.getName());
    }

    @Test
    void disabledConfigurationCreatesNoEgonColaBeans() {
        runner(false).run(context -> assertThat(context)
                .doesNotHaveBean(EgonColaTenantIdProvider.class)
                .doesNotHaveBean(EgonColaUserIdProvider.class)
                .doesNotHaveBean(EgonColaMetaObjectHandler.class)
                .doesNotHaveBean(EgonColaModelValidationInterceptor.class));
    }

    @Test
    void enabledConfigurationBuildsProvidersHandlerValidationAndOrderedInnerChain() {
        runner(true).run(context -> {
            assertThat(context).hasSingleBean(EgonColaTenantIdProvider.class)
                    .hasSingleBean(EgonColaUserIdProvider.class)
                    .hasSingleBean(EgonColaMetaObjectHandler.class)
                    .hasSingleBean(EgonColaModelValidationInterceptor.class)
                    .hasSingleBean(MybatisPlusInterceptor.class);
            assertThat(context).doesNotHaveBean(com.baomidou.mybatisplus.core.injector.ISqlInjector.class);
            assertThat(context).hasSingleBean(EgonColaIdentifierGenerator.class)
                    .hasSingleBean(EgonColaOriginalSqlGuardInterceptor.class)
                    .hasSingleBean(top.egon.cola.component.common.mybatis.routing.EgonColaWriteTargetResolver.class);
            assertThat(context).doesNotHaveBean(com.baomidou.mybatisplus.extension.ddl.IDdl.class);
            MybatisPlusInterceptor outer = context.getBean(MybatisPlusInterceptor.class);
            assertThat(outer.getInterceptors()).extracting(Object::getClass)
                    .containsExactly(EgonColaTenantIdGuardInnerInterceptor.class,
                            BlockAttackInnerInterceptor.class,
                            TenantLineInnerInterceptor.class,
                            OptimisticLockerInnerInterceptor.class,
                            EgonColaLocalWriteGuardInnerInterceptor.class,
                            PaginationInnerInterceptor.class);
        });
    }

    @Test
    void consumerProviderBacksOffDefaultMdcProvider() {
        EgonColaTenantIdProvider custom = () -> 11L;
        runner(true).withBean(EgonColaTenantIdProvider.class, () -> custom)
                .run(context -> assertThat(context.getBean(EgonColaTenantIdProvider.class))
                        .isSameAs(custom));
    }

    @Test
    void missingValidatorFailsWithStableConfigurationCode() {
        runnerWithoutValidator().run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(
                    EgonColaMybatisPlusConfigurationException.class);
            assertThat(context.getStartupFailure()).hasRootCauseMessage("VALIDATOR_BEAN_MISSING");
        });
    }

    @Test
    void unsafeOuterChainFailsFast() {
        runnerWithoutValidator()
                .withBean(Validator.class, VALIDATOR_FACTORY::getValidator)
                .withPropertyValues("egon.cola.component.mybatis-plus.enabled=true")
                .withUserConfiguration(UnsafeOuterConfiguration.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure().toString())
                            .contains("MYBATIS_PLUS_INTERCEPTOR_ORDER_INVALID");
                });
    }

    @Test
    void consumerValidationUtilsBeansDoNotBreakModelValidationWiring() {
        ValidationUtils foreign = new ValidationUtils(VALIDATOR_FACTORY.getValidator());
        runner(true)
                .withBean("agentFlowValidationUtils", ValidationUtils.class, () -> foreign)
                .withBean("agentValidationUtils", ValidationUtils.class, () -> foreign)
                .run(context -> assertThat(context).hasNotFailed()
                        .hasBean("egonColaValidationUtils")
                        .hasBean("egonColaModelValidationUtils"));
    }

    @Test
    void consumerClocksDoNotBreakMetaFillWiring() {
        runner(true)
                .withBean("agentClock", Clock.class, Clock::systemUTC)
                .withBean("agentFlowClock", Clock.class, Clock::systemUTC)
                .run(context -> assertThat(context).hasNotFailed()
                        .hasBean("egonColaMetaObjectHandler"));
    }

    @Test
    void singleConsumerClockBacksOffTheDefaultClock() {
        runner(true)
                .withBean("agentClock", Clock.class, Clock::systemUTC)
                .run(context -> assertThat(context).hasNotFailed()
                        .doesNotHaveBean("egonColaMybatisPlusClock"));
    }

    @Test
    void unrelatedMetaObjectHandlerFailsFast() {
        runner(true).withBean(com.baomidou.mybatisplus.core.handlers.MetaObjectHandler.class,
                        () -> new UnrelatedMetaObjectHandler())
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure().toString())
                            .contains("META_OBJECT_HANDLER_CONTRACT_INVALID");
                });
    }

    @Test
    void safeMetaObjectHandlerSubclassBacksOffDefaultWithoutDroppingContract() {
        runner(true)
                .withBean(com.baomidou.mybatisplus.core.handlers.MetaObjectHandler.class,
                        () -> new SafeMetaObjectHandler())
                .run(context -> assertThat(context.getBean(
                        com.baomidou.mybatisplus.core.handlers.MetaObjectHandler.class))
                        .isInstanceOf(SafeMetaObjectHandler.class));
    }

    @Test
    void contradictoryDevAndProdProfilesFailClosed() {
        runner(true).withPropertyValues("spring.profiles.active=dev,prod").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasMessage("ENVIRONMENT_PROFILE_CONFLICT");
        });
    }

    @Test
    void nonDevDoesNotInstallDiagnosticPluginsAndCannotEnableThem() {
        runner(true).withPropertyValues("spring.profiles.active=prod").run(context -> {
            assertThat(context).doesNotHaveBean(com.baomidou.mybatisplus.extension.plugins.inner.DataChangeRecorderInnerInterceptor.class)
                    .doesNotHaveBean(com.baomidou.mybatisplus.extension.plugins.inner.IllegalSQLInnerInterceptor.class);
        });
        runner(true).withPropertyValues("spring.profiles.active=prod", "egon.cola.component.mybatis-plus.data-change-recorder.enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasMessage("DEV_DIAGNOSTIC_PROFILE_REQUIRED");
                });
    }

    @Test
    void missingOrAmbiguousDistributedIdGeneratorIsNotReplacedByAnMpDefault() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(EgonColaMybatisPlusAutoConfiguration.class))
                .withUserConfiguration(SafeOuterConfiguration.class)
                .withBean(Validator.class, VALIDATOR_FACTORY::getValidator).run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage("ID_GENERATOR_BEAN_MISSING");
                });
        runner(true).withBean("anotherIdGenerator", LongIdGenerator.class, () -> () -> 2L).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage("ID_GENERATOR_BEAN_AMBIGUOUS");
        });
    }

    @Test
    void pageAndBatchLimitsCannotExceedTheApprovedBounds() {
        runner(true).withPropertyValues("egon.cola.component.mybatis-plus.pagination.max-page-size=501")
                .run(context -> assertThat(context).hasFailed());
        runner(true).withPropertyValues("egon.cola.component.mybatis-plus.batch.max-chunk-size=1001")
                .run(context -> assertThat(context).hasFailed());
        runner(true).withPropertyValues("egon.cola.component.mybatis-plus.batch.default-size=10", "egon.cola.component.mybatis-plus.batch.max-chunk-size=5")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void everyFactoryIsCheckedIncludingTheNonPrimaryFactory() {
        runner(true).withUserConfiguration(TwoFactoriesConfiguration.class).run(context -> assertThat(context).hasNotFailed()
                .hasBean("firstFactory").hasBean("secondFactory"));
        runner(true).withUserConfiguration(TwoFactoriesConfiguration.class).withPropertyValues("test.bad-second=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasMessage("FACTORY_INTERCEPTOR_CONTRACT_INVALID");
                });
        runner(true).withUserConfiguration(TwoFactoriesConfiguration.class).withPropertyValues("test.bad-second-id=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasMessage("FACTORY_IDENTIFIER_GENERATOR_INVALID");
                });
    }

    @Test
    void devRecorderRequiresTheActualRawLoggerToBeOff() {
        var raw = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(
                top.egon.cola.component.common.mybatis.interceptor.EgonColaDataChangeRecorderInnerInterceptor.class);
        var previous = raw.getLevel();
        try {
            raw.setLevel(ch.qos.logback.classic.Level.INFO);
            runner(true).withPropertyValues("spring.profiles.active=dev", "egon.cola.component.mybatis-plus.data-change-recorder.enabled=true")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure()).hasMessage("DATA_CHANGE_RAW_LOGGER_NOT_OFF");
                    });
            raw.setLevel(ch.qos.logback.classic.Level.OFF);
            runner(true).withPropertyValues("spring.profiles.active=dev", "egon.cola.component.mybatis-plus.data-change-recorder.enabled=true",
                            "egon.cola.component.mybatis-plus.illegal-sql.enabled=true")
                    .run(context -> assertThat(context).hasNotFailed()
                            .hasSingleBean(top.egon.cola.component.common.mybatis.interceptor.EgonColaDataChangeRecorderInnerInterceptor.class)
                            .hasSingleBean(com.baomidou.mybatisplus.extension.plugins.inner.IllegalSQLInnerInterceptor.class));
        } finally {
            raw.setLevel(previous);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TwoFactoriesConfiguration {
        @Bean("firstFactory")
        @org.springframework.context.annotation.Primary
        org.apache.ibatis.session.SqlSessionFactory firstFactory(MybatisPlusInterceptor outer,
                EgonColaOriginalSqlGuardInterceptor original, EgonColaModelValidationInterceptor validation,
                EgonColaIdentifierGenerator id, com.baomidou.mybatisplus.core.handlers.MetaObjectHandler handler) {
            return factory(outer, original, validation, id, handler, false, false);
        }

        @Bean("secondFactory")
        org.apache.ibatis.session.SqlSessionFactory secondFactory(MybatisPlusInterceptor outer,
                EgonColaOriginalSqlGuardInterceptor original, EgonColaModelValidationInterceptor validation,
                EgonColaIdentifierGenerator id, com.baomidou.mybatisplus.core.handlers.MetaObjectHandler handler,
                @org.springframework.beans.factory.annotation.Value("${test.bad-second:false}") boolean bad,
                @org.springframework.beans.factory.annotation.Value("${test.bad-second-id:false}") boolean badId) {
            return factory(outer, original, validation, id, handler, bad, badId);
        }

        private static org.apache.ibatis.session.SqlSessionFactory factory(MybatisPlusInterceptor outer,
                EgonColaOriginalSqlGuardInterceptor original, EgonColaModelValidationInterceptor validation,
                EgonColaIdentifierGenerator id, com.baomidou.mybatisplus.core.handlers.MetaObjectHandler handler, boolean bad, boolean badId) {
            var configuration = new com.baomidou.mybatisplus.core.MybatisConfiguration();
            configuration.setEnvironment(new org.apache.ibatis.mapping.Environment("test",
                    new org.mybatis.spring.transaction.SpringManagedTransactionFactory(), org.mockito.Mockito.mock(javax.sql.DataSource.class)));
            configuration.addInterceptor(outer);
            configuration.addInterceptor(validation);
            if (!bad) { configuration.addInterceptor(original); }
            var global = new com.baomidou.mybatisplus.core.config.GlobalConfig();
            global.setMetaObjectHandler(handler);
            global.setIdentifierGenerator(badId ? entity -> 1L : id);
            com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils.setGlobalConfig(configuration, global);
            return new org.apache.ibatis.session.defaults.DefaultSqlSessionFactory(configuration);
        }
    }

    @Test
    void controlledDdlCannotCoexistWithTheDefaultAfterStartRunner() {
        runner(true).withPropertyValues("egon.cola.component.mybatis-plus.ddl.enabled=true")
                .withBean(com.baomidou.mybatisplus.autoconfigure.DdlApplicationRunner.class,
                        () -> new com.baomidou.mybatisplus.autoconfigure.DdlApplicationRunner(List.of()))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasMessage("DEFAULT_DDL_RUNNER_CONFLICT");
                });
    }

    @Test
    @SuppressWarnings("rawtypes")
    void dynamicTableMappingsAreExplicitAndCannotOverlapShardingProfiles() {
        runner(true).withPropertyValues("egon.cola.component.mybatis-plus.dynamic-table-name.enabled=true",
                        "egon.cola.component.mybatis-plus.dynamic-table-name.tables.users=users_archive")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var dynamic = context.getBean(com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor.class);
                    assertThat(dynamic.getTableNameHandler().dynamicTableName("SELECT id FROM users", "users")).isEqualTo("users_archive");
                    assertThat(dynamic.getTableNameHandler().dynamicTableName("SELECT id FROM other", "other")).isEqualTo("other");
                });
        var profile = new top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO("users",
                top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO.TableKindEnum.SINGLE, "static-v1", 1, 1,
                java.util.Map.of(), null, null, 0x9e3779b97f4a7c15L,
                java.util.Map.of(new top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO.PartitionKeyBO(0, 0),
                        List.of(new top.egon.cola.component.common.mybatis.routing.EgonColaPhysicalTargetBO("primary", "public", "users"))), 1, null);
        runner(true).withBean("egonColaRoutingProfiles", java.util.Map.class, () -> java.util.Map.of("users", profile))
                .withPropertyValues("egon.cola.component.mybatis-plus.dynamic-table-name.enabled=true",
                        "egon.cola.component.mybatis-plus.dynamic-table-name.tables.users=users_archive")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasMessage("DYNAMIC_SHARDING_TABLE_CONFLICT");
                });
    }

    @Test
    void ignoredTableNormalizationPreservesTheExistingCaseInsensitiveContract() {
        var properties = new EgonColaMybatisPlusProperties();
        properties.getTenantId().getIgnoredTables().add("PUBLIC.TEST_GLOBAL_RECORD");
        assertThat(properties.getTenantId().ignores("test_global_record")).isTrue();
        assertThat(properties.getTenantId().ignores("test_business_record")).isFalse();
    }

    private ApplicationContextRunner runner(boolean enabled) {
        return runnerWithoutValidator()
                .withUserConfiguration(SafeOuterConfiguration.class)
                .withBean(Validator.class, VALIDATOR_FACTORY::getValidator)
                .withPropertyValues("egon.cola.component.mybatis-plus.enabled=" + enabled);
    }

    private ApplicationContextRunner runnerWithoutValidator() {
        return new ApplicationContextRunner()
                .withBean("snowflakeIdGenerator", LongIdGenerator.class, () -> () -> 1001L)
                .withConfiguration(AutoConfigurations.of(EgonColaMybatisPlusAutoConfiguration.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class SafeOuterConfiguration {
        @Bean
        MybatisPlusInterceptor mybatisPlusInterceptor(List<InnerInterceptor> interceptors) {
            MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
            interceptor.setInterceptors(interceptors);
            return interceptor;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class UnsafeOuterConfiguration {
        @Bean
        MybatisPlusInterceptor mybatisPlusInterceptor() {
            MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
            interceptor.setInterceptors(List.of(new BlockAttackInnerInterceptor()));
            return interceptor;
        }
    }

    private static final class UnrelatedMetaObjectHandler
            implements com.baomidou.mybatisplus.core.handlers.MetaObjectHandler {
        @Override
        public void insertFill(org.apache.ibatis.reflection.MetaObject metaObject) {
        }

        @Override
        public void updateFill(org.apache.ibatis.reflection.MetaObject metaObject) {
        }
    }

    private static final class SafeMetaObjectHandler extends EgonColaMetaObjectHandler {
        private SafeMetaObjectHandler() {
            super(() -> 0L, () -> "test-user", java.time.Clock.systemUTC());
        }

        @Override
        protected void afterInsertFill(MetaObject metaObject) {
        }
    }
}
