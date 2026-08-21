package top.egon.cola.component.common.mybatis.autoconfigure;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusInnerInterceptorAutoConfiguration;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import jakarta.validation.Validator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.business.EgonColaMdcTenantIdProvider;
import top.egon.cola.component.common.mybatis.business.EgonColaMdcUserIdProvider;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdTenantLineHandler;
import top.egon.cola.component.common.mybatis.business.EgonColaUserIdProvider;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;
import top.egon.cola.component.common.mybatis.handler.EgonColaMetaObjectHandler;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaModelValidationInterceptor;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaTenantIdGuardInnerInterceptor;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;

import java.time.Clock;

/**
 * Opt-in-by-property (enabled by default) common MyBatis-Plus runtime chain.
 */
@AutoConfiguration(before = {
        MybatisPlusAutoConfiguration.class,
        MybatisPlusInnerInterceptorAutoConfiguration.class
})
@EnableConfigurationProperties(EgonColaMybatisPlusProperties.class)
@ConditionalOnProperty(prefix = EgonColaMybatisPlusProperties.PREFIX,
        name = "enabled", havingValue = "true", matchIfMissing = true)
public class EgonColaMybatisPlusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock egonColaMybatisPlusClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean(EgonColaTenantIdProvider.class)
    public EgonColaTenantIdProvider egonColaMdcTenantIdProvider(
            EgonColaMybatisPlusProperties properties) {
        return new EgonColaMdcTenantIdProvider(properties);
    }

    @Bean
    @ConditionalOnMissingBean(EgonColaUserIdProvider.class)
    public EgonColaUserIdProvider egonColaMdcUserIdProvider(
            EgonColaMybatisPlusProperties properties) {
        return new EgonColaMdcUserIdProvider(properties);
    }

    @Bean
    public ValidationUtils egonColaValidationUtils(ObjectProvider<Validator> validatorProvider) {
        Validator validator = validatorProvider.getIfAvailable();
        if (validator == null) {
            throw new EgonColaMybatisPlusConfigurationException("VALIDATOR_BEAN_MISSING");
        }
        return new ValidationUtils(validator);
    }

    @Bean
    public EgonColaModelValidationUtils egonColaModelValidationUtils(
            ValidationUtils validationUtils,
            EgonColaTenantIdProvider tenantIdProvider) {
        return new EgonColaModelValidationUtils(validationUtils, tenantIdProvider);
    }

    @Bean
    public EgonColaModelValidationInterceptor egonColaModelValidationInterceptor(
            EgonColaModelValidationUtils modelValidationUtils) {
        return new EgonColaModelValidationInterceptor(modelValidationUtils);
    }

    @Bean
    @ConditionalOnMissingBean(com.baomidou.mybatisplus.core.handlers.MetaObjectHandler.class)
    @ConditionalOnProperty(prefix = EgonColaMybatisPlusProperties.PREFIX + ".meta-fill",
            name = "enabled", havingValue = "true", matchIfMissing = true)
    public EgonColaMetaObjectHandler egonColaMetaObjectHandler(
            EgonColaTenantIdProvider tenantIdProvider,
            EgonColaUserIdProvider userIdProvider,
            Clock clock) {
        return new EgonColaMetaObjectHandler(tenantIdProvider, userIdProvider, clock);
    }

    @Bean
    @Order(100)
    public EgonColaTenantIdGuardInnerInterceptor egonColaTenantIdGuardInnerInterceptor(
            EgonColaTenantIdProvider tenantIdProvider,
            EgonColaMybatisPlusProperties properties) {
        return new EgonColaTenantIdGuardInnerInterceptor(tenantIdProvider, properties);
    }

    @Bean
    @Order(200)
    @ConditionalOnProperty(prefix = EgonColaMybatisPlusProperties.PREFIX + ".block-attack",
            name = "enabled", havingValue = "true", matchIfMissing = true)
    public BlockAttackInnerInterceptor egonColaBlockAttackInnerInterceptor() {
        return new BlockAttackInnerInterceptor();
    }

    @Bean
    @Order(300)
    public TenantLineInnerInterceptor egonColaTenantLineInnerInterceptor(
            EgonColaTenantIdProvider tenantIdProvider,
            EgonColaMybatisPlusProperties properties) {
        TenantLineHandler handler = new EgonColaTenantIdTenantLineHandler(tenantIdProvider, properties);
        return new TenantLineInnerInterceptor(handler);
    }

    @Bean
    @Order(400)
    @ConditionalOnProperty(prefix = EgonColaMybatisPlusProperties.PREFIX + ".optimistic-locker",
            name = "enabled", havingValue = "true", matchIfMissing = true)
    public OptimisticLockerInnerInterceptor egonColaOptimisticLockerInnerInterceptor() {
        return new OptimisticLockerInnerInterceptor();
    }

    @Bean
    @Order(500)
    @ConditionalOnProperty(prefix = EgonColaMybatisPlusProperties.PREFIX + ".pagination",
            name = "enabled", havingValue = "true", matchIfMissing = true)
    public PaginationInnerInterceptor egonColaPaginationInnerInterceptor(
            EgonColaMybatisPlusProperties properties) {
        PaginationInnerInterceptor interceptor = new PaginationInnerInterceptor();
        interceptor.setOverflow(properties.getPagination().isOverflow());
        interceptor.setMaxLimit((long) properties.getPagination().getMaxPageSize());
        return interceptor;
    }

    @Bean
    public EgonColaMybatisPlusContractValidator egonColaMybatisPlusContractValidator(
            ObjectProvider<com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor> outerProvider,
            ObjectProvider<com.baomidou.mybatisplus.core.handlers.MetaObjectHandler> handlerProvider,
            ObjectProvider<EgonColaModelValidationInterceptor> validationProvider,
            EgonColaMybatisPlusProperties properties) {
        return new EgonColaMybatisPlusContractValidator(
                outerProvider, handlerProvider, validationProvider, properties);
    }
}
