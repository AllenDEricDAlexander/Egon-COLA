package top.egon.cola.component.accessguard.autoconfigure;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.accessguard.store.RateLimitBackend;
import top.egon.cola.component.accessguard.store.local.LocalRateLimitBackend;
import top.egon.cola.component.accessguard.store.redisson.RedissonRateLimitBackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AccessGuardRedissonAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    AccessGuardCoreAutoConfiguration.class,
                    AccessGuardLocalStoreAutoConfiguration.class,
                    AccessGuardRedissonAutoConfiguration.class,
                    AccessGuardTimeLimitAutoConfiguration.class))
            .withPropertyValues("spring.application.name=test");

    @Test
    void missingConfiguredClientFailsInsteadOfSilentlyUsingLocalStorage() {
        contextRunner.withPropertyValues(
                        "egon.cola.component.access-guard.storage=REDISSON",
                        "egon.cola.component.access-guard.redisson.client-bean-name=guardRedis")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasMessageContaining("guardRedis"));
    }

    @Test
    void configuredNameMustResolveToARedissonClient() {
        contextRunner.withUserConfiguration(WrongClientConfiguration.class)
                .withPropertyValues(
                        "egon.cola.component.access-guard.storage=REDISSON",
                        "egon.cola.component.access-guard.redisson.client-bean-name=guardRedis")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasMessageContaining("RedissonClient"));
    }

    @Test
    void blankClientNameMustResolveExactlyOneCandidate() {
        contextRunner.withUserConfiguration(TwoRedissonClientsConfiguration.class)
                .withPropertyValues(
                        "egon.cola.component.access-guard.storage=REDISSON",
                        "egon.cola.component.access-guard.redisson.client-bean-name=")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasMessageContaining("exactly one RedissonClient")
                        .hasMessageContaining("found 2"));
    }

    @Test
    void selectedRedissonStoresArePrimaryWhileLocalFallbackRemainsAvailable() {
        contextRunner.withUserConfiguration(RedissonClientConfiguration.class)
                .withPropertyValues("egon.cola.component.access-guard.storage=REDISSON")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(RateLimitBackend.class))
                            .isInstanceOf(RedissonRateLimitBackend.class);
                    assertThat(context).hasSingleBean(LocalRateLimitBackend.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class WrongClientConfiguration {

        @Bean
        String guardRedis() {
            return "wrong";
        }
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
        RedissonClient firstRedissonClient() {
            return mock(RedissonClient.class);
        }

        @Bean
        RedissonClient secondRedissonClient() {
            return mock(RedissonClient.class);
        }
    }
}
