package top.egon.cola.component.common.mybatis.autoconfigure;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusInnerInterceptorAutoConfiguration;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.IllegalSQLInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.id.autoconfigure.IdGeneratorAutoConfiguration;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.common.mybatis.business.EgonColaMdcTenantIdProvider;
import top.egon.cola.component.common.mybatis.business.EgonColaMdcUserIdProvider;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdTenantLineHandler;
import top.egon.cola.component.common.mybatis.business.EgonColaUserIdProvider;
import top.egon.cola.component.common.mybatis.ddl.EgonColaPostgreDdlRunner;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;
import top.egon.cola.component.common.mybatis.handler.EgonColaMetaObjectHandler;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaDataChangeRecorderInnerInterceptor;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaLocalWriteGuardInnerInterceptor;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaModelValidationInterceptor;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaOriginalSqlGuardInterceptor;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaTenantIdGuardInnerInterceptor;
import top.egon.cola.component.common.mybatis.model.EgonColaIdentifierGenerator;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaTwoLevelRouteStrategy;
import top.egon.cola.component.common.mybatis.routing.EgonColaWriteTargetResolver;

import java.time.Clock;
import java.util.List;
import java.util.Map;

/**
 * Opt-in-by-property (enabled by default) common MyBatis-Plus runtime chain.
 */
@Slf4j
@AutoConfiguration(after = IdGeneratorAutoConfiguration.class, before = {
        MybatisPlusAutoConfiguration.class,
        MybatisPlusInnerInterceptorAutoConfiguration.class
})
@EnableConfigurationProperties(EgonColaMybatisPlusProperties.class)
@ConditionalOnProperty(prefix = EgonColaMybatisPlusProperties.PREFIX,
        name = "enabled", havingValue = "true", matchIfMissing = true)
public class EgonColaMybatisPlusAutoConfiguration {

    @Bean("egonColaMybatisPlusClock")
    @ConditionalOnMissingBean(Clock.class)
    public Clock egonColaMybatisPlusClock() {
        return Clock.systemUTC();
    }

    @Bean("egonColaMdcTenantIdProvider")
    @ConditionalOnMissingBean(EgonColaTenantIdProvider.class)
    public EgonColaTenantIdProvider egonColaMdcTenantIdProvider(
            EgonColaMybatisPlusProperties properties) {
        return new EgonColaMdcTenantIdProvider(properties);
    }

    @Bean("egonColaMdcUserIdProvider")
    @ConditionalOnMissingBean(EgonColaUserIdProvider.class)
    public EgonColaUserIdProvider egonColaMdcUserIdProvider(
            EgonColaMybatisPlusProperties properties) {
        return new EgonColaMdcUserIdProvider(properties);
    }

    @Bean("egonColaValidationUtils")
    public ValidationUtils egonColaValidationUtils(ObjectProvider<Validator> validatorProvider) {
        Validator validator = validatorProvider.getIfAvailable();
        if (validator == null) {
            throw new EgonColaMybatisPlusConfigurationException("VALIDATOR_BEAN_MISSING");
        }
        return new ValidationUtils(validator);
    }

    @Bean("egonColaModelValidationUtils")
    // The host may also introduce other components that provide Validator Utils (such as agent flow), and injecting them by type can be ambiguous, so named parsing is necessary.
    public EgonColaModelValidationUtils egonColaModelValidationUtils(
            @Qualifier("egonColaValidationUtils") ValidationUtils validationUtils,
            EgonColaTenantIdProvider tenantIdProvider) {
        return new EgonColaModelValidationUtils(validationUtils, tenantIdProvider);
    }

    @Bean("egonColaModelValidationInterceptor")
    public EgonColaModelValidationInterceptor egonColaModelValidationInterceptor(
            EgonColaModelValidationUtils modelValidationUtils) {
        return new EgonColaModelValidationInterceptor(modelValidationUtils);
    }

