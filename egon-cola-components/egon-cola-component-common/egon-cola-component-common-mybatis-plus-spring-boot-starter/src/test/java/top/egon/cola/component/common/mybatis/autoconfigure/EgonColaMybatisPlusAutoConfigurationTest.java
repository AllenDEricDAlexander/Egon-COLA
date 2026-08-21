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
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.business.EgonColaUserIdProvider;
import top.egon.cola.component.common.mybatis.handler.EgonColaMetaObjectHandler;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaModelValidationInterceptor;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaTenantIdGuardInnerInterceptor;

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
            MybatisPlusInterceptor outer = context.getBean(MybatisPlusInterceptor.class);
            assertThat(outer.getInterceptors()).extracting(Object::getClass)
                    .containsExactly(EgonColaTenantIdGuardInnerInterceptor.class,
                            BlockAttackInnerInterceptor.class,
                            TenantLineInnerInterceptor.class,
                            OptimisticLockerInnerInterceptor.class,
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

    private ApplicationContextRunner runner(boolean enabled) {
        return runnerWithoutValidator()
                .withUserConfiguration(SafeOuterConfiguration.class)
                .withBean(Validator.class, VALIDATOR_FACTORY::getValidator)
                .withPropertyValues("egon.cola.component.mybatis-plus.enabled=" + enabled);
    }

    private ApplicationContextRunner runnerWithoutValidator() {
        return new ApplicationContextRunner()
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
