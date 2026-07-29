package top.egon.cola.component.accessguard.autoconfigure;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.accessguard.blacklist.BlacklistService;
import top.egon.cola.component.accessguard.blacklist.RedissonBlacklistService;
import top.egon.cola.component.accessguard.ratelimiter.LocalRateLimiterExecutor;
import top.egon.cola.component.accessguard.ratelimiter.RateLimiterExecutor;
import top.egon.cola.component.accessguard.ratelimiter.RedissonRateLimiterExecutor;
import top.egon.cola.component.accessguard.support.AccessGuardRedisKeys;
import top.egon.cola.component.accessguard.whitelist.RedissonWhiteListRepository;
import top.egon.cola.component.accessguard.whitelist.WhiteListRepository;

/**
 * Backs the guard with Redis when {@code storage=REDISSON}.
 *
 * <p>Registered before {@link AccessGuardAutoConfiguration} so its local defaults, which are all
 * {@code @ConditionalOnMissingBean}, back off. The application supplies the {@link RedissonClient};
 * this component never creates one, so an application without Redis is never forced to connect.
 */
@AutoConfiguration(before = AccessGuardAutoConfiguration.class)
@EnableConfigurationProperties(AccessGuardProperties.class)
@ConditionalOnClass(RedissonClient.class)
@ConditionalOnBean(RedissonClient.class)
@ConditionalOnProperty(
        prefix = "egon.cola.component.access-guard",
        name = "storage",
        havingValue = "REDISSON"
)
public class AccessGuardRedissonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AccessGuardRedisKeys accessGuardRedisKeys(AccessGuardProperties properties) {
        return new AccessGuardRedisKeys(
                properties.getKeyPrefix(), properties.getApp(), properties.getEnv());
    }

    @Bean
    @ConditionalOnMissingBean
    public WhiteListRepository whiteListRepository(
            BeanFactory beanFactory, AccessGuardProperties properties, AccessGuardRedisKeys redisKeys) {
        return new RedissonWhiteListRepository(resolveClient(beanFactory, properties), redisKeys);
    }

    @Bean
    @ConditionalOnMissingBean
    public BlacklistService blacklistService(
            BeanFactory beanFactory, AccessGuardProperties properties, AccessGuardRedisKeys redisKeys) {
        return new RedissonBlacklistService(resolveClient(beanFactory, properties), redisKeys);
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimiterExecutor rateLimiterExecutor(
            BeanFactory beanFactory, AccessGuardProperties properties, AccessGuardRedisKeys redisKeys) {
        return new RedissonRateLimiterExecutor(
                resolveClient(beanFactory, properties), redisKeys, new LocalRateLimiterExecutor());
    }

    /**
     * Looks the client up by the configured bean name so an application running several Redis
     * connections can choose which one carries governance state. Deliberately resolved rather than
     * republished as a bean, so this component never adds a second RedissonClient candidate.
     */
    private RedissonClient resolveClient(BeanFactory beanFactory, AccessGuardProperties properties) {
        String beanName = properties.getRedisson().getClientBeanName();
        if (beanName == null || beanName.isBlank()) {
            return beanFactory.getBean(RedissonClient.class);
        }
        return beanFactory.getBean(beanName, RedissonClient.class);
    }
}
