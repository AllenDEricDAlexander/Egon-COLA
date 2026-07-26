package top.egon.cola.component.accessguard.autoconfigure;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.accessguard.blacklist.BlacklistService;
import top.egon.cola.component.accessguard.blacklist.RedissonBlacklistService;
import top.egon.cola.component.accessguard.ratelimiter.LocalRateLimiterExecutor;
import top.egon.cola.component.accessguard.ratelimiter.RateLimiterExecutor;
import top.egon.cola.component.accessguard.ratelimiter.RedissonRateLimiterExecutor;
import top.egon.cola.component.accessguard.support.AccessGuardRedisKeys;
import top.egon.cola.component.accessguard.whitelist.RedissonWhiteListRepository;
import top.egon.cola.component.accessguard.whitelist.WhiteListRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AccessGuardRedissonAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    AccessGuardRedissonAutoConfiguration.class, AccessGuardAutoConfiguration.class));

    @Test
    void startsOnLocalImplementationsWithoutRedis() {
        contextRunner.run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(RateLimiterExecutor.class)
                .doesNotHaveBean(RedissonClient.class)
                .doesNotHaveBean(AccessGuardRedisKeys.class)
                .getBean(RateLimiterExecutor.class).isInstanceOf(LocalRateLimiterExecutor.class));
    }

    @Test
    void staysLocalWhenRedissonIsPresentButStorageIsNotSelected() {
        contextRunner.withUserConfiguration(RedissonClientConfiguration.class)
                .run(context -> assertThat(context)
                        .getBean(RateLimiterExecutor.class)
                        .isInstanceOf(LocalRateLimiterExecutor.class));
    }

    @Test
    void wiresRedisBackedGovernanceWhenStorageIsRedisson() {
        contextRunner.withUserConfiguration(RedissonClientConfiguration.class)
                .withPropertyValues("egon.cola.component.access-guard.storage=REDISSON")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(AccessGuardRedisKeys.class);
                    assertThat(context).getBean(RateLimiterExecutor.class)
                            .isInstanceOf(RedissonRateLimiterExecutor.class);
                    assertThat(context).getBean(BlacklistService.class)
                            .isInstanceOf(RedissonBlacklistService.class);
                    assertThat(context).getBean(WhiteListRepository.class)
                            .isInstanceOf(RedissonWhiteListRepository.class);
                });
    }

    @Test
    void honoursTheConfiguredKeyPrefixAppAndEnv() {
        contextRunner.withUserConfiguration(RedissonClientConfiguration.class)
                .withPropertyValues(
                        "egon.cola.component.access-guard.storage=REDISSON",
                        "egon.cola.component.access-guard.key-prefix=custom:guard",
                        "egon.cola.component.access-guard.app=draw",
                        "egon.cola.component.access-guard.env=prod")
                .run(context -> assertThat(context.getBean(AccessGuardRedisKeys.class)
                        .limiter("draw-api", "hash001"))
                        .isEqualTo("custom:guard:draw:prod:draw-api:hash001:limiter"));
    }

    @Test
    void resolvesTheClientByConfiguredBeanName() {
        contextRunner.withUserConfiguration(TwoRedissonClientsConfiguration.class)
                .withPropertyValues(
                        "egon.cola.component.access-guard.storage=REDISSON",
                        "egon.cola.component.access-guard.redisson.client-bean-name=guardRedissonClient")
                .run(context -> assertThat(context).hasNotFailed()
                        .hasSingleBean(AccessGuardRedisKeys.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class RedissonClientConfiguration {
        @Bean
        RedissonClient redissonClient() {
            return mock(RedissonClient.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TwoRedissonClientsConfiguration {
        @Bean
        RedissonClient cacheRedissonClient() {
            return mock(RedissonClient.class);
        }

        @Bean
        RedissonClient guardRedissonClient() {
            return mock(RedissonClient.class);
        }
    }
}
