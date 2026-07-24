package top.egon.cola.component.ddc.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.ddc.registry.DdcOpenApiServiceRegistryClient;
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

    @Bean(name = "ddcRegistryRedissonClient", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient ddcRegistryRedissonClient(DdcProperties properties) {
        Config config = new Config();
        DdcProperties.Redis redis = properties.getRedis();
        config.useSingleServer()
                .setAddress("redis://" + redis.getHost() + ":" + redis.getPort())
                .setDatabase(redis.getDatabase());
        if (redis.getPassword() != null && !redis.getPassword().isBlank()) {
            config.useSingleServer().setPassword(redis.getPassword());
        }
        return Redisson.create(config);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean(DdcServiceRegistryClient.class)
    public DdcServiceRegistryClient ddcServiceRegistryClient(
            DdcProperties properties,
            RedissonClient redissonClient) {
        return new DdcOpenApiServiceRegistryClient(properties, redissonClient);
    }
}
