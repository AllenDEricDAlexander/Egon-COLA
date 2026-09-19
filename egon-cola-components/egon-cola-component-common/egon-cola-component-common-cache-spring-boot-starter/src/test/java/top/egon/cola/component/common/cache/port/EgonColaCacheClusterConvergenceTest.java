package top.egon.cola.component.common.cache.port;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.cache.Cache;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import top.egon.cola.component.common.cache.autoconfigure.EgonColaCacheProperties;
import top.egon.cola.component.common.cache.codec.EgonColaCacheCodecs;
import top.egon.cola.component.common.cache.core.EgonColaTwoLevelCacheManager;
import top.egon.cola.component.common.cache.event.EgonColaCacheChangedListener;
import top.egon.cola.component.common.cache.support.CacheRedisTestSupport;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static top.egon.cola.component.common.cache.event.EgonColaCacheChangedOperation.EVICT;
import static top.egon.cola.component.common.cache.event.EgonColaCacheChangedOperation.PREFIX_EVICT;

class EgonColaCacheClusterConvergenceTest extends CacheRedisTestSupport {

    private static final String REGION = "UserBO";

    private RedissonClient clientA;
    private RedissonClient clientB;
    private EgonColaCacheProperties propertiesA;
    private EgonColaCacheProperties propertiesB;
    private EgonColaTwoLevelCacheManager managerA;
    private EgonColaTwoLevelCacheManager managerB;
    private EgonColaCacheChangedListener listenerA;
    private EgonColaCacheChangedListener listenerB;
    private EgonColaTwoLevelCachePort portA;

