package ${package}.infrastructure.config.datasource;

import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import top.egon.cola.component.common.id.generator.UuidV7Generator;

/**
 * Activates the ShardingSphere logical data source for the sharding profile.
 */
@Configuration(proxyBeanMethods = false)
@Profile("sharding")
@EnableConfigurationProperties({
    ShardingDataSourceProperties.class,
    FlywayProperties.class
})
public class ShardingSphereDataSourceConfiguration {

    @Bean
    UuidV7Generator uuidV7Generator() {
        return new UuidV7Generator();
    }

    @Bean
    PhysicalDataSourceFactory physicalDataSourceFactory() {
        return new PhysicalDataSourceFactory();
    }

    @Bean
    ShardingYamlLoader shardingYamlLoader(
            ResourceLoader resourceLoader,
            Environment environment) {
        return new ShardingYamlLoader(resourceLoader, environment);
    }

    @Bean
    ShardingTopologyValidator shardingTopologyValidator() {
        return new ShardingTopologyValidator();
    }

    @Bean
    PhysicalDataSourceFlywayMigrator physicalDataSourceFlywayMigrator() {
        return new PhysicalDataSourceFlywayMigrator();
    }

    @Bean
    ShardingDataSourceBootstrapper shardingDataSourceBootstrapper(
            PhysicalDataSourceFactory physicalDataSourceFactory,
            ShardingYamlLoader shardingYamlLoader,
            ShardingTopologyValidator shardingTopologyValidator,
            PhysicalDataSourceFlywayMigrator physicalDataSourceFlywayMigrator) {
        return new ShardingDataSourceBootstrapper(
                physicalDataSourceFactory,
                shardingYamlLoader,
                shardingTopologyValidator,
                physicalDataSourceFlywayMigrator);
    }

    @Bean
    @Primary
    DataSource dataSource(
            ShardingDataSourceBootstrapper bootstrapper,
            ShardingDataSourceProperties properties,
            FlywayProperties flywayProperties) {
        return bootstrapper.createDataSource(properties, flywayProperties);
    }
}
