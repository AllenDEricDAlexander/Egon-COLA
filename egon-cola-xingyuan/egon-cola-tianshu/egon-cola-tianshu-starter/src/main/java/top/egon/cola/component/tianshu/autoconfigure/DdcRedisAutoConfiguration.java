package top.egon.cola.component.tianshu.autoconfigure;

import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.tianshu.redis.DdcRedisClientFactory;

/**
 * 为配置发布订阅与服务注册订阅装配共享的 Tianshu Redisson 客户端。 /
 * Configures the Redisson client shared by configuration-publication and service-registry subscriptions.
 */
@AutoConfiguration(before = {DdcAutoConfiguration.class, DdcRegistryAutoConfiguration.class})
@EnableConfigurationProperties(DdcProperties.class)
public class DdcRedisAutoConfiguration {

    /**
     * 在配置远程生命周期或服务注册任一启用时创建唯一 Tianshu Redisson 客户端。 /
     * Creates the sole Tianshu Redisson client when either remote configuration lifecycle or service registry is enabled.
     *
     * @param properties Tianshu 属性 / Tianshu properties
     * @return 共享 Redisson 客户端 / shared Redisson client
     */
    @Bean(name = "ddcRedissonClient", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "ddcRedissonClient")
    @ConditionalOnExpression(
            "(${egon.cola.component.tianshu.enabled:false} && "
                    + "${egon.cola.component.tianshu.redis.enabled:true}) || "
                    + "${egon.cola.component.tianshu.registry.enabled:false}"
    )
    public RedissonClient ddcRedissonClient(DdcProperties properties) {
        DdcProperties.Redis redis = properties.getRedis();
        return DdcRedisClientFactory.create(
                redis.getMode(),
                redis.getNodes(),
                redis.getMasterName(),
                redis.getHost(),
                redis.getPort(),
                redis.getPassword(),
                redis.getDatabase()
        );
    }
}
