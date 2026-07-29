package top.egon.cola.component.accessguard.autoconfigure;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import top.egon.cola.component.accessguard.core.plan.GuardPlanProperties;
import top.egon.cola.component.accessguard.store.AccessGuardStorageIntegration;
import top.egon.cola.component.accessguard.store.AllowListStore;
import top.egon.cola.component.accessguard.store.DenyListStore;
import top.egon.cola.component.accessguard.store.PenaltyStore;
import top.egon.cola.component.accessguard.store.RateLimitBackend;
import top.egon.cola.component.accessguard.store.local.LocalAllowListStore;
import top.egon.cola.component.accessguard.store.local.LocalDenyListStore;
import top.egon.cola.component.accessguard.store.local.LocalPenaltyStore;
import top.egon.cola.component.accessguard.store.local.LocalRateLimitBackend;
import top.egon.cola.component.accessguard.store.redisson.AccessGuardRedisKeyFactory;
import top.egon.cola.component.accessguard.store.redisson.RedissonAllowListStore;
import top.egon.cola.component.accessguard.store.redisson.RedissonDenyListStore;
import top.egon.cola.component.accessguard.store.redisson.RedissonPenaltyStore;
import top.egon.cola.component.accessguard.store.redisson.RedissonRateLimitBackend;

import java.util.Arrays;

@AutoConfiguration(before = {
        AccessGuardLocalStoreAutoConfiguration.class,
        AccessGuardCoreAutoConfiguration.class
})
@EnableConfigurationProperties(GuardPlanProperties.class)
@ConditionalOnClass(RedissonClient.class)
@ConditionalOnProperty(
        prefix = GuardPlanProperties.PREFIX,
        name = "storage",
        havingValue = "REDISSON")
public class AccessGuardV2RedissonAutoConfiguration {

    @Bean
    SelectedRedissonClient accessGuardSelectedRedissonClient(
            ListableBeanFactory beanFactory,
            GuardPlanProperties properties
    ) {
        return new SelectedRedissonClient(resolveClient(beanFactory, properties.getRedisson().getClientBeanName()));
    }

    @Bean
    @ConditionalOnMissingBean
    AccessGuardRedisKeyFactory accessGuardRedisKeyFactory(
            GuardPlanProperties properties,
            Environment environment
    ) {
        String application = properties.getRedisson().getApplication();
        if (application == null || application.isBlank()) {
            application = environment.getProperty("spring.application.name", "");
        }
        if (application.isBlank()) {
            throw new IllegalStateException(
                    "REDISSON storage requires spring.application.name or access-guard.redisson.application");
        }
        return new AccessGuardRedisKeyFactory(properties.getRedisson().getKeyPrefix(), application);
    }

    @Bean
    @ConditionalOnMissingBean(value = DenyListStore.class, ignored = LocalDenyListStore.class)
    RedissonDenyListStore accessGuardRedissonDenyListStore(
            SelectedRedissonClient selected,
            AccessGuardRedisKeyFactory keyFactory
    ) {
        return new RedissonDenyListStore(selected.client(), keyFactory);
    }

    @Bean
    @ConditionalOnMissingBean(value = AllowListStore.class, ignored = LocalAllowListStore.class)
    RedissonAllowListStore accessGuardRedissonAllowListStore(
            SelectedRedissonClient selected,
            AccessGuardRedisKeyFactory keyFactory
    ) {
        return new RedissonAllowListStore(selected.client(), keyFactory);
    }

    @Bean
    @ConditionalOnMissingBean(value = PenaltyStore.class, ignored = LocalPenaltyStore.class)
    RedissonPenaltyStore accessGuardRedissonPenaltyStore(
            SelectedRedissonClient selected,
            AccessGuardRedisKeyFactory keyFactory
    ) {
        return new RedissonPenaltyStore(selected.client(), keyFactory);
    }

    @Bean
    @ConditionalOnMissingBean(value = RateLimitBackend.class, ignored = LocalRateLimitBackend.class)
    RedissonRateLimitBackend accessGuardRedissonRateLimitBackend(
            SelectedRedissonClient selected,
            AccessGuardRedisKeyFactory keyFactory,
            GuardPlanProperties properties
    ) {
        return new RedissonRateLimitBackend(
                selected.client(), keyFactory, properties.getLocal().getIdleTtl());
    }

    @Bean
    AccessGuardStorageIntegration accessGuardRedissonStorageIntegration() {
        return () -> GuardPlanProperties.Storage.REDISSON.name();
    }

    private static RedissonClient resolveClient(ListableBeanFactory beanFactory, String configuredName) {
        if (configuredName == null || configuredName.isBlank()) {
            String[] names = beanFactory.getBeanNamesForType(RedissonClient.class, false, false);
            if (names.length != 1) {
                throw new IllegalStateException(
                        "REDISSON storage requires exactly one RedissonClient when client-bean-name is blank; found "
                                + names.length + " " + Arrays.toString(names));
            }
            return beanFactory.getBean(names[0], RedissonClient.class);
        }
        String beanName = configuredName.trim();
        if (!beanFactory.containsBean(beanName)) {
            throw new IllegalStateException("Configured RedissonClient bean '" + beanName + "' was not found");
        }
        Class<?> beanType = beanFactory.getType(beanName, false);
        if (beanType == null || !RedissonClient.class.isAssignableFrom(beanType)) {
            throw new IllegalStateException(
                    "Configured bean '" + beanName + "' must be a RedissonClient but was "
                            + (beanType == null ? "unknown" : beanType.getName()));
        }
        return beanFactory.getBean(beanName, RedissonClient.class);
    }

    record SelectedRedissonClient(RedissonClient client) {
    }
}