    private static Map<String, Object> payload(long id) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", id);
        value.put("name", "tom-" + id);
        return value;
    }

    private static void withTenant(long tenantId, Runnable body) {
        org.slf4j.MDC.put("tenantId", String.valueOf(tenantId));
        try {
            body.run();
        } finally {
            org.slf4j.MDC.remove("tenantId");
        }
    }

    /** A 侧"事务提交"：登记后手工驱动同步器，触发 afterCommit 真实失效与发布。 */
    private void commitEvictionOnA(long tenantId, List<String> exactKeys, List<String> globs) {
        TransactionSynchronizationManager.initSynchronization();
        try {
            withTenant(tenantId, () ->
                    portA.registerEvictionAfterCommit(REGION, exactKeys, globs));
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
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
        throw new AssertionError("cluster state did not converge within 2s", last);
    }

    private RMapCache<String, Object> l2(RedissonClient client, EgonColaCacheProperties properties) {
        return client.getMapCache(properties.getKeyPrefix() + ":" + REGION,
                EgonColaCacheCodecs.mapValueCodec());
    }

    private boolean hasL1(EgonColaTwoLevelCacheManager manager, String key) {
        Cache handle = manager.getCache(REGION);
        com.google.common.cache.Cache<?, ?> l1 = (com.google.common.cache.Cache<?, ?>) handle.getNativeCache();
        return l1.getIfPresent(key) != null;
    }

    @BeforeEach
    void wireTwoNodes() {
        clientA = newClient();
        clientB = newClient();
        propertiesA = props(Map.of("second-evict-delay", "PT0.1S",
                "node-id", "node-a", "ttl.expire", "PT10S"));
        propertiesB = props(Map.of("second-evict-delay", "PT0.1S",
                "node-id", "node-b", "ttl.expire", "PT10S"));
        managerA = new EgonColaTwoLevelCacheManager(propertiesA, clientA);
        managerB = new EgonColaTwoLevelCacheManager(propertiesB, clientB);
        portA = new EgonColaTwoLevelCachePort(managerA, propertiesA);
        listenerA = new EgonColaCacheChangedListener(managerA, propertiesA);
        listenerB = new EgonColaCacheChangedListener(managerB, propertiesB);
        listenerA.start();
        listenerB.start();
    }

    @AfterEach
    void stopListeners() {
        // @BeforeEach 因 Docker 缺失 assumption 中断时字段仍为 null
        if (listenerA != null) {
            listenerA.stop();
        }
        if (listenerB != null) {
            listenerB.stop();
        }
    }

    @Test
    void remoteEvictClearsBothLevelsOnPeer() {
        Cache handleA = managerA.getCache(REGION);
        Map<String, Object> value = payload(7);
        handleA.put("42:7", value);

        AtomicBoolean loaderCalledOnB = new AtomicBoolean();
        assertThat(managerB.getCache(REGION).get("42:7",
                () -> loaderCalledOnB.compareAndSet(false, true))).isEqualTo(value);
        assertThat(hasL1(managerB, "42:7")).isTrue();

        commitEvictionOnA(42, List.of("42:7"), List.of());

        RMapCache<String, Object> sharedL2 = l2(clientB, propertiesB);
        assertEventually(() -> {
            assertThat(sharedL2.get("42:7")).isNull();
            assertThat(hasL1(managerB, "42:7")).isFalse();
            assertThat(hasL1(managerA, "42:7")).isFalse();
        });
        assertThat(loaderCalledOnB).isFalse();
    }

    @Test
    void remotePutDropsPeerL1AndKeepsL2Latest() {
        Cache handleB = managerB.getCache(REGION);
        handleB.put("42:9", payload(9));
        assertThat(hasL1(managerB, "42:9")).isTrue();

        Map<String, Object> authoritative = payload(90);
        managerA.getCache(REGION).put("42:9", authoritative);

        RMapCache<String, Object> sharedL2 = l2(clientB, propertiesB);
        assertEventually(() -> assertThat(hasL1(managerB, "42:9")).isFalse());
        assertThat(sharedL2.get("42:9")).isEqualTo(authoritative);
        // 对端 L2 保留：B 再读命中 L2，不再回源（DEC-007）
        AtomicBoolean loaderCalled = new AtomicBoolean();
        assertThat(handleB.get("42:9", () -> {
            loaderCalled.set(true);
            return "STALE-DB";
        })).isEqualTo(authoritative);
        assertThat(loaderCalled).isFalse();
    }

    @Test
    void prefixEvictRespectsTenantBoundary() {
        RMapCache<String, Object> sharedL2 = l2(clientA, propertiesA);
        for (String key : List.of("42:7", "42:8", "42:9", "43:7", "43:8", "43:9")) {
            sharedL2.put(key, payload(Long.parseLong(key.substring(3))), 30, TimeUnit.SECONDS);
        }
        Cache handleB = managerB.getCache(REGION);
        handleB.get("42:7", () -> "loader-must-not-run");
        handleB.get("43:7", () -> "loader-must-not-run");
        assertThat(hasL1(managerB, "42:7")).isTrue();
        assertThat(hasL1(managerB, "43:7")).isTrue();

        commitEvictionOnA(42, List.of(), List.of("42:*"));

        assertEventually(() -> {
            assertThat(sharedL2.readAllKeySet()).doesNotContain("42:7", "42:8", "42:9")
                    .contains("43:7", "43:8", "43:9");
            assertThat(hasL1(managerB, "42:7")).isFalse();
        });
        assertThat(hasL1(managerB, "43:7")).isTrue();
    }

    @Test
    void duplicateAndOutOfOrderEventsConvergeIdempotently() {
        Cache handleA = managerA.getCache(REGION);
        handleA.put("42:7", payload(7));
        Cache handleB = managerB.getCache(REGION);
        handleB.get("42:7", () -> "loader-must-not-run");

        commitEvictionOnA(42, List.of("42:7"), List.of());
        RMapCache<String, Object> sharedL2 = l2(clientB, propertiesB);
        assertEventually(() -> {
            assertThat(sharedL2.get("42:7")).isNull();
            assertThat(hasL1(managerB, "42:7")).isFalse();
        });

        // 重复与乱序投递（含晚到的前缀事件）只增幂等删除，不复活、不报错
        managerA.publish(REGION, EVICT, List.of("42:7"));
        managerA.publish(REGION, EVICT, List.of("42:7"));
        managerA.publish(REGION, PREFIX_EVICT, List.of("42:*"));
        try {
            TimeUnit.MILLISECONDS.sleep(400);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        assertThat(sharedL2.get("42:7")).isNull();
        assertThat(hasL1(managerB, "42:7")).isFalse();
        assertThat(handleB.get("42:7", () -> payload(71))).isEqualTo(payload(71));
    }
}
