package top.egon.cola.component.common.cache.port;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cache.Cache;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import top.egon.cola.component.common.cache.autoconfigure.EgonColaCacheProperties;
import top.egon.cola.component.common.cache.core.EgonColaTwoLevelCacheManager;
import top.egon.cola.component.common.cache.event.EgonColaCacheChangedEvent.KeyGuard;
import top.egon.cola.component.common.core.cache.EgonColaCachePort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import static top.egon.cola.component.common.cache.event.EgonColaCacheChangedOperation.EVICT;
import static top.egon.cola.component.common.cache.event.EgonColaCacheChangedOperation.PREFIX_EVICT;

/**
 * {@link EgonColaCachePort} 的两级缓存实现：读侧复用内核互斥回源，写侧以线程内
 * {@link EvictionBuffer} 缓冲、事务 afterCommit 后执行"本地双级失效 + 通用事件发布 +
 * 延迟第二拍重放"。登记期守卫（区域名/键形状/租户段）fail-fast，先于任何缓存交互；
 * 提交后任何缓存故障只记日志不上抛（REQ-025）。
 */
@Slf4j
@RequiredArgsConstructor
public class EgonColaTwoLevelCachePort implements EgonColaCachePort {

    private final EgonColaTwoLevelCacheManager manager;
    private final EgonColaCacheProperties properties;

    private final ThreadLocal<EvictionBuffer> buffers = ThreadLocal.withInitial(EvictionBuffer::new);

    @Override
    public Object get(String cacheName, String key, Supplier<Object> loader) {
        if (loader == null) {
            throw new IllegalArgumentException("CACHE_LOADER_REQUIRED");
        }
        requireReadableKey(cacheName, key);
        Cache cache = manager.getCache(cacheName);
        return cache.get(key, (Callable<Object>) loader::get);
    }

    @Override
    public List<Object> getAll(String cacheName, List<String> keys, Function<String, Object> loader) {
        if (loader == null) {
            throw new IllegalArgumentException("CACHE_LOADER_REQUIRED");
        }
        KeyGuard.requireValidName(cacheName);
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        int maxKeys = properties.getBatch().getMaxKeys();
        if (keys.size() > maxKeys) {
            throw new IllegalArgumentException("CACHE_KEY_SIZE_EXCEEDED: " + keys.size());
        }
        long tenant = currentTenantOrThrow();
        for (String key : keys) {
            requireExactKeyOfTenant(key, tenant);
        }
        Cache cache = manager.getCache(cacheName);
        List<Object> values = new ArrayList<>(keys.size());
        for (String key : keys) {
            values.add(cache.get(key, (Callable<Object>) () -> loader.apply(key)));
        }
        return values;
    }