    @Bean("egonColaMetaObjectHandler")
    @ConditionalOnMissingBean(com.baomidou.mybatisplus.core.handlers.MetaObjectHandler.class)
    @ConditionalOnProperty(prefix = EgonColaMybatisPlusProperties.PREFIX + ".meta-fill",
            name = "enabled", havingValue = "true", matchIfMissing = true)
    // The host may have multiple named Clocks (such as agentClock and agentFlowClock), and injecting them by type can be ambiguous;
    // When it is unique (or @ Primary), use the host clock. Otherwise, return to systemUTC and maintain the existing semantics of 'host can override'.
    public EgonColaMetaObjectHandler egonColaMetaObjectHandler(
            EgonColaTenantIdProvider tenantIdProvider,
            EgonColaUserIdProvider userIdProvider,
            ObjectProvider<Clock> clockProvider) {
        return new EgonColaMetaObjectHandler(tenantIdProvider, userIdProvider,
                clockProvider.getIfUnique(Clock::systemUTC));
    }

    @Bean("egonColaTenantIdGuardInnerInterceptor")
    @Order(100)
    public EgonColaTenantIdGuardInnerInterceptor egonColaTenantIdGuardInnerInterceptor(
            EgonColaTenantIdProvider tenantIdProvider,
            EgonColaUserIdProvider userIdProvider,
            EgonColaMybatisPlusProperties properties) {
        return new EgonColaTenantIdGuardInnerInterceptor(tenantIdProvider, userIdProvider, properties);
    }

    @Bean("egonColaBlockAttackInnerInterceptor")
    @Order(200)
    @ConditionalOnProperty(prefix = EgonColaMybatisPlusProperties.PREFIX + ".block-attack",
            name = "enabled", havingValue = "true", matchIfMissing = true)
    public BlockAttackInnerInterceptor egonColaBlockAttackInnerInterceptor() {
        return new BlockAttackInnerInterceptor();
    }

    @Bean("egonColaTenantLineInnerInterceptor")
    @Order(300)
    public TenantLineInnerInterceptor egonColaTenantLineInnerInterceptor(
            EgonColaTenantIdProvider tenantIdProvider,
            EgonColaMybatisPlusProperties properties) {
        TenantLineHandler handler = new EgonColaTenantIdTenantLineHandler(tenantIdProvider, properties);
        return new TenantLineInnerInterceptor(handler);
    }

    @Bean("egonColaOptimisticLockerInnerInterceptor")
    @Order(400)
    @ConditionalOnProperty(prefix = EgonColaMybatisPlusProperties.PREFIX + ".optimistic-locker",
            name = "enabled", havingValue = "true", matchIfMissing = true)
    public OptimisticLockerInnerInterceptor egonColaOptimisticLockerInnerInterceptor() {
        return new OptimisticLockerInnerInterceptor();
    }

    @Bean("egonColaPaginationInnerInterceptor")
    @Order(500)
    @ConditionalOnProperty(prefix = EgonColaMybatisPlusProperties.PREFIX + ".pagination",
            name = "enabled", havingValue = "true", matchIfMissing = true)
    public PaginationInnerInterceptor egonColaPaginationInnerInterceptor(
            EgonColaMybatisPlusProperties properties) {
        PaginationInnerInterceptor interceptor = new PaginationInnerInterceptor(DbType.POSTGRE_SQL);
        interceptor.setOverflow(properties.getPagination().isOverflow());
        interceptor.setMaxLimit((long) properties.getPagination().getMaxPageSize());
        return interceptor;
    }

    @Bean("egonColaIdentifierGenerator")
    @ConditionalOnMissingBean(IdentifierGenerator.class)
    public EgonColaIdentifierGenerator egonColaIdentifierGenerator(ObjectProvider<LongIdGenerator> generators,
                                                                   ConfigurableListableBeanFactory beans) {
        List<LongIdGenerator> available = generators.orderedStream().toList();
        if (available.isEmpty() || !beans.containsBean("snowflakeIdGenerator")) {
            throw new EgonColaMybatisPlusConfigurationException("ID_GENERATOR_BEAN_MISSING");
        }
        if (available.size() != 1) {
            throw new EgonColaMybatisPlusConfigurationException("ID_GENERATOR_BEAN_AMBIGUOUS");
        }
        return new EgonColaIdentifierGenerator(beans.getBean("snowflakeIdGenerator", LongIdGenerator.class));
    }

    @Bean("egonColaTwoLevelRouteStrategy")
    public EgonColaTwoLevelRouteStrategy egonColaTwoLevelRouteStrategy(@Qualifier("egonColaValidationUtils") ValidationUtils validation) {
        return new EgonColaTwoLevelRouteStrategy(validation);
    }

