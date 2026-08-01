package top.egon.cola.component.ddc.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.ddc.registry.DdcOpenApiServiceRegistryClient;
import top.egon.cola.component.ddc.registry.DdcServiceKeyFactory;
import top.egon.cola.component.ddc.registry.DdcServiceRegistryClient;

@AutoConfiguration(after = DdcAutoConfig.class)
@EnableConfigurationProperties(DdcProperties.class)
@ConditionalOnProperty(
        prefix = "egon.cola.component.ddc.registry",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class DdcRegistryAutoConfig {

    @Bean
    @ConditionalOnMissingBean(DdcServiceKeyFactory.class)
    public DdcServiceKeyFactory ddcServiceKeyFactory(
            DdcProperties properties) {
        return new DdcServiceKeyFactory(properties);
    }

    @Bean(name = "ddcRegistryRedissonClient", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "ddcRegistryRedissonClient")
    public RedissonClient ddcRegistryRedissonClient(DdcProperties properties) {
        DdcProperties.Redis redis = properties.getRedis();
        return Redisson.create(DdcRedisTopology.create(
                redis.getMode(),
                redis.getNodes(),
                redis.getMasterName(),
                redis.getHost(),
                redis.getPort(),
                redis.getPassword(),
                redis.getDatabase()
        ));
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnBean(name = "ddcRegistryRedissonClient")
    @ConditionalOnMissingBean(DdcServiceRegistryClient.class)
    public DdcServiceRegistryClient ddcServiceRegistryClient(
            DdcProperties properties,
            @Qualifier("ddcRegistryRedissonClient")
            RedissonClient redissonClient) {
        return new DdcOpenApiServiceRegistryClient(properties, redissonClient);
    }
}
