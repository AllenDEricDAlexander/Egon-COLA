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
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import top.egon.cola.component.common.cache.autoconfigure.EgonColaCacheProperties;
import top.egon.cola.component.common.cache.core.EgonColaTwoLevelCacheManager;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.id.autoconfigure.IdGeneratorAutoConfiguration;
import top.egon.cola.component.common.mybatis.business.EgonColaMdcUserIdProvider;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdTenantLineHandler;
import top.egon.cola.component.common.mybatis.business.EgonColaUserIdProvider;
import top.egon.cola.component.common.mybatis.cache.EgonColaRepositoryKeyGenerator;
import top.egon.cola.component.common.mybatis.ddl.EgonColaPostgreDdlRunner;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;
import top.egon.cola.component.common.mybatis.handler.EgonColaMetaObjectHandler;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaLocalWriteGuardInnerInterceptor;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaModelValidationInterceptor;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaOriginalSqlGuardInterceptor;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaTenantIdGuardInnerInterceptor;
import top.egon.cola.component.common.mybatis.model.EgonColaIdentifierGenerator;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils.Binding;
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
public class EgonColaMybatisPlusAutoConfiguration implements AutoCloseable {

    private final ValidationUtils validationUtils;
    private final Binding modelValidationBinding;

    public EgonColaMybatisPlusAutoConfiguration(EgonColaMybatisPlusProperties properties,
                                                ObjectProvider<Validator> validatorProvider) {
        EgonColaTenantIdProvider.initialize(properties.getTenantId().getMdcKey());
        Validator validator = validatorProvider.getIfAvailable();
        if (validator == null) {
            throw new EgonColaMybatisPlusConfigurationException("VALIDATOR_BEAN_MISSING");
        }
        this.validationUtils = new ValidationUtils(validator);
        Binding live = EgonColaModelValidationUtils.current();
        this.modelValidationBinding = live == null
                ? EgonColaModelValidationUtils.initialize(this.validationUtils, this) : live;
    }

    @Override
    public void close() {
        if (modelValidationBinding.owner() == this) {
            modelValidationBinding.close();
        }
    }

    @Bean("egonColaMybatisPlusClock")
    @ConditionalOnMissingBean(Clock.class)
    public Clock egonColaMybatisPlusClock() {
        return Clock.systemUTC();
    }

    @Bean("egonColaMdcUserIdProvider")
    @ConditionalOnMissingBean(EgonColaUserIdProvider.class)
    public EgonColaUserIdProvider egonColaMdcUserIdProvider(
            EgonColaMybatisPlusProperties properties) {
        return new EgonColaMdcUserIdProvider(properties);
    }

    @Bean("egonColaValidationUtils")
    // The host may also introduce other components that provide Validator Utils (such as agent flow), so the assembled
    // facade is published by name and bound once to the static model validation entry above.
    public ValidationUtils egonColaValidationUtils() {
        return validationUtils;
    }

    @Bean("egonColaModelValidationInterceptor")
    public EgonColaModelValidationInterceptor egonColaModelValidationInterceptor() {
        return new EgonColaModelValidationInterceptor();
    }

    @Bean("egonColaMetaObjectHandler")
    @ConditionalOnMissingBean(com.baomidou.mybatisplus.core.handlers.MetaObjectHandler.class)
    // The host may have multiple named Clocks (such as agentClock and agentFlowClock), and injecting them by type can be ambiguous;
    // When it is unique (or @ Primary), use the host clock. Otherwise, return to systemUTC and maintain the existing semantics of 'host can override'.
    public EgonColaMetaObjectHandler egonColaMetaObjectHandler(
            EgonColaUserIdProvider userIdProvider,
            ObjectProvider<Clock> clockProvider) {
        return new EgonColaMetaObjectHandler(userIdProvider, clockProvider.getIfUnique(Clock::systemUTC));
    }

    @Bean("egonColaTenantIdGuardInnerInterceptor")
    @Order(100)
    public EgonColaTenantIdGuardInnerInterceptor egonColaTenantIdGuardInnerInterceptor(
            EgonColaUserIdProvider userIdProvider,
            EgonColaMybatisPlusProperties properties) {
        return new EgonColaTenantIdGuardInnerInterceptor(userIdProvider, properties);
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
            EgonColaMybatisPlusProperties properties) {
        TenantLineHandler handler = new EgonColaTenantIdTenantLineHandler(properties);
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
    public EgonColaIdentifierGenerator egonColaIdentifierGenerator() {
        return new EgonColaIdentifierGenerator();
    }

    @Bean("egonColaRepositoryKeyGenerator")
    public KeyGenerator egonColaRepositoryKeyGenerator(
            ObjectProvider<CacheManager> cacheManagers, Environment environment) {
        requireTenantScopedCache(cacheManagers, environment);
        return new EgonColaRepositoryKeyGenerator();
    }

    /**
     * 必需二级缓存的启动裁决（REQ-019/REQ-021）：{@code tenant:id} 键只有在租户隔离缓存真正
     * 生效时才有意义。组件缺省开启，因此一旦本上下文里存在别的 CacheManager——典型是宿主自管
     * 实现让整组让位，或缓存自动装配被排除后 Spring 的内存实现顶上——都立即失败，而不是把租户键
     * 交给一个不隔离租户的缓存。显式 {@code enabled=false}（离线剖面）与"本上下文没有任何
     * CacheManager"（切片上下文）都不作假阳性失败；RedissonClient 缺失/歧义由缓存组件自身的
     * fail-fast 承担，此处不复述。
     */
    private static void requireTenantScopedCache(ObjectProvider<CacheManager> cacheManagers,
                                                 Environment environment) {
        if (!environment.getProperty(EgonColaCacheProperties.PREFIX + ".enabled", Boolean.class, true)) {
            return;
        }
        List<CacheManager> managers = cacheManagers.orderedStream().toList();
        boolean twoLevel = managers.stream().anyMatch(EgonColaTwoLevelCacheManager.class::isInstance);
        if (twoLevel || managers.isEmpty()) {
            return;
        }
        throw new EgonColaMybatisPlusConfigurationException("CACHE_MANAGER_INCOMPATIBLE");
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