    @Bean("egonColaPostgreDdlRunner")
    public EgonColaPostgreDdlRunner egonColaPostgreDdlRunner(@Qualifier("egonColaValidationUtils") ValidationUtils validation,
                                                             ObjectProvider<Clock> clocks, EgonColaMybatisPlusProperties properties) {
        return new EgonColaPostgreDdlRunner(validation, new org.springframework.core.io.support.PathMatchingResourcePatternResolver(),
                clocks.getIfUnique(Clock::systemUTC), properties.getDdl().getLockTimeout(), properties.getDdl().getStatementTimeout());
    }

    @Bean("egonColaOriginalSqlGuardInterceptor")
    @Order(Integer.MAX_VALUE)
    public EgonColaOriginalSqlGuardInterceptor egonColaOriginalSqlGuardInterceptor(EgonColaMybatisPlusProperties properties) {
        return new EgonColaOriginalSqlGuardInterceptor(properties);
    }

    @Bean("egonColaLocalWriteGuardInnerInterceptor")
    @Order(410)
    public EgonColaLocalWriteGuardInnerInterceptor egonColaLocalWriteGuardInnerInterceptor(
            EgonColaWriteTargetResolver resolver, EgonColaTenantIdGuardInnerInterceptor sqlGuard,
            EgonColaTwoLevelRouteStrategy strategy,
            @Qualifier("egonColaRoutingProfiles") Map<String, EgonColaRoutingProfileBO> profiles) {
        return new EgonColaLocalWriteGuardInnerInterceptor(resolver, sqlGuard, strategy, profiles);
    }

    @Bean("egonColaDynamicTableNameInnerInterceptor")
    @Order(250)
    @ConditionalOnProperty(prefix = EgonColaMybatisPlusProperties.PREFIX + ".dynamic-table-name", name = "enabled", havingValue = "true")
    public DynamicTableNameInnerInterceptor egonColaDynamicTableNameInnerInterceptor(EgonColaMybatisPlusProperties properties) {
        Map<String, String> tables = Map.copyOf(properties.getDynamicTableName().getTables());
        return new DynamicTableNameInnerInterceptor((sql, table) -> tables.getOrDefault(table, table));
    }

    @Bean("egonColaDataChangeRecorderInnerInterceptor")
    @Order(430)
    @Profile("dev & !prod")
    @ConditionalOnProperty(prefix = EgonColaMybatisPlusProperties.PREFIX + ".data-change-recorder",
            name = "enabled", havingValue = "true")
    @SuppressWarnings("deprecation")
    public EgonColaDataChangeRecorderInnerInterceptor egonColaDataChangeRecorderInnerInterceptor() {
        return new EgonColaDataChangeRecorderInnerInterceptor();
    }

    @Bean("egonColaIllegalSqlInnerInterceptor")
    @Order(450)
    @Profile("dev & !prod")
    @ConditionalOnProperty(prefix = EgonColaMybatisPlusProperties.PREFIX + ".illegal-sql", name = "enabled", havingValue = "true")
    @SuppressWarnings("deprecation")
    public IllegalSQLInnerInterceptor egonColaIllegalSqlInnerInterceptor() {
        return new IllegalSQLInnerInterceptor();
    }

    @Bean("mybatisPlusInterceptor")
    @Order(0)
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor mybatisPlusInterceptor(ObjectProvider<InnerInterceptor> interceptors) {
        MybatisPlusInterceptor outer = new MybatisPlusInterceptor();
        outer.setInterceptors(interceptors.orderedStream().toList());
        return outer;
    }

    @Bean("egonColaMybatisPlusContractValidator")
    public EgonColaMybatisPlusContractValidator egonColaMybatisPlusContractValidator(
            ObjectProvider<MybatisPlusInterceptor> outerProvider,
            ObjectProvider<com.baomidou.mybatisplus.core.handlers.MetaObjectHandler> handlerProvider,
            ObjectProvider<EgonColaModelValidationInterceptor> validationProvider,
            EgonColaMybatisPlusProperties properties, ConfigurableListableBeanFactory beanFactory, Environment environment,
            @Qualifier("egonColaRoutingProfiles") Map<String, EgonColaRoutingProfileBO> profiles) {
        // Providers intentionally aggregate every factory/handler; do not narrow them to @Primary.
        return new EgonColaMybatisPlusContractValidator(outerProvider, handlerProvider, validationProvider, properties,
                beanFactory, environment, profiles);
    }
}
