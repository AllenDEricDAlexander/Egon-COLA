package top.egon.cola.archetype.source.service.infrastructure.config.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.ddl.EgonColaPostgreDdlRunner;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaTwoLevelRouteStrategy;
import top.egon.cola.component.common.mybatis.routing.EgonColaWriteTargetResolver;

import javax.sql.DataSource;
import java.util.Map;

/** Owns the logical datasource and publishes the exact same immutable policy to the common guards. */
@Configuration(value = "shardingSphereDataSourceConfiguration", proxyBeanMethods = false)
@EnableConfigurationProperties(DataSourceModeProperties.class)
public class ShardingSphereDataSourceConfiguration {
    @Bean("shardingDataSourcePropertiesLoader")
    ShardingDataSourcePropertiesLoader shardingDataSourcePropertiesLoader(Environment environment,
            @Qualifier("egonColaValidationUtils") ValidationUtils validation) {
        return new ShardingDataSourcePropertiesLoader(environment, validation);
    }

    @Bean("shardingDataSourceProperties")
    ShardingDataSourceProperties shardingDataSourceProperties(ShardingDataSourcePropertiesLoader loader, DataSourceModeProperties mode) {
        return loader.load(mode);
    }

    @Bean("physicalDataSourceFactory")
    PhysicalDataSourceFactory physicalDataSourceFactory() { return new PhysicalDataSourceFactory(); }

    @Bean("shardingYamlLoader")
    ShardingYamlLoader shardingYamlLoader(ResourceLoader resources, Environment environment) {
        return new ShardingYamlLoader(resources, environment);
    }

    @Bean("shardingTopologyValidator")
    ShardingTopologyValidator shardingTopologyValidator(@Qualifier("egonColaValidationUtils") ValidationUtils validation) {
        return new ShardingTopologyValidator(validation);
    }

    @Bean("shardingDataSourceBootstrapper")
    ShardingDataSourceBootstrapper shardingDataSourceBootstrapper(PhysicalDataSourceFactory pools, ShardingYamlLoader yaml,
            ShardingTopologyValidator topology, EgonColaPostgreDdlRunner runner, ObjectMapper mapper, EgonColaMybatisPlusProperties policy) {
        return new ShardingDataSourceBootstrapper(pools, yaml, topology, runner, mapper, policy,
                YamlShardingSphereDataSourceFactory::createDataSource);
    }

    @Bean("dataSource")
    @Primary
    DataSource dataSource(ShardingDataSourceBootstrapper bootstrapper, ShardingDataSourceProperties properties) {
        return bootstrapper.createDataSource(properties);
    }

    @Bean("egonColaRoutingProfiles")
    Map<String, EgonColaRoutingProfileBO> egonColaRoutingProfiles(@Qualifier("dataSource") DataSource source,
            ShardingDataSourceBootstrapper bootstrapper) {
        return bootstrapper.profiles();
    }

    @Bean("egonColaWriteTargetResolver")
    EgonColaWriteTargetResolver egonColaWriteTargetResolver(@Qualifier("dataSource") DataSource source,
            ShardingDataSourceBootstrapper bootstrapper, ShardingDataSourceProperties properties,
            EgonColaTwoLevelRouteStrategy strategy, @Qualifier("egonColaValidationUtils") ValidationUtils validation) {
        return new ShardingWriteTargetResolver(bootstrapper.profiles(), ShardingNodeMap.parse(ShardingTopologyValidator.routing(properties.routing())),
                strategy, validation, bootstrapper.fingerprint());
    }
}