    @Override
    public void registerEvictionAfterCommit(String cacheName, Collection<String> exactKeys,
                                            Collection<String> globPatterns) {
        long tenant = currentTenantOrThrow();
        KeyGuard.requireValidName(cacheName);
        if (exactKeys != null) {
            for (String key : exactKeys) {
                requireExactKeyOfTenant(key, tenant);
            }
        }
        if (globPatterns != null) {
            for (String glob : globPatterns) {
                KeyGuard.requireGlob(glob);
                requireTenantSegment(glob, tenant);
            }
        }
        EvictionBuffer buffer = buffers.get();
        boolean needsSynchronization = buffer.offer(cacheName, exactKeys, globPatterns);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            if (needsSynchronization) {
                TransactionSynchronizationManager
                        .registerSynchronization(new EvictionSynchronization(buffer));
            }
        } else {
            log.warn("no transaction context; immediate eviction applied region={}", cacheName);
            flushEvictions(buffer.drain());
        }
    }

    private long currentTenantOrThrow() {
        String raw = MDC.get(properties.getTenantMdcKey());
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("CACHE_KEY_TENANT_MISMATCH: tenant context missing");
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("CACHE_KEY_TENANT_MISMATCH: " + raw);
        }
    }

    private void requireReadableKey(String cacheName, String key) {
        KeyGuard.requireValidName(cacheName);
        requireExactKeyOfTenant(key, currentTenantOrThrow());
    }

    private static void requireExactKeyOfTenant(String key, long tenant) {
        KeyGuard.requireExactKey(key);
        requireTenantSegment(key, tenant);
    }

    private static void requireTenantSegment(String keyOrGlob, long tenant) {
        int separator = keyOrGlob.indexOf(':');
        String segment = separator < 0 ? keyOrGlob : keyOrGlob.substring(0, separator);
        long actual;
        try {
            actual = Long.parseLong(segment);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("CACHE_KEY_TENANT_MISMATCH: " + keyOrGlob);
        }
        if (actual != tenant) {
            throw new IllegalArgumentException("CACHE_KEY_TENANT_MISMATCH: " + keyOrGlob);
        }
    }

    /** afterCommit 语义：先主拍（本地双级失效 + 发布），再排延迟第二拍（仅本地、不再发布）。 */
    private void flushEvictions(List<EvictionBuffer.Pending> items) {
        for (EvictionBuffer.Pending item : items) {
            try {
                applyLocallyAndPublish(item);
                manager.scheduler().schedule(() -> replayLocally(item),
                        properties.getSecondEvictDelay().toMillis(), TimeUnit.MILLISECONDS);
            } catch (RejectedExecutionException ex) {
                log.warn("CACHE_L2_OPERATION_FAILED region={} op=SCHEDULER reason=scheduler-closed",
                        item.cacheName(), ex);
            } catch (Exception ex) {
                log.warn("CACHE_L2_OPERATION_FAILED region={}", item.cacheName(), ex);
            }
        }
    }

    private void applyLocallyAndPublish(EvictionBuffer.Pending item) {
        if (!item.exactKeys().isEmpty()) {
            manager.applyLocalEviction(item.cacheName(), item.exactKeys());
            manager.publish(item.cacheName(), EVICT, item.exactKeys());
        }
        for (String glob : item.globs()) {
            manager.applyLocalPrefixEviction(item.cacheName(), glob);
            manager.publish(item.cacheName(), PREFIX_EVICT, List.of(glob));
        }
    }

    private void replayLocally(EvictionBuffer.Pending item) {
        try {
            if (!item.exactKeys().isEmpty()) {
                manager.applyLocalEviction(item.cacheName(), item.exactKeys());
            }
            for (String glob : item.globs()) {
                manager.applyLocalPrefixEviction(item.cacheName(), glob);
            }
        } catch (Exception ex) {
            log.warn("CACHE_L2_OPERATION_FAILED region={} op=REPLAY", item.cacheName(), ex);
        }
    }

    /**
     * 线程内失效登记缓冲（Buffer 模式，镜像既有 after-commit 先例）：同区域合并去重保序，
     * 每事务窗口只挂一根同步器；回滚经 afterCompletion 丢弃，杜绝脏键泄漏到下次提交。
     */
    static final class EvictionBuffer {

        record Pending(String cacheName, List<String> exactKeys, List<String> globs) {
        }

        private static final class RegionBatch {

            private final LinkedHashSet<String> exactKeys = new LinkedHashSet<>();
            private final LinkedHashSet<String> globs = new LinkedHashSet<>();
        }

        private final Map<String, RegionBatch> regions = new LinkedHashMap<>();
        private boolean synchronizationRegistered;

        synchronized boolean offer(String cacheName, Collection<String> exactKeys,
                                   Collection<String> globPatterns) {
            RegionBatch batch = regions.computeIfAbsent(cacheName, name -> new RegionBatch());
            if (exactKeys != null) {
                batch.exactKeys.addAll(exactKeys);
            }
            if (globPatterns != null) {
                batch.globs.addAll(globPatterns);
            }
            if (synchronizationRegistered) {
                return false;
            }
            synchronizationRegistered = true;
            return true;
        }

        synchronized List<Pending> drain() {
            List<Pending> drained = new ArrayList<>(regions.size());
            regions.forEach((name, batch) -> drained.add(new Pending(name,
                    List.copyOf(batch.exactKeys), List.copyOf(batch.globs))));
            regions.clear();
            synchronizationRegistered = false;
            return drained;
        }
    }

    private final class EvictionSynchronization implements TransactionSynchronization {

        private final EvictionBuffer buffer;

        private EvictionSynchronization(EvictionBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public void afterCommit() {
            flushEvictions(buffer.drain());
        }

        @Override
        public void afterCompletion(int status) {
            if (status == STATUS_ROLLED_BACK) {
                buffer.drain();
            } else {
                flushEvictions(buffer.drain());
            }
        }
    }
}
