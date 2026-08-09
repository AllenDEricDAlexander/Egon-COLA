package top.egon.cola.component.ddc.autoconfigure;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.ddc.registry.DdcServiceKeyFactory;
import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.client.registry.HttpDdcServiceRegistryClient;

/**
 * 在显式启用服务注册时装配服务键工厂和注册客户端。 Configures the service-key factory and registry client when service registry is explicitly enabled.
 */
@AutoConfiguration(after = DdcAutoConfig.class)
@EnableConfigurationProperties(DdcProperties.class)
@ConditionalOnProperty(
        prefix = "egon.cola.component.ddc.registry",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class DdcRegistryAutoConfig {

    /**
     * 创建从 DDC 作用域属性生成服务键的工厂。 Creates the factory that derives service keys from DDC scope properties.
     *
     * @param properties DDC 属性。 DDC properties
     * @return 服务键工厂。 service-key factory
     */
    @Bean
    @ConditionalOnMissingBean(DdcServiceKeyFactory.class)
    public DdcServiceKeyFactory ddcServiceKeyFactory(
            DdcProperties properties) {
        return new DdcServiceKeyFactory(properties);
    }

    /**
     * 创建基于 DDC OpenAPI 语义和 Redis 存储的服务注册客户端。 Creates the service-registry client backed by DDC OpenAPI semantics and Redis storage.
     *
     * @param properties     DDC 属性。 DDC properties
     * @param redissonClient 共享 DDC Redisson 客户端。 shared DDC Redisson client
     * @return 可关闭的服务注册客户端。 closeable service-registry client
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnBean(name = "ddcRedissonClient")
    @ConditionalOnMissingBean(DdcServiceRegistryClient.class)
    public DdcServiceRegistryClient ddcServiceRegistryClient(
            DdcProperties properties,
            @Qualifier("ddcRedissonClient")
            RedissonClient redissonClient) {
        return new HttpDdcServiceRegistryClient(properties, redissonClient);
    }
}
