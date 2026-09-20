package top.egon.cola.component.common.cache.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RMapCache;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.slf4j.MDC;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.cache.autoconfigure.EgonColaCacheProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 原生 Spring 代理 + mock Redis；不启动网络服务或容器。
 */
class EgonColaCacheAnnotationTest {

    private AnnotationConfigApplicationContext context;
    private EgonColaTwoLevelCacheManager manager;
    private EgonColaCacheProperties properties;
    private ExampleRepository repository;
    private final AtomicInteger calls = new AtomicInteger();
    private final Map<String, Map<String, Object>> data = new ConcurrentHashMap<>();
    private final Map<String, Long> ttls = new ConcurrentHashMap<>();
    private RedissonClient redis;
    private RLock distributedLock;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        MDC.put("tenantId", "41");
        properties = new EgonColaCacheProperties();
        properties.getTtl().setL1Jitter(Duration.ZERO);
        properties.getTtl().setL2Jitter(Duration.ZERO);
        redis = mock(RedissonClient.class);
        distributedLock = mock(RLock.class);
        ReentrantLock lock = new ReentrantLock();
        when(redis.getLock(anyString())).thenReturn(distributedLock);
        when(distributedLock.tryLock(anyLong(), anyLong(), any())).thenAnswer(invocation -> {
            lock.lock();
            return true;
        });
        when(distributedLock.isHeldByCurrentThread()).thenAnswer(invocation -> lock.isHeldByCurrentThread());
        org.mockito.Mockito.doAnswer(invocation -> {
            lock.unlock();
            return null;
        }).when(distributedLock).unlock();
        when(redis.getTopic(anyString(), any(Codec.class))).thenReturn(mock(RTopic.class));
        when(redis.getMapCache(anyString(), any(Codec.class))).thenAnswer(invocation -> {
            String region = invocation.getArgument(0);
            Map<String, Object> values = data.computeIfAbsent(region, ignored -> new ConcurrentHashMap<>());
            RMapCache<String, Object> map = mock(RMapCache.class);
            when(map.get(anyString())).thenAnswer(call -> values.get(call.getArgument(0)));
            when(map.remainTimeToLive(anyString())).thenAnswer(call -> values.containsKey(call.getArgument(0))
                    ? ttls.getOrDefault(region + ":" + call.getArgument(0), 60000L) : -2L);
            when(map.fastPut(anyString(), any(), anyLong(), any())).thenAnswer(call -> {
                values.put(call.getArgument(0), call.getArgument(1));
                ttls.put(region + ":" + call.getArgument(0), call.getArgument(2));
                return true;
            });
            when(map.readAllKeySet()).thenAnswer(call -> new HashSet<>(values.keySet()));
            when(map.fastRemove(any(String[].class))).thenAnswer(call -> {
                long removed = 0;
                for (Object key : call.getArguments()) {
                    if (values.remove(key) != null) {
                        removed++;
                    }
                }
                return removed;
            });
            return map;
        });
        manager = new EgonColaTwoLevelCacheManager(properties, redis);
        context = new AnnotationConfigApplicationContext();
        context.register(CacheConfiguration.class);
        context.registerBean("cacheManager", EgonColaTwoLevelCacheManager.class, () -> manager);
        context.registerBean(ExampleRepository.class, () -> new ExampleRepository(calls));
        context.refresh();
        repository = context.getBean(ExampleRepository.class);
    }

    @AfterEach
    void close() {
        context.close();
        MDC.clear();
    }

    @Test
    void cacheConfigSpelConditionAndUnlessUseNativeSpringSemantics() {
        assertThat(repository.find("alice", true)).isEqualTo("alice");
        repository.find("alice", true);
        assertThat(calls).hasValue(1);
        repository.find("alice", false);
        assertThat(calls).hasValue(2);
        repository.find("missing", true);
        repository.find("missing", true);
        assertThat(calls).hasValue(4);
        assertThat(manager.getCache("users").get("41:user:missing")).isNull();
    }

    @Test
    void cachePutAndCachingUpdateAndInvalidateSeparateRegions() {
        repository.find("alice", true);
        manager.getCache("search").put("41:query:all", "old");
        assertThat(repository.update("alice", "new")).isEqualTo("new");
        assertThat(repository.find("alice", true)).isEqualTo("new");
        assertThat(manager.getCache("search").get("41:query:all")).isNull();
        assertThat(calls).hasValue(2);
        repository.delete("alice");
        assertThat(repository.find("alice", true)).isEqualTo("alice");
    }

    @Test
    void allEntriesIsTenantScopedAndPublishesPrefixInvalidation() {
        Cache cache = manager.getCache("search");
        cache.put("41:query:all", "a");
        MDC.put("tenantId", "42");
        cache.put("42:query:all", "b");
        MDC.put("tenantId", "41");
        repository.update("alice", "new");
        assertThat(cache.get("41:query:all")).isNull();
        MDC.put("tenantId", "42");
        assertThat(cache.get("42:query:all").get()).isEqualTo("b");
        org.mockito.Mockito.verify(manager.topic(), org.mockito.Mockito.atLeastOnce())
                .publish(org.mockito.ArgumentMatchers.contains("PREFIX_EVICT"));
    }

    @Test
    void syncCachesNullAndRejectsUnlessCombination() {
        assertThat(repository.sync("missing")).isNull();
        assertThat(repository.sync("missing")).isNull();
        assertThat(calls).hasValue(1);
        assertThat(ttls.get(properties.getKeyPrefix() + ":users:41:user:missing")).isEqualTo(60000);
        assertThatThrownBy(() -> repository.invalidSync("a"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("unless");
    }

    @Test
    void syncCoalescesConcurrentLoads() throws Exception {
        var pool = Executors.newFixedThreadPool(8);
        CountDownLatch gate = new CountDownLatch(1);
        try {
            List<Future<String>> results = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                results.add(pool.submit(() -> {
                    MDC.put("tenantId", "41");
                    try {
                        gate.await();
                        return repository.sync("alice");
                    } finally {
                        MDC.clear();
                    }
                }));
            }
            gate.countDown();
            for (Future<String> result : results) {
                assertThat(result.get(5, TimeUnit.SECONDS)).isEqualTo("alice");
            }
            assertThat(calls).hasValue(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void mutationsWaitForCommitAndRollbackDiscardsThem() {
        repository.find("alice", true);
        TransactionTemplate transaction = new TransactionTemplate(new LocalTransactionManager());
        transaction.executeWithoutResult(status -> {
            repository.update("alice", "rollback");
            status.setRollbackOnly();
        });
        assertThat(repository.find("alice", true)).isEqualTo("alice");
        transaction.executeWithoutResult(status -> repository.update("alice", "committed"));
        assertThat(repository.find("alice", true)).isEqualTo("committed");
        transaction.executeWithoutResult(status -> {
            repository.delete("alice");
            status.setRollbackOnly();
        });
        assertThat(repository.find("alice", true)).isEqualTo("committed");
        transaction.executeWithoutResult(status -> repository.delete("alice"));
        assertThat(manager.getCache("users").get("41:user:alice")).isNull();
    }

    @Test
    void transactionReadsBypassCacheAndNeverPublishRolledBackLoads() {
        repository.update("alice", "cached");
        TransactionTemplate transaction = new TransactionTemplate(new LocalTransactionManager());
        transaction.executeWithoutResult(status -> {
            assertThat(repository.sync("alice")).isEqualTo("alice");
            assertThat(repository.sync("missing")).isNull();
            status.setRollbackOnly();
        });
        assertThat(repository.find("alice", true)).isEqualTo("cached");
        assertThat(manager.getCache("users").get("41:user:missing")).isNull();
    }

    @Test
    void beforeInvocationEvictsEvenWhenMethodFailsAndTransactionRollsBack() {
        repository.find("alice", true);
        TransactionTemplate transaction = new TransactionTemplate(new LocalTransactionManager());
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> repository.deleteBefore("alice")))
                .isInstanceOf(IllegalStateException.class);
        assertThat(manager.getCache("users").get("41:user:alice")).isNull();
    }

    @Test
    void selfInvocationBypassesProxyAndProgrammaticCacheRemainsAvailable() {
        repository.selfFind("alice");
        repository.selfFind("alice");
        assertThat(calls).hasValue(2);
        assertThat(manager.getCache("users").get("41:user:alice")).isNull();
        assertThat(manager.getCache("users").get("41:user:alice", () -> "manual")).isEqualTo("manual");
        assertThat(repository.find("alice", true)).isEqualTo("manual");
    }

    @Test
    void rejectsMissingTenantAndCrossTenantBeforeCacheHits() {
        repository.find("alice", true);
        MDC.remove("tenantId");
        assertThatThrownBy(() -> manager.getCache("users").get("41:user:alice"))
                .hasMessageContaining("CACHE_KEY_TENANT_MISMATCH");
        MDC.put("tenantId", "42");
        assertThatThrownBy(() -> manager.getCache("users").get("41:user:alice"))
                .hasMessageContaining("CACHE_KEY_TENANT_MISMATCH");
        assertThatThrownBy(() -> manager.getCache("users").put("42:*", "x"))
                .hasMessageContaining("CACHE_GLOB_PATTERN_FORBIDDEN");
    }

    @Test
    void regionTtlOverridesGlobalDefaultsAndAddsBoundedJitter() {
        EgonColaCacheProperties.RegionTtl override = new EgonColaCacheProperties.RegionTtl();
        override.setL1Expire(Duration.ofSeconds(4));
        override.setL1Jitter(Duration.ofSeconds(2));
        override.setL2Expire(Duration.ofSeconds(10));
        override.setL2Jitter(Duration.ofSeconds(2));
        properties.getRegions().put("users", override);
        repository.find("alice", true);
        repository.sync("missing");
        // 两级独立采样：较短的 L1 区间不改变 L2 写入寿命
        assertThat(ttls.get(properties.getKeyPrefix() + ":users:41:user:alice")).isBetween(10000L, 12000L);
        // 空值哨兵使用 nullExpire 且不加抖动
        assertThat(ttls.get(properties.getKeyPrefix() + ":users:41:user:missing")).isEqualTo(60000L);
        manager.getCache("search").put("41:query:all", "value");
        assertThat(ttls.get(properties.getKeyPrefix() + ":search:41:query:all")).isEqualTo(3600000L);
    }

    @Test
    void lockHandleAndReleaseFailuresDoNotOverrideLoaderResult() {
        doThrow(new IllegalStateException("redis down")).when(redis).getLock(anyString());
        assertThat(repository.sync("alice")).isEqualTo("alice");
        org.mockito.Mockito.doReturn(distributedLock).when(redis).getLock(anyString());
        doThrow(new IllegalStateException("redis down")).when(distributedLock).unlock();
        assertThatCode(() -> repository.sync("bob")).doesNotThrowAnyException();
    }

    @Test
    void lateRemoteInvalidationsKeepNewSharedValues() {
        Cache cache = manager.getCache("users");
        cache.put("41:user:alice", "new");
        manager.applyRemotePut("users", List.of("41:user:alice"));
        assertThat(cache.get("41:user:alice").get()).isEqualTo("new");
        manager.applyRemotePrefixEviction("users", "41:*");
        assertThat(cache.get("41:user:alice").get()).isEqualTo("new");
    }

    @Test
    void l2HitBackfillsL1OnlyWhenRemainingLifetimeIsKnown() {
        Cache users = manager.getCache("users");
        users.put("41:user:carol", "shared");
        RMapCache<String, Object> l2 = manager.l2("users");
        when(l2.remainTimeToLive("41:user:carol")).thenReturn(-1L);
        manager.applyRemotePut("users", List.of("41:user:carol"));

        AtomicInteger loads = new AtomicInteger();
        Object hit = users.get("41:user:carol", () -> {
            loads.incrementAndGet();
            return null;
        });
        assertThat(hit).isEqualTo("shared");
        assertThat(loads).hasValue(0);
        assertThat(l1Peek(users, "41:user:carol")).isNull();

        when(l2.remainTimeToLive("41:user:carol")).thenReturn(60000L);
        Object refilled = users.get("41:user:carol", () -> null);
        assertThat(refilled).isEqualTo("shared");
        assertThat(l1Peek(users, "41:user:carol")).isNotNull();
    }

    private static Object l1Peek(Cache cache, String key) {
        return ((com.google.common.cache.Cache<?, ?>) cache.getNativeCache()).getIfPresent(key);
    }

    @Test
    void l2BackfillNeverRenewsSharedLifetime() {
        Cache users = manager.getCache("users");
        RMapCache<String, Object> l2 = manager.l2("users");
        users.put("41:user:dave", "shared");
        manager.applyRemotePut("users", List.of("41:user:dave"));
        org.mockito.Mockito.clearInvocations(l2);

        Object hit = users.get("41:user:dave", () -> null);

        assertThat(hit).isEqualTo("shared");
        org.mockito.Mockito.verify(l2, org.mockito.Mockito.never())
                .fastPut(anyString(), any(), anyLong(), any());
        org.mockito.Mockito.verify(l2, org.mockito.Mockito.never()).put(anyString(), any());
    }

    @Test
    void commitUsesCapturedTenantAndL2LookupFailureFallsBack() {
        TransactionTemplate transaction = new TransactionTemplate(new LocalTransactionManager());
        transaction.executeWithoutResult(status -> {
            repository.update("alice", "new");
            MDC.put("tenantId", "42");
        });
        MDC.put("tenantId", "41");
        assertThat(repository.find("alice", true)).isEqualTo("new");
        manager.applyRemotePut("users", List.of("41:user:alice"));
        doThrow(new IllegalStateException("redis down")).when(manager.l2("users")).get(anyString());
        assertThat(repository.find("alice", true)).isEqualTo("alice");
    }

    @Test
    void managerOwnsSchedulerLifecycle() {
        manager.destroy();
        assertThat(manager.scheduler().isShutdown()).isTrue();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableCaching(proxyTargetClass = true)
    static class CacheConfiguration {
    }

    @CacheConfig(cacheNames = "users")
    static class ExampleRepository {
        private final AtomicInteger calls;

        ExampleRepository(AtomicInteger calls) {
            this.calls = calls;
        }

        @Cacheable(key = "T(org.slf4j.MDC).get('tenantId') + ':user:' + #p0", condition = "#p1", unless = "#result == null")
        public String find(String name, boolean enabled) {
            return load(name);
        }

        @Cacheable(key = "T(org.slf4j.MDC).get('tenantId') + ':user:' + #p0", sync = true)
        public String sync(String name) {
            return load(name);
        }

        @Cacheable(key = "'41:' + #p0", sync = true, unless = "#result == null")
        public String invalidSync(String name) {
            return load(name);
        }

        @Caching(put = @CachePut(key = "T(org.slf4j.MDC).get('tenantId') + ':user:' + #p0"),
                evict = @CacheEvict(cacheNames = "search", allEntries = true))
        public String update(String name, String value) {
            calls.incrementAndGet();
            return value;
        }

        @CacheEvict(key = "T(org.slf4j.MDC).get('tenantId') + ':user:' + #p0")
        public void delete(String name) {
        }

        @CacheEvict(key = "T(org.slf4j.MDC).get('tenantId') + ':user:' + #p0", beforeInvocation = true)
        public void deleteBefore(String name) {
            throw new IllegalStateException("business failure");
        }

        public String selfFind(String name) {
            return find(name, true);
        }

        private String load(String name) {
            calls.incrementAndGet();
            return "missing".equals(name) ? null : name;
        }
    }

    static class LocalTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
