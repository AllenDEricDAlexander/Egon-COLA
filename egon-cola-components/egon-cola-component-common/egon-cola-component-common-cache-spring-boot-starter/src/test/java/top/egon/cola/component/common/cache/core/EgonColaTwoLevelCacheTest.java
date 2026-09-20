package top.egon.cola.component.common.cache.core;

import com.google.common.cache.Cache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.slf4j.MDC;
import org.redisson.api.RedissonClient;
import top.egon.cola.component.common.cache.model.EgonColaCacheNullValueBO;
import top.egon.cola.component.common.cache.support.CacheRedisTestSupport;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class EgonColaTwoLevelCacheTest extends CacheRedisTestSupport {

    @BeforeEach
    void tenantContext() {
        MDC.put("tenantId", "41");
    }

    @AfterEach
    void clearTenantContext() {
        MDC.clear();
        if (manager != null) {
            manager.destroy();
        }
    }

    private static final String REGION = "UserBO";

    private EgonColaTwoLevelCacheManager manager;
    private EgonColaTwoLevelCache cache;

    /**
     * 懒建夹具：Redis 触达点在测试方法体内首个调用才发生，Docker 缺失时仅相应用例 assumption 跳过，
     * 抖动/构造段照常执行。
     */
    private EgonColaTwoLevelCacheManager manager() {
        cache();
        return manager;
    }

    private EgonColaTwoLevelCache cache() {
        if (cache == null) {
            manager = new EgonColaTwoLevelCacheManager(props(Map.of()), newClient());
            cache = (EgonColaTwoLevelCache) manager.getCache(REGION);
        }
        return cache;
    }

    private static Map<String, Object> payload() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", 7L);
        value.put("name", "tom");
        return value;
    }

    private Cache<String, ?> l1Handle(EgonColaTwoLevelCache target) {
        return (Cache<String, ?>) target.getNativeCache();
    }

    @Test
    void putWritesBothLevels() {
        Map<String, Object> value = payload();

        cache().put("41:7", value);

        assertThat(cache().get("41:7").get()).isEqualTo(value);
        assertThat(l1Handle(cache()).getIfPresent("41:7")).isNotNull();
        assertThat(manager().l2(REGION).get("41:7")).isEqualTo(value);
    }

    @Test
    void l2HitBackfillsL1WithoutLoader() {
        Map<String, Object> value = payload();
        cache().put("41:7", value);
        manager().applyRemotePut(REGION, List.of("41:7"));
        assertThat(l1Handle(cache()).getIfPresent("41:7")).isNull();

        AtomicInteger loads = new AtomicInteger();
        Object loaded = cache().get("41:7", () -> {
            loads.incrementAndGet();
            return null;
        });

        assertThat(loaded).isEqualTo(value);
        assertThat(loads).hasValue(0);
        assertThat(l1Handle(cache()).getIfPresent("41:7")).isNotNull();
    }

    @Test
    void doubleMissLoadsOnceAndWritesBothLevels() {
        Map<String, Object> value = payload();
        AtomicInteger loads = new AtomicInteger();

        Object loaded = cache().get("41:7", () -> {
            loads.incrementAndGet();
            return value;
        });

        assertThat(loaded).isEqualTo(value);
        assertThat(loads).hasValue(1);
        assertThat(l1Handle(cache()).getIfPresent("41:7")).isNotNull();
        assertThat(manager().l2(REGION).get("41:7")).isEqualTo(value);
    }

    @Test
    void nullSentinelShortCircuitsSecondLoad() {
        AtomicInteger loads = new AtomicInteger();

        Object first = cache().get("41:8", () -> {
            loads.incrementAndGet();
            return null;
        });
        assertThat(first).isNull();
        assertThat(loads).hasValue(1);
        Object second = cache().get("41:8", () -> {
            loads.incrementAndGet();
            return null;
        });
        assertThat(second).isNull();
        assertThat(loads).hasValue(1);
        assertThat(manager().l2(REGION).get("41:8")).isEqualTo(EgonColaCacheNullValueBO.INSTANCE);
    }

    @Test
    void eightThreadsSameKeyLoadExactlyOnce() throws Exception {
        EgonColaTwoLevelCache target = cache();
        Map<String, Object> value = payload();
        AtomicInteger loads = new AtomicInteger();
        CountDownLatch gate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                futures.add(pool.submit(() -> {
                    gate.await();
                    MDC.put("tenantId", "41");
                    try {
                        return target.get("41:10", () -> {
                            loads.incrementAndGet();
                            Thread.sleep(50);
                            return value;
                        });
                    } finally {
                        MDC.clear();
                    }
                }));
            }
            gate.countDown();

            for (Future<Object> future : futures) {
                assertThat(future.get(15, TimeUnit.SECONDS)).isEqualTo(value);
            }
            assertThat(loads).hasValue(1);
            assertThat(manager().l2(REGION).get("41:10")).isEqualTo(value);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void lockHeldByOthersDegradesToAuthoritativeLoadWithoutCaching() {
        RedissonClient secondClient = newClient();
        EgonColaTwoLevelCacheManager degradedManager =
                new EgonColaTwoLevelCacheManager(props(Map.of("lock.wait-time", "PT0.2S")), newClient());
        EgonColaTwoLevelCache degraded = (EgonColaTwoLevelCache) degradedManager.getCache(REGION);
        EgonColaTwoLevelCacheManager holderManager =
                new EgonColaTwoLevelCacheManager(props(Map.of()), secondClient);
        holderManager.lock(REGION, "41:9").lock(5, TimeUnit.SECONDS);
        try {
            Map<String, Object> value = payload();
            AtomicInteger loads = new AtomicInteger();

            Object loaded = degraded.get("41:9", () -> {
                loads.incrementAndGet();
                return value;
            });

            assertThat(loaded).isEqualTo(value);
            assertThat(loads).hasValue(1);
            assertThat(l1Handle(degraded).getIfPresent("41:9")).isNull();
            assertThat(degradedManager.l2(REGION).get("41:9")).isNull();
        } finally {
            holderManager.lock(REGION, "41:9").unlock();
        }
    }

    @Test
    void backfilledL1DeadlineIsCappedByRemainingL2Lifetime() throws Exception {
        EgonColaTwoLevelCache target = cache();
        Map<String, Object> value = payload();
        manager().l2(REGION).put("41:12", value, 2, TimeUnit.SECONDS);

        AtomicInteger loads = new AtomicInteger();
        Object backfilled = target.get("41:12", () -> {
            loads.incrementAndGet();
            return null;
        });
        assertThat(backfilled).isEqualTo(value);
        assertThat(loads).hasValue(0);
        assertThat(l1Handle(target).getIfPresent("41:12")).isNotNull();

        TimeUnit.MILLISECONDS.sleep(2200);

        Object reloaded = target.get("41:12", () -> {
            loads.incrementAndGet();
            return value;
        });
        assertThat(reloaded).isEqualTo(value);
        assertThat(loads).hasValue(1);
    }

    @Test
    void jitterStaysWithinBoundedRangeForSeededRandom() {
        long base = TimeUnit.SECONDS.toMillis(30);
        long jitter = TimeUnit.SECONDS.toMillis(6);
        Random seeded = new Random(42);
        Set<Long> samples = new HashSet<>();

        for (int i = 0; i < 100; i++) {
            long jittered = EgonColaTwoLevelCache.Jitter.jitteredMillis(base, jitter, seeded);
            assertThat(jittered).isBetween(base, base + jitter);
            samples.add(jittered);
        }

        assertThat(samples).hasSizeGreaterThan(1);
    }

    @Test
    void zeroJitterKeepsBaseExact() {
        long base = TimeUnit.MINUTES.toMillis(5);

        assertThat(EgonColaTwoLevelCache.Jitter.jitteredMillis(base, 0L, new Random(7))).isEqualTo(base);
    }
}
