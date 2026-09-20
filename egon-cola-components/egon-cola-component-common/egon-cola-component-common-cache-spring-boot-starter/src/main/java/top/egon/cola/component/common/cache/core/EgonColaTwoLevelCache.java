package top.egon.cola.component.common.cache.core;

import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RMapCache;
import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import top.egon.cola.component.common.cache.autoconfigure.EgonColaCacheProperties;
import top.egon.cola.component.common.cache.event.EgonColaCacheChangedEvent.KeyGuard;
import top.egon.cola.component.common.cache.event.EgonColaCacheChangedOperation;
import top.egon.cola.component.common.cache.model.EgonColaCacheNullValueBO;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * 两级读写内核（Template Method 骨 + 互斥回源）：L1 Guava 限额 + 值内嵌逐条 deadline，
 * L2 {@code RMapCache} 逐条 TTL；单次 jitter 采样共享给双级（PC-004，L1 截止恒 ≤ L2 存活）。
 */
@Slf4j
public class EgonColaTwoLevelCache extends AbstractValueAdaptingCache {

    /** Guava 33.6 已移除 {@code Expiry}，逐条 TTL 以值内嵌绝对截止纳秒实现，读取时自校验。 */
    record L1Value(Object value, long expireAtNanos) {
    }

    private final String name;
    private final EgonColaTwoLevelCacheManager manager;
    private final com.google.common.cache.Cache<String, L1Value> l1;
    private final Random random = new Random();
    private final ConcurrentHashMap<String, CompletableFuture<Object>> loads = new ConcurrentHashMap<>();

    EgonColaTwoLevelCache(String name, EgonColaTwoLevelCacheManager manager) {
        super(true);
        this.name = name;
        this.manager = manager;
        this.l1 = CacheBuilder.newBuilder()
                .maximumSize(manager.properties().getL1().getMaxSize())
                .build();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return l1;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        String k = requireKey(key);
        if (valueLoader == null) {
            throw new IllegalArgumentException("CACHE_LOADER_REQUIRED");
        }
        // 事务内加载不能与其他事务共享未提交结果，也不能提前发布到缓存。
        if (EgonColaTwoLevelCacheManager.inTransaction()) {
            T value = callLoader(k, valueLoader);
            put(k, value);
            return value;
        }
        Object hit = lookup(k);
        if (hit != null) {
            return castValue(fromStoreValue(hit));
        }
        CompletableFuture<Object> pending = new CompletableFuture<>();
        CompletableFuture<Object> existing = loads.putIfAbsent(k, pending);
        if (existing != null) {
            try {
                return castValue(existing.join());
            } catch (CompletionException ex) {
                Throwable cause = ex.getCause();
                throw new Cache.ValueRetrievalException(k, valueLoader,
                        cause instanceof Cache.ValueRetrievalException retrieval ? retrieval.getCause() : cause);
            }
        }
        try {
            T value = loadWithLock(k, valueLoader);
            pending.complete(value);
            return value;
        } catch (RuntimeException | Error ex) {
            pending.completeExceptionally(ex);
            throw ex;
        } finally {
            loads.remove(k, pending);
        }
    }

