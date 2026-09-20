package top.egon.cola.component.common.cache.contract;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.redisson.api.RTopic;
import org.redisson.client.codec.Codec;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.common.cache.autoconfigure.EgonColaCacheAutoConfiguration;
import top.egon.cola.component.common.cache.autoconfigure.EgonColaCacheProperties;
import top.egon.cola.component.common.cache.core.EgonColaTwoLevelCacheManager;
import top.egon.cola.component.common.cache.event.EgonColaCacheChangedListener;
import top.egon.cola.component.common.cache.port.EgonColaTwoLevelCachePort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * REQ-019/REQ-020 冻结的必需二级缓存装配合同：组件默认开启、Redis 客户端缺失即 fail-fast、
 * 旧共享 TTL 键混用直接拒绝绑定而不是静默忽略。
 */
class RequiredTwoLevelCacheContractTest {

    private static RedissonClient stubClient() {
        RedissonClient client = mock(RedissonClient.class);
        lenient().when(client.getTopic(anyString(), any(Codec.class))).thenReturn(mock(RTopic.class));
        return client;
    }

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(EgonColaCacheAutoConfiguration.class));
    }

    @Test
    void requiredComponentIsEnabledByDefault() {
        assertThat(new EgonColaCacheProperties().isEnabled()).isTrue();
    }

    @Test
    void assemblyNeedsNoExplicitEnableSwitch() {
        runner().withBean("redissonClient", RedissonClient.class, RequiredTwoLevelCacheContractTest::stubClient)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.containsBean("egonColaTwoLevelCacheManager")).isTrue();
                    assertThat(context.containsBean("egonColaCacheChangedListener")).isTrue();
                    assertThat(context.containsBean("egonColaCachePort")).isTrue();
                });
    }

    @Test
    void missingRedissonClientFailsFastByDefault() {
        runner().run(context -> {
            assertThat(context).hasFailed();
            assertThat(rootMessage(context.getStartupFailure()))
                    .contains("CACHE_REDISSON_CLIENT_MISSING");
        });
    }

    @Test
    void explicitDisableStillYieldsWholeTrio() {
        runner().withPropertyValues("egon.cola.component.cache.enabled=false")
                .withBean("redissonClient", RedissonClient.class, RequiredTwoLevelCacheContractTest::stubClient)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(EgonColaTwoLevelCacheManager.class);
                    assertThat(context).doesNotHaveBean(EgonColaCacheChangedListener.class);
                    assertThat(context).doesNotHaveBean(EgonColaTwoLevelCachePort.class);
                });
    }

    @Test
    void hostCacheManagerStillWinsWhenComponentDefaultsToOn() {
        CacheManager host = mock(CacheManager.class);
        runner().withBean("redissonClient", RedissonClient.class, RequiredTwoLevelCacheContractTest::stubClient)
                .withBean("customCacheManager", CacheManager.class, () -> host)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(EgonColaTwoLevelCacheManager.class);
                    assertThat(context.getBean(CacheManager.class)).isSameAs(host);
                });
    }

    @Test
    void legacySharedTtlKeysAreRejectedNotIgnored() {
        assertBindingRejects("egon.cola.component.cache.ttl.expire=PT30M",
                "egon.cola.component.cache.ttl.expire");
        assertBindingRejects("egon.cola.component.cache.ttl.jitter-ratio=0.1",
                "egon.cola.component.cache.ttl.jitter-ratio");
        assertBindingRejects("egon.cola.component.cache.regions.users.expire=PT10S",
                "egon.cola.component.cache.regions.users.expire");
        assertBindingRejects("egon.cola.component.cache.unknown-switch=true",
                "egon.cola.component.cache.unknown-switch");
    }

    private void assertBindingRejects(String property, String fragment) {
        new ApplicationContextRunner()
                .withUserConfiguration(PropertiesConfiguration.class)
                .withPropertyValues(property)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(rootMessage(context.getStartupFailure())).contains(fragment);
                });
    }

    private static String rootMessage(Throwable failure) {
        Throwable current = failure;
        while (current != null && current.getCause() != null) {
            current = current.getCause();
        }
        return String.valueOf(current == null ? null : current.getMessage());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(EgonColaCacheProperties.class)
    static class PropertiesConfiguration {
    }
}
