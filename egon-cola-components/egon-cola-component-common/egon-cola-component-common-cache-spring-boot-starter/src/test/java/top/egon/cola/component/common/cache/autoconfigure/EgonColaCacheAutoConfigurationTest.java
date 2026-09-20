package top.egon.cola.component.common.cache.autoconfigure;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.redisson.api.RTopic;
import org.redisson.client.codec.Codec;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import top.egon.cola.component.common.cache.core.EgonColaTwoLevelCacheManager;
import top.egon.cola.component.common.cache.event.EgonColaCacheChangedListener;
import top.egon.cola.component.common.cache.port.EgonColaTwoLevelCachePort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class EgonColaCacheAutoConfigurationTest {

    private static RedissonClient stubClient() {
        RedissonClient client = mock(RedissonClient.class);
        lenient().when(client.getTopic(anyString(), any(Codec.class))).thenReturn(mock(RTopic.class));
        return client;
    }

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(EgonColaCacheAutoConfiguration.class));
    }

    private ApplicationContextRunner enabledWith(RedissonClient client) {
        return runner()
                .withPropertyValues("egon.cola.component.cache.enabled=true")
                .withBean("redissonClient", RedissonClient.class, () -> client);
    }

    /** runner 可能直抛启动异常，也可能注入失败上下文：两种剖面都要求因果链含 fail-fast ISE。 */
    private static void assertFailFastWithMissingClient(ApplicationContextRunner runner) {
        Throwable failure = catchThrowable(() -> runner.run(context -> assertThat(context).hasFailed()));
        if (failure == null) {
            return;
        }
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof IllegalStateException
                    && current.getMessage() != null
                    && current.getMessage().contains("CACHE_REDISSON_CLIENT_MISSING")) {
                return;
            }
        }
        throw new AssertionError("no IllegalStateException(CACHE_REDISSON_CLIENT_MISSING) in cause chain",
                failure);
    }

    @Test
    void disabledByDefaultShowsNoBeans() {
        runner().run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(EgonColaTwoLevelCacheManager.class);
            assertThat(context).doesNotHaveBean(EgonColaCacheChangedListener.class);
            assertThat(context).doesNotHaveBean(EgonColaTwoLevelCachePort.class);
        });
    }

    @Test
    void explicitlyDisabledShowsNoBeans() {
        runner().withPropertyValues("egon.cola.component.cache.enabled=false")
                .withBean("redissonClient", RedissonClient.class,
                        EgonColaCacheAutoConfigurationTest::stubClient)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(EgonColaTwoLevelCacheManager.class);
                    assertThat(context).doesNotHaveBean(EgonColaCacheChangedListener.class);
                    assertThat(context).doesNotHaveBean(EgonColaTwoLevelCachePort.class);
                });
    }

    @Test
    void enabledWithUniqueClientRegistersThreeNamedBeans() {
        enabledWith(stubClient()).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.containsBean("egonColaTwoLevelCacheManager")).isTrue();
            assertThat(context.containsBean("egonColaCacheChangedListener")).isTrue();
            assertThat(context.containsBean("egonColaCachePort")).isTrue();
            assertThat(context.getBean(EgonColaTwoLevelCacheManager.class))
                    .isSameAs(context.getBean("egonColaTwoLevelCacheManager"));
            assertThat(context.getBean(EgonColaCacheChangedListener.class))
                    .isSameAs(context.getBean("egonColaCacheChangedListener"));
            assertThat(context.getBean(EgonColaTwoLevelCachePort.class))
                    .isSameAs(context.getBean("egonColaCachePort"));
            assertThat(context.getBean(EgonColaCacheProperties.class).getSecondEvictDelay().toMillis())
                    .isEqualTo(5000);
        });
    }

    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @org.springframework.cache.annotation.EnableCaching
    static class CachingConfiguration {
    }

    @Test
    void winsBeforeBootDefaultCacheManagerWhenCachingIsEnabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(EgonColaCacheAutoConfiguration.class,
                        org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration.class))
                .withUserConfiguration(CachingConfiguration.class)
                .withPropertyValues("egon.cola.component.cache.enabled=true")
                .withBean("redissonClient", RedissonClient.class, EgonColaCacheAutoConfigurationTest::stubClient)
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(CacheManager.class);
                    assertThat(context.getBean(CacheManager.class)).isInstanceOf(EgonColaTwoLevelCacheManager.class);
                });
    }

    @Test
    void enabledWithoutClientFailsFastWithMissingCode() {
        assertFailFastWithMissingClient(runner()
                .withPropertyValues("egon.cola.component.cache.enabled=true"));
    }

    @Test
    void hostCacheManagerWinsAndWholeTrioYields() {
        CacheManager host = mock(CacheManager.class);
        enabledWith(stubClient())
                .withBean("customCacheManager", CacheManager.class, () -> host)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(EgonColaTwoLevelCacheManager.class);
                    assertThat(context).doesNotHaveBean(EgonColaCacheChangedListener.class);
                    assertThat(context).doesNotHaveBean(EgonColaTwoLevelCachePort.class);
                    assertThat(context.getBean(CacheManager.class)).isSameAs(host);
                });
    }

    @Test
    void namedRedissonClientBeatsNonUnique() {
        runner()
                .withPropertyValues("egon.cola.component.cache.enabled=true")
                .withBean("redissonClient", RedissonClient.class,
                        EgonColaCacheAutoConfigurationTest::stubClient)
                .withBean("auxiliaryClient", RedissonClient.class,
                        EgonColaCacheAutoConfigurationTest::stubClient)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.containsBean("egonColaTwoLevelCacheManager")).isTrue();
                });
    }

    @Test
    void unnamedDuplicateClientsFailFastAsAmbiguous() {
        assertFailFastWithMissingClient(runner()
                .withPropertyValues("egon.cola.component.cache.enabled=true")
                .withBean("firstClient", RedissonClient.class,
                        EgonColaCacheAutoConfigurationTest::stubClient)
                .withBean("secondClient", RedissonClient.class,
                        EgonColaCacheAutoConfigurationTest::stubClient));
    }

    @Test
    void contextCloseStopsLifecycleAndStopIsIdempotent() {
        enabledWith(stubClient()).run(context -> assertThat(context).hasNotFailed());

        // runner 关闭即 SmartLifecycle stop；独立复核 stop 幂等与恰一次反注册
        EgonColaTwoLevelCacheManager manager = mock(EgonColaTwoLevelCacheManager.class);
        RTopic topic = mock(RTopic.class);
        lenient().when(manager.topic()).thenReturn(topic);
        lenient().when(topic.addListener(any(), any())).thenReturn(3);
        EgonColaCacheChangedListener listener =
                new EgonColaCacheChangedListener(manager, new EgonColaCacheProperties());
        listener.start();
        listener.stop();
        listener.stop();
        verify(topic, times(1)).removeListener(3);
    }
}
