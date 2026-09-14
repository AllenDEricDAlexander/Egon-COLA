package top.egon.cola.component.common.mybatis.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaTwoLevelRouteStrategy;
import top.egon.cola.component.common.mybatis.routing.EgonColaWriteTargetResolver;
import top.egon.cola.component.common.mybatis.schema.EgonColaTableInfoSchemaMaintainer;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaPhysicalDataSourceFactory;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingDataSourceBootstrapper;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingDataSourceBootstrapper.LogicalDataSourceFactory;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingTopologyValidator;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingYamlLoader;
import top.egon.cola.component.common.mybatis.sharding.resolver.EgonColaShardingWriteTargetResolver;
import top.egon.cola.component.common.mybatis.sharding.strategy.EgonColaBroadcastReadOnlyShardingStrategy;
import top.egon.cola.component.common.mybatis.sharding.strategy.EgonColaComplexTenantThenBusinessShardingStrategy;
import top.egon.cola.component.common.mybatis.sharding.strategy.EgonColaNativeYamlShardingStrategy;
import top.egon.cola.component.common.mybatis.sharding.strategy.EgonColaShardingStrategy;
import top.egon.cola.component.common.mybatis.sharding.strategy.EgonColaShardingStrategyFactory;
import top.egon.cola.component.common.mybatis.sharding.strategy.EgonColaSingleTableShardingStrategy;
import top.egon.cola.component.common.mybatis.sharding.strategy.EgonColaStandardTenantIdShardingStrategy;

import javax.sql.DataSource;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mandatory ShardingSphere logical DataSource. Missing YAML or disabled sharding fails startup.
 */
@Slf4j
@AutoConfiguration(after = EgonColaMybatisPlusAutoConfiguration.class)
@EnableConfigurationProperties(EgonColaShardingProperties.class)
@Import({
        EgonColaSingleTableShardingStrategy.class,
        EgonColaBroadcastReadOnlyShardingStrategy.class,
        EgonColaStandardTenantIdShardingStrategy.class,
        EgonColaComplexTenantThenBusinessShardingStrategy.class,
        EgonColaNativeYamlShardingStrategy.class,
        EgonColaShardingStrategyFactory.class,
        EgonColaPhysicalDataSourceFactory.class,
        EgonColaShardingYamlLoader.class,
        EgonColaShardingTopologyValidator.class,
        EgonColaTableInfoSchemaMaintainer.class
})
public class EgonColaShardingAutoConfiguration {

    @Bean("egonColaShardingStrategies")
    public Map<String, EgonColaShardingStrategy> egonColaShardingStrategies(List<EgonColaShardingStrategy> strategies) {
        Map<String, EgonColaShardingStrategy> registry = new LinkedHashMap<>();
        for (EgonColaShardingStrategy strategy : strategies) {
            registry.put(strategy.type(), strategy);
        }
        return registry;
    }

    @Bean("egonColaClock")
    @ConditionalOnMissingBean(name = "egonColaClock")
    public Clock egonColaClock() {
        return Clock.systemUTC();
    }

    @Bean("egonColaShardingLogicalDataSourceFactory")
    @ConditionalOnMissingBean(name = "egonColaShardingLogicalDataSourceFactory")
    public LogicalDataSourceFactory egonColaShardingLogicalDataSourceFactory() {
        return YamlShardingSphereDataSourceFactory::createDataSource;
    }

    @Bean("egonColaShardingDataSourceBootstrapper")
    public EgonColaShardingDataSourceBootstrapper egonColaShardingDataSourceBootstrapper(
            @Qualifier("egonColaPhysicalDataSourceFactory") EgonColaPhysicalDataSourceFactory pools,
            @Qualifier("egonColaShardingYamlLoader") EgonColaShardingYamlLoader yamlLoader,
            @Qualifier("egonColaShardingTopologyValidator") EgonColaShardingTopologyValidator validator,
            @Qualifier("egonColaShardingLogicalDataSourceFactory") LogicalDataSourceFactory logical) {
        return new EgonColaShardingDataSourceBootstrapper(pools, yamlLoader, validator, logical);
    }

    @Bean("dataSource")
    @Primary
    public DataSource dataSource(
            @Qualifier("egonColaShardingDataSourceBootstrapper") EgonColaShardingDataSourceBootstrapper bootstrapper,
            EgonColaShardingProperties properties) {
        if (properties == null || !properties.isEnabled()
                || properties.getDataSources() == null || properties.getDataSources().isEmpty()) {
            throw new EgonColaMybatisPlusConfigurationException("SHARDING_REQUIRED");
        }
        return bootstrapper.createDataSource(properties);
    }

    @Bean("egonColaRoutingProfiles")
    public Map<String, EgonColaRoutingProfileBO> egonColaRoutingProfiles(
            @Qualifier("dataSource") DataSource dataSource,
            @Qualifier("egonColaShardingDataSourceBootstrapper") EgonColaShardingDataSourceBootstrapper bootstrapper) {
        return bootstrapper.profiles();
    }

    @Bean("egonColaShardingRouteFingerprint")
    public String egonColaShardingRouteFingerprint(
            @Qualifier("egonColaShardingDataSourceBootstrapper") EgonColaShardingDataSourceBootstrapper bootstrapper) {
        return bootstrapper.fingerprint();
    }

    @Bean("egonColaWriteTargetResolver")
    public EgonColaWriteTargetResolver egonColaWriteTargetResolver(
            @Qualifier("egonColaRoutingProfiles") Map<String, EgonColaRoutingProfileBO> profiles,
            @Qualifier("egonColaTwoLevelRouteStrategy") EgonColaTwoLevelRouteStrategy strategy,
            @Qualifier("egonColaValidationUtils") ValidationUtils validationUtils,
            @Qualifier("egonColaShardingRouteFingerprint") String fingerprint) {
        return new EgonColaShardingWriteTargetResolver(profiles, strategy, validationUtils, fingerprint);
    }
}