    private <T> T loadWithLock(String k, Callable<T> valueLoader) {
        Object hit = lookup(k);
        if (hit != null) {
            return castValue(fromStoreValue(hit));
        }
        EgonColaCacheProperties.Lock lockConfig = manager.properties().getLock();
        RLock lock;
        boolean acquired;
        try {
            lock = manager.lock(name, k);
            acquired = lock.tryLock(lockConfig.getWaitTime().toMillis(),
                    lockConfig.getLeaseTime().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return callLoader(k, valueLoader);
        } catch (RuntimeException ex) {
            log.warn("CACHE_L2_OPERATION_FAILED region={} op=LOCK_ACQUIRE", name, ex);
            return callLoader(k, valueLoader);
        }
        if (!acquired) {
            return callLoader(k, valueLoader);
        }
        try {
            Object recheck = lookup(k);
            if (recheck != null) {
                return castValue(fromStoreValue(recheck));
            }
            T value = callLoader(k, valueLoader);
            writeThrough(k, toStoreValue(value));
            return value;
        } finally {
            try {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            } catch (RuntimeException ex) {
                log.warn("CACHE_L2_OPERATION_FAILED region={} op=LOCK_RELEASE", name, ex);
            }
        }
    }

    @Override
    public void put(Object key, Object value) {
        String k = requireKey(key);
        Object stored = toStoreValue(value);
        manager.afterCommit(() -> writeThrough(k, stored));
    }

    @Override
    public void evict(Object key) {
        String k = requireKey(key);
        manager.afterCommit(() -> evictNow(k));
    }

    @Override
    public boolean evictIfPresent(Object key) {
        // Spring 用于 beforeInvocation=true，必须立即生效，不能延迟到提交后。
        evictNow(requireKey(key));
        return false;
    }

    private void evictNow(String key) {
        evictLocal(List.of(key));
        manager.publish(name, EgonColaCacheChangedOperation.EVICT, List.of(key));
    }

    @Override
    public void clear() {
        String glob = manager.currentTenant() + ":*";
        manager.afterCommit(() -> clearTenant(glob));
    }

    @Override
    public boolean invalidate() {
        clearTenant(manager.currentTenant() + ":*");
        return false;
    }

    private void clearTenant(String glob) {
        manager.applyLocalPrefixEviction(name, glob);
        manager.publish(name, EgonColaCacheChangedOperation.PREFIX_EVICT, List.of(glob));
    }

    @Override
    protected Object lookup(Object key) {
        String k = requireKey(key);
        if (EgonColaTwoLevelCacheManager.inTransaction()) {
            return null;
        }
        L1Value hit = l1.getIfPresent(k);
        if (hit != null) {
            if (hit.expireAtNanos() > System.nanoTime()) {
                return hit.value();
            }
            l1.invalidate(k);
        }
        return fromL2(k);
    }

    @Override
    protected Object toStoreValue(Object value) {
        return value == null ? EgonColaCacheNullValueBO.INSTANCE : value;
    }

    @Override
    protected Object fromStoreValue(Object storeValue) {
        return storeValue instanceof EgonColaCacheNullValueBO ? null : storeValue;
    }

    void evictLocal(List<String> keys) {
        l1.invalidateAll(keys);
        try {
            manager.l2(name).fastRemove(keys.toArray(String[]::new));
        } catch (RuntimeException ex) {
            log.warn("CACHE_L2_OPERATION_FAILED region={} op=EVICT keyCount={}", name, keys.size(), ex);
        }
    }

    void invalidateLocal(List<String> keys) {
        l1.invalidateAll(keys);
    }

    void applyPrefixLocally(String globKey) {
        String tenantPrefix = globKey.substring(0, globKey.indexOf(':') + 1);
        List<String> doomed = l1.asMap().keySet().stream()
                .filter(key -> key.startsWith(tenantPrefix))
                .toList();
        l1.invalidateAll(doomed);
    }

    private Object fromL2(String k) {
        Object stored;
        long remainMillis;
        long startedAt = System.nanoTime();
        try {
            RMapCache<String, Object> l2 = manager.l2(name);
            stored = l2.get(k);
            if (stored == null) {
                return null;
            }
            remainMillis = l2.remainTimeToLive(k);
        } catch (RuntimeException ex) {
            log.warn("CACHE_L2_OPERATION_FAILED region={} op=LOOKUP", name, ex);
            return null;
        }
        long deadline = startedAt + TimeUnit.MILLISECONDS.toNanos(remainMillis);
        if (remainMillis <= 0 || deadline <= System.nanoTime()) {
            return null;
        }
        l1.put(k, new L1Value(stored, deadline));
        return stored;
    }

    private void writeThrough(String k, Object stored) {
        long ttlMillis = ttlMillis(stored);
        l1.put(k, new L1Value(stored, System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(ttlMillis)));
        try {
            manager.l2(name).fastPut(k, stored, ttlMillis, TimeUnit.MILLISECONDS);
        } catch (RuntimeException ex) {
            log.warn("CACHE_L2_OPERATION_FAILED region={} op=WRITE keyCount=1", name, ex);
        }
        manager.publish(name, EgonColaCacheChangedOperation.PUT, List.of(k));
    }

    private long ttlMillis(Object stored) {
        EgonColaCacheProperties.Ttl ttl = manager.properties().getTtl();
        boolean nullValue = stored instanceof EgonColaCacheNullValueBO;
        Duration base = nullValue ? ttl.getNullExpire() : ttl.getExpire();
        double ratio = ttl.getJitterRatio();
        EgonColaCacheProperties.RegionTtl region = manager.properties().getRegions().get(name);
        if (region != null) {
            Duration override = nullValue ? region.getNullExpire() : region.getExpire();
            base = override == null ? base : override;
            ratio = region.getJitterRatio() == null ? ratio : region.getJitterRatio();
        }
        return Jitter.jitteredMillis(base.toMillis(), ratio, random);
    }

    private <T> T callLoader(String k, Callable<T> valueLoader) {
        try {
            return valueLoader.call();
        } catch (Exception ex) {
            throw new Cache.ValueRetrievalException(k, valueLoader, ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T castValue(Object value) {
        return (T) value;
    }

    private String requireKey(Object key) {
        if (!(key instanceof String k)) {
            throw new IllegalArgumentException("CACHE_KEY_TENANT_MISMATCH: " + key);
        }
        KeyGuard.requireTenant(k, Long.parseLong(manager.currentTenant()));
        return k;
    }

    /** 单次 TTL 抖动采样：{@code [base, base*(1+ratio)]}，ratio=0 恒等。 */
    static final class Jitter {

        private Jitter() {
        }

        static long jitteredMillis(long baseMillis, double ratio, Random random) {
            return baseMillis + (long) (baseMillis * ratio * random.nextDouble());
        }
    }
}
