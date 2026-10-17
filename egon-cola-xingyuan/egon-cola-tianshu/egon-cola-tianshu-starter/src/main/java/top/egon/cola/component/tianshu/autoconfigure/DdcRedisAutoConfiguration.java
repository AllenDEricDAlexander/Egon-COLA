package top.egon.cola.component.tianshu.autoconfigure;

import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcTenantProperties;
import top.egon.cola.component.tianshu.redis.DdcRedisClientFactory;

/**
 * 为配置发布订阅与服务注册订阅装配共享的 Tianshu Redisson 客户端。 /
 * Configures the Redisson client shared by configuration-publication and service-registry subscriptions.
 */
@AutoConfiguration(before = {DdcAutoConfiguration.class, DdcRegistryAutoConfiguration.class})
@EnableConfigurationProperties({DdcProperties.class, DdcTenantProperties.class})
public class DdcRedisAutoConfiguration {

    /**
     * 在配置远程生命周期、服务注册或 AI Skill 工作区订阅任一启用时创建唯一 Tianshu Redisson 客户端。 /
     * Creates the sole Tianshu Redisson client when the remote configuration lifecycle, the service registry or the AI
     * Skill workspace subscription is enabled.
     *
     * @param properties Tianshu 属性 / Tianshu properties
     * @return 共享 Redisson 客户端 / shared Redisson client
     */
    @Bean(name = "ddcRedissonClient", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "ddcRedissonClient")
    @ConditionalOnExpression(
            "(${egon.cola.component.tianshu.enabled:false} && "
                    + "${egon.cola.component.tianshu.redis.enabled:true}) || "
                    + "${egon.cola.component.tianshu.registry.enabled:false} || "
                    + "${egon.cola.component.tianshu.ai.skill.workspace.subscription-enabled:false}"
    )
    public RedissonClient ddcRedissonClient(
            @Qualifier("egon.cola.component.tianshu-top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties")
            DdcProperties properties,
            @Qualifier("egon.cola.component.tianshu-top.egon.cola.component.tianshu.autoconfigure.properties.DdcTenantProperties")
            DdcTenantProperties tenantProperties) {
        DdcProperties.Redis redis = properties.getRedis();
        tenantProperties.requireTenantId();
        return DdcRedisClientFactory.create(
                redis.getMode(),
                redis.getNodes(),
                redis.getMasterName(),
                redis.getHost(),
                redis.getPort(),
                redis.getPassword(),
                tenantProperties.requireRedisUsername(),
                redis.getDatabase()
        );
    }
}
