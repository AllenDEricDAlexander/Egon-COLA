package top.egon.cola.component.common.cache.port;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RMapCache;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.slf4j.MDC;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import top.egon.cola.component.common.cache.autoconfigure.EgonColaCacheProperties;
import top.egon.cola.component.common.cache.codec.EgonColaCacheCodecs;
import top.egon.cola.component.common.cache.core.EgonColaTwoLevelCacheManager;
import top.egon.cola.component.common.cache.support.CacheRedisTestSupport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static top.egon.cola.component.common.cache.event.EgonColaCacheChangedOperation.EVICT;
import static top.egon.cola.component.common.cache.event.EgonColaCacheChangedOperation.PREFIX_EVICT;

class EgonColaTwoLevelCachePortEvictionIntegrationTest extends CacheRedisTestSupport {

    private static final String REGION = "UserBO";

    private EgonColaCacheProperties properties;
    private RedissonClient client;
    private EgonColaTwoLevelCacheManager manager;
    private EgonColaTwoLevelCachePort port;
    private List<String> eventBodies;
    private int rawListenerId;

    private static Map<String, Object> payload() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", 7L);
        value.put("name", "tom");
        return value;
    }

    /**
     * 手工事务同步器驱动（等价 TransactionTemplate 提交/回滚语义，零新依赖）。
     */
    private static void inTransaction(Runnable body, boolean commit) {
        TransactionSynchronizationManager.initSynchronization();
        try {
            body.run();
            if (commit) {
                TransactionSynchronizationManager.getSynchronizations()
                        .forEach(TransactionSynchronization::afterCommit);
            } else {
                TransactionSynchronizationManager.getSynchronizations()
                        .forEach(synchronization ->
                                synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private static void withTenant(long tenantId, Runnable body) {
        MDC.put("tenantId", String.valueOf(tenantId));
        try {
            body.run();
        } finally {
            MDC.remove("tenantId");
        }
    }

    private static void assertEventually(Runnable assertion) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        Throwable last = null;
        while (true) {
            try {
                assertion.run();
                return;
            } catch (Throwable ex) {
                last = ex;
                if (System.nanoTime() >= deadline) {
                    break;
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(50);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        if (last instanceof AssertionError error) {
            throw error;
        }
        throw new AssertionError("assertion did not settle within 2s", last);
    }

    private RMapCache<String, Object> l2() {
        return client.getMapCache(properties.getKeyPrefix() + ":" + REGION,
                EgonColaCacheCodecs.mapValueCodec());
    }

    private ListAppender<ILoggingEvent> attachPortAppender() {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        ((Logger) LoggerFactory.getLogger(EgonColaTwoLevelCachePort.class)).addAppender(appender);
        return appender;
    }

    private void detachPortAppender(ListAppender<ILoggingEvent> appender) {
        ((Logger) LoggerFactory.getLogger(EgonColaTwoLevelCachePort.class)).detachAppender(appender);
    }

    /**
     * 真实链路惰性装配：Docker 缺失时仅依赖容器的用例 assumption 跳过，
     * 守卫/降级用例以 mock 端口在本地照常执行。
     */
    private void realWiring() {
        if (manager != null) {
            return;
        }
        properties = props(Map.of("second-evict-delay", "PT0.1S"));
        client = newClient();
        manager = spy(new EgonColaTwoLevelCacheManager(properties, client));
        port = new EgonColaTwoLevelCachePort(manager, properties);
        eventBodies = new CopyOnWriteArrayList<>();
        RTopic rawTopic = client.getTopic(properties.getRedis().getTopic(), StringCodec.INSTANCE);
        rawListenerId = rawTopic.addListener(String.class,
                (channel, message) -> eventBodies.add(message));
    }

    @AfterEach
    void unwire() {
        if (client != null) {
            client.getTopic(properties.getRedis().getTopic(), StringCodec.INSTANCE)
                    .removeListener(rawListenerId);
        }
    }

    @Test
    void commitTriggersOneEvictOneEventAndOneDelayedReplayWithoutRePublish() {
        realWiring();
        manager.getCache(REGION);
        Map<String, Object> value = payload();
        l2().fastPut("41:7", value);

        inTransaction(() -> {
            withTenant(41, () ->
                    port.registerEvictionAfterCommit(REGION, List.of("41:7"), List.of()));
            // 提交前：对 Redis 与失效动作零交互
            verify(manager, never()).publish(any(), any(), any());
            verify(manager, never()).applyLocalEviction(any(), any());
            assertThat(eventBodies).isEmpty();
            assertThat(l2().get("41:7")).isNotNull();
        }, true);

        verify(manager, times(1)).publish(REGION, EVICT, List.of("41:7"));
        verify(manager, times(1)).applyLocalEviction(eq(REGION), argThat(keys -> keys.equals(List.of("41:7"))));
        assertThat(l2().get("41:7")).isNull();
        assertEventually(() -> assertThat(eventBodies).hasSize(1));
        assertThat(eventBodies.get(0)).contains("\"operation\":\"EVICT\"").contains("41:7");

        // 第二拍：延迟到点再本地双级失效一次，且不再发布事件
        assertEventually(() ->
                verify(manager, times(2)).applyLocalEviction(eq(REGION), any()));
        try {
            TimeUnit.MILLISECONDS.sleep(300);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        verify(manager, times(2)).applyLocalEviction(eq(REGION), any());
        verify(manager, times(1)).publish(eq(REGION), any(), any());
        assertThat(eventBodies).hasSize(1);
    }

    @Test
    void rollbackProducesZeroInteractionsAndDiscardsBufferedKeys() {
        realWiring();
        l2().fastPut("41:7", payload());

        inTransaction(() -> withTenant(41, () ->
                port.registerEvictionAfterCommit(REGION, List.of("41:7"), List.of())), false);

        verify(manager, never()).publish(any(), any(), any());
        verify(manager, never()).applyLocalEviction(any(), any());
        assertThat(l2().get("41:7")).isNotNull();
        assertThat(eventBodies).isEmpty();

        // 后续登记不再夹带被回滚丢弃的键（缓冲确已清空）
        withTenant(41, () -> port.registerEvictionAfterCommit(REGION, List.of("41:8"), List.of()));
        verify(manager, times(1)).applyLocalEviction(REGION, List.of("41:8"));
        verify(manager, never()).applyLocalEviction(eq(REGION), argThat(keys -> keys.contains("41:7")));
        verify(manager, times(1)).publish(REGION, EVICT, List.of("41:8"));
    }

    @Test
    void nonTransactionalRegistrationFlushesImmediatelyWithWarn() {
        realWiring();
        ListAppender<ILoggingEvent> appender = attachPortAppender();
        try {
            withTenant(41, () -> port.registerEvictionAfterCommit(REGION, List.of("41:7"), List.of()));
        } finally {
            detachPortAppender(appender);
        }

        verify(manager, times(1)).applyLocalEviction(REGION, List.of("41:7"));
        verify(manager, times(1)).publish(REGION, EVICT, List.of("41:7"));
        assertThat(appender.list).anySatisfy(event -> assertThat(event.getFormattedMessage())
                .contains("no transaction context"));
    }

    @Test
    void registrationGuardsRunBeforeAnyCacheInteraction() {
        EgonColaTwoLevelCacheManager guardManager = mock(EgonColaTwoLevelCacheManager.class);
        EgonColaTwoLevelCachePort guarded =
                new EgonColaTwoLevelCachePort(guardManager, props(Map.of()));

        assertThatThrownBy(() -> withTenant(41, () ->
                guarded.registerEvictionAfterCommit(REGION, List.of("42:7"), List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CACHE_KEY_TENANT_MISMATCH");
        for (String badGlob : List.of("*", "41:*:", "41:7:*")) {
            assertThatThrownBy(() -> withTenant(41, () ->
                    guarded.registerEvictionAfterCommit(REGION, List.of(), List.of(badGlob))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CACHE_GLOB_PATTERN_FORBIDDEN");
        }
        assertThatThrownBy(() -> withTenant(41, () ->
                guarded.registerEvictionAfterCommit(REGION, List.of(), List.of("42:*"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CACHE_KEY_TENANT_MISMATCH");
        // MDC 缺失 → fail-closed（PC-005）
        assertThatThrownBy(() ->
                guarded.registerEvictionAfterCommit(REGION, List.of("41:7"), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CACHE_KEY_TENANT_MISMATCH");
        assertThatThrownBy(() -> withTenant(41, () ->
                guarded.registerEvictionAfterCommit("bad name", List.of("41:7"), List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CACHE_NAME_INVALID");

        verifyNoInteractions(guardManager);
    }

    @Test
    void getGuardsLoaderTenantAndRegionBeforeDelegation() {
        EgonColaTwoLevelCacheManager guardManager = mock(EgonColaTwoLevelCacheManager.class);
        EgonColaTwoLevelCachePort guarded =
                new EgonColaTwoLevelCachePort(guardManager, props(Map.of("batch.max-keys", "2")));

        assertThatThrownBy(() -> withTenant(41, () -> guarded.get(REGION, "41:7", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CACHE_LOADER_REQUIRED");
        assertThatThrownBy(() -> withTenant(41, () ->
                guarded.getAll(REGION, List.of("41:1", "41:2", "41:3"), key -> "v")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CACHE_KEY_SIZE_EXCEEDED");
        assertThatThrownBy(() -> guarded.get(REGION, "41:7", () -> "v"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CACHE_KEY_TENANT_MISMATCH");

        verifyNoInteractions(guardManager);
    }

    @Test
    void brokenInfrastructureDegradesToAuthoritativeLoaderValues() {
        RedissonClient broken = mock(RedissonClient.class);
        EgonColaTwoLevelCacheManager brokenManager =
                new EgonColaTwoLevelCacheManager(props(Map.of()), broken);
        EgonColaTwoLevelCachePort degraded =
                new EgonColaTwoLevelCachePort(brokenManager, props(Map.of()));

        withTenant(41, () -> {
            assertThat(degraded.get(REGION, "41:7", () -> "auth")).isEqualTo("auth");
            assertThat(degraded.getAll(REGION, List.of("41:7", "41:8"), key -> "a-" + key))
                    .containsExactly("a-41:7", "a-41:8");
        });
    }

    @Test
    void getLoadsOnceOnMissThenServesFromBothLevelsAndCachesNullSentinel() {
        realWiring();
        AtomicInteger loads = new AtomicInteger();

        withTenant(41, () -> {
            Object first = port.get(REGION, "41:7", () -> {
                loads.incrementAndGet();
                return payload();
            });
            assertThat(first).isEqualTo(payload());

            Object second = port.get(REGION, "41:7", () -> {
                loads.incrementAndGet();
                return payload();
            });
            assertThat(second).isEqualTo(payload());

            assertThat(port.get(REGION, "41:8", () -> {
                loads.incrementAndGet();
                return null;
            })).isNull();
            assertThat(port.get(REGION, "41:8", () -> {
                loads.incrementAndGet();
                return payload();
            })).isNull();
        });

        assertThat(loads.get()).isEqualTo(2);
        assertThat(l2().get("41:7")).isEqualTo(payload());
        assertEventually(() -> assertThat(eventBodies).hasSize(2));
    }

    @Test
    void getAllAssemblesSameOrderValuesWithNullsForMissing() {
        realWiring();
        List<String> keys = List.of("41:7", "41:8", "41:9");
        AtomicInteger loaderCalls = new AtomicInteger();

        withTenant(41, () -> {
            List<Object> first = port.getAll(REGION, keys, key -> {
                loaderCalls.incrementAndGet();
                return key.endsWith(":8") ? null : payload();
            });
            assertThat(first).hasSize(3);
            assertThat(first.get(0)).isEqualTo(payload());
            assertThat(first.get(1)).isNull();
            assertThat(first.get(2)).isEqualTo(payload());
        });

        assertThat(loaderCalls.get()).isEqualTo(3);
        assertEventually(() -> assertThat(eventBodies).hasSize(3));
    }

    @Test
    void shutdownSchedulerSwallowsSecondShotAndKeepsPrimaryEviction() {
        realWiring();
        ListAppender<ILoggingEvent> appender = attachPortAppender();
        try {
            manager.scheduler().shutdown();
            inTransaction(() -> withTenant(41, () ->
                    port.registerEvictionAfterCommit(REGION, List.of("41:7"), List.of())), true);

            verify(manager, times(1)).applyLocalEviction(REGION, List.of("41:7"));
            verify(manager, times(1)).publish(REGION, EVICT, List.of("41:7"));
            assertThat(appender.list).anySatisfy(event ->
                    assertThat(event.getFormattedMessage()).contains("CACHE_L2_OPERATION_FAILED"));
        } finally {
            detachPortAppender(appender);
        }
    }

    @Test
    void flushFailuresAreSwallowedInAfterCommitWithWarnOnly() {
        realWiring();
        ListAppender<ILoggingEvent> appender = attachPortAppender();
        try {
            doThrow(new RuntimeException("redis down")).when(manager)
                    .applyLocalPrefixEviction(eq(REGION), any());

            assertThatCode(() -> inTransaction(() -> withTenant(42, () ->
                    port.registerEvictionAfterCommit(REGION, List.of(), List.of("42:*"))), true))
                    .doesNotThrowAnyException();

            verify(manager, never()).publish(any(), any(), any());
            assertThat(appender.list).anySatisfy(event -> assertThat(event.getFormattedMessage())
                    .contains("CACHE_L2_OPERATION_FAILED"));
            assertThat(appender.list).allSatisfy(event ->
                    assertThat(event.getLevel().levelInt).isLessThan(Level.ERROR.levelInt));
        } finally {
            detachPortAppender(appender);
        }
    }
}
