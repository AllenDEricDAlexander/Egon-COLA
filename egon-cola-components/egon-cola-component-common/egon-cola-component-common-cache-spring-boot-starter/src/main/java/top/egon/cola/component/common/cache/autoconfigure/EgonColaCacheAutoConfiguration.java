package top.egon.cola.component.common.cache.autoconfigure;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.common.cache.core.EgonColaTwoLevelCacheManager;
import top.egon.cola.component.common.cache.event.EgonColaCacheChangedListener;
import top.egon.cola.component.common.cache.port.EgonColaTwoLevelCachePort;

/**
 * 两级缓存组件条件装配（REQ-001/REQ-020）：缺省关闭；宿主已有任意 {@link CacheManager}
 * 时整组让位；RedissonClient 绝不自建——按名优先、唯一兜底，两者皆空 fail-fast。
 */
@AutoConfiguration(before = CacheAutoConfiguration.class)
@ConditionalOnProperty(prefix = "egon.cola.component.cache", name = "enabled", havingValue = "true")
@ConditionalOnMissingBean(CacheManager.class)
@EnableConfigurationProperties(EgonColaCacheProperties.class)
public class EgonColaCacheAutoConfiguration {

    private static final String REDISSON_CLIENT_BEAN_NAME = "redissonClient";

    @Bean("egonColaTwoLevelCacheManager")
    public EgonColaTwoLevelCacheManager egonColaTwoLevelCacheManager(
            EgonColaCacheProperties properties,
            ObjectProvider<RedissonClient> clients,
            BeanFactory beanFactory) {
        return new EgonColaTwoLevelCacheManager(properties, resolveRedissonClient(clients, beanFactory));
    }

    @Bean("egonColaCacheChangedListener")
    public EgonColaCacheChangedListener egonColaCacheChangedListener(
            @Qualifier("egonColaTwoLevelCacheManager") EgonColaTwoLevelCacheManager manager,
            EgonColaCacheProperties properties) {
        return new EgonColaCacheChangedListener(manager, properties);
    }

    @Bean("egonColaCachePort")
    public EgonColaTwoLevelCachePort egonColaCachePort(
            @Qualifier("egonColaTwoLevelCacheManager") EgonColaTwoLevelCacheManager manager,
            EgonColaCacheProperties properties) {
        return new EgonColaTwoLevelCachePort(manager, properties);
    }

    private static RedissonClient resolveRedissonClient(ObjectProvider<RedissonClient> clients,
                                                        BeanFactory beanFactory) {
        if (beanFactory.containsBean(REDISSON_CLIENT_BEAN_NAME)) {
            return beanFactory.getBean(REDISSON_CLIENT_BEAN_NAME, RedissonClient.class);
        }
        RedissonClient unique = clients.getIfUnique();
        if (unique == null) {
            throw new IllegalStateException("CACHE_REDISSON_CLIENT_MISSING");
        }
        return unique;
    }
}
