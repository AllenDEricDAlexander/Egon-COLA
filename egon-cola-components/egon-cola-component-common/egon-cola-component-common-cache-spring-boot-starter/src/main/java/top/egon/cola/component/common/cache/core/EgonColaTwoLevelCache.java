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
import java.util.Set;
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
        Object hit = lookup(k);
        if (hit != null) {
            return castValue(fromStoreValue(hit));
        }
        EgonColaCacheProperties.Lock lockConfig = manager.properties().getLock();
        RLock lock = manager.lock(name, k);
        boolean acquired;
        try {
            acquired = lock.tryLock(lockConfig.getWaitTime().toMillis(),
                    lockConfig.getLeaseTime().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return loadWithoutCaching(k, valueLoader);
        } catch (RuntimeException ex) {
            log.warn("CACHE_L2_OPERATION_FAILED region={} op=LOCK_ACQUIRE", name, ex);
            return loadWithoutCaching(k, valueLoader);
        }
        if (!acquired) {
            return loadWithoutCaching(k, valueLoader);
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
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public void put(Object key, Object value) {
        String k = requireKey(key);
        writeThrough(k, toStoreValue(value));
    }

    @Override
    public void evict(Object key) {
        String k = requireKey(key);
        evictLocal(List.of(k));
        manager.publish(name, EgonColaCacheChangedOperation.EVICT, List.of(k));
    }

    @Override
    public void clear() {
        l1.invalidateAll();
        try {
            RMapCache<String, Object> l2 = manager.l2(name);
            Set<String> keys = l2.readAllKeySet();
            int maxKeys = manager.properties().getBatch().getMaxKeys();
            List<String> remaining = List.copyOf(keys);
            for (int from = 0; from < remaining.size(); from += maxKeys) {
                List<String> chunk = remaining.subList(from, Math.min(from + maxKeys, remaining.size()));
                l2.fastRemove(chunk.toArray(String[]::new));
            }
        } catch (RuntimeException ex) {
            log.warn("CACHE_L2_OPERATION_FAILED region={} op=CLEAR", name, ex);
        }
    }

    @Override
    protected Object lookup(Object key) {
        String k = requireKey(key);
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
        if (remainMillis <= 0) {
            return null;
        }
        l1.put(k, new L1Value(stored, System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(remainMillis)));
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
        Duration base = stored instanceof EgonColaCacheNullValueBO ? ttl.getNullExpire() : ttl.getExpire();
        return Jitter.jitteredMillis(base.toMillis(), ttl.getJitterRatio(), random);
    }

    private <T> T loadWithoutCaching(String k, Callable<T> valueLoader) {
        return callLoader(k, valueLoader);
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

    private static String requireKey(Object key) {
        if (!(key instanceof String k)) {
            throw new IllegalArgumentException("CACHE_KEY_TENANT_MISMATCH: " + key);
        }
        KeyGuard.requireExactKey(k);
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
