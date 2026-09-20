package top.egon.cola.component.common.cache.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.slf4j.MDC;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.redisson.api.RMapCache;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import top.egon.cola.component.common.cache.autoconfigure.EgonColaCacheProperties;
import top.egon.cola.component.common.cache.codec.EgonColaCacheCodecs;
import top.egon.cola.component.common.cache.event.EgonColaCacheChangedEvent;
import top.egon.cola.component.common.cache.event.EgonColaCacheChangedEvent.KeyGuard;
import top.egon.cola.component.common.cache.event.EgonColaCacheChangedOperation;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 两级缓存设施单点收口（MC-PATTERN-001）：区域句柄、L2 {@code RMapCache} 懒建、
 * 事件发布与本地失效原语、回源互斥锁与延迟二拍调度器全部经此类暴露。
 */
@Slf4j
@RequiredArgsConstructor
public class EgonColaTwoLevelCacheManager implements CacheManager, DisposableBean {

    private static final ObjectMapper EVENT_MAPPER = EgonColaCacheCodecs.eventMapper();

    private final EgonColaCacheProperties properties;
    private final RedissonClient redissonClient;

    private final JsonJacksonCodec valueCodec = EgonColaCacheCodecs.mapValueCodec();
    private final Map<String, EgonColaTwoLevelCache> handles = new ConcurrentHashMap<>();
    private final Map<String, RMapCache<String, Object>> l2Handles = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "egon-cache-second-evict");
                thread.setDaemon(true);
                return thread;
            });

    private volatile String originNodeId;
    private volatile RTopic topic;

    @Override
    public Cache getCache(String name) {
        KeyGuard.requireValidName(name);
        return handles.computeIfAbsent(name, n -> new EgonColaTwoLevelCache(n, this));
    }

    @Override
    public Collection<String> getCacheNames() {
        return Set.copyOf(handles.keySet());
    }

    public synchronized String originNodeId() {
        String current = originNodeId;
        if (current == null) {
            String configured = properties.getNodeId();
            current = configured == null || configured.isBlank()
                    ? "node-" + UUID.randomUUID()
                    : configured;
            originNodeId = current;
        }
        return current;
    }

    public EgonColaCacheProperties properties() {
        return properties;
    }

    RMapCache<String, Object> l2(String cacheName) {
        return l2Handles.computeIfAbsent(cacheName,
                n -> redissonClient.getMapCache(properties.getKeyPrefix() + ":" + n, valueCodec));
    }

    public RLock lock(String cacheName, String key) {
        return redissonClient.getLock(properties.getKeyPrefix() + ":lock:" + cacheName + ":" + key);
    }

    public ScheduledExecutorService scheduler() {
        return scheduler;
    }

    public void publish(String cacheName, EgonColaCacheChangedOperation operation, List<String> keys) {
        EgonColaCacheChangedEvent event = null;
        try {
            event = new EgonColaCacheChangedEvent(EgonColaCacheChangedEvent.SCHEMA_VERSION,
                    UUID.randomUUID().toString(), originNodeId(), Instant.now(), cacheName, operation, keys);
            topic().publish(EVENT_MAPPER.writeValueAsString(event));
        } catch (Exception ex) {
            log.warn("CACHE_L2_OPERATION_FAILED region={} op={} keyCount={} originNode={} eventId={}",
                    cacheName, operation, keys.size(), abbreviate(originNodeId()),
                    event == null ? "-" : abbreviate(event.eventId()), ex);
        }
    }

    public void applyLocalEviction(String cacheName, List<String> keys) {
        EgonColaTwoLevelCache handle = handles.get(cacheName);
        if (handle != null) {
            handle.evictLocal(keys);
        } else {
            try {
                l2(cacheName).fastRemove(keys.toArray(String[]::new));
            } catch (RuntimeException ex) {
                log.warn("CACHE_L2_OPERATION_FAILED region={} op=EVICT", cacheName, ex);
            }
        }
    }

    public void applyRemotePut(String cacheName, List<String> keys) {
        EgonColaTwoLevelCache handle = handles.get(cacheName);
        if (handle != null) {
            handle.invalidateLocal(keys);
        }
    }

    /**
     * 租户段 glob 失效（ASM-003）：本地 L1 前缀过滤 + L2 readAllKeySet 过滤后分批 fastRemove。
     */
    public void applyLocalPrefixEviction(String cacheName, String globKey) {
        KeyGuard.requireGlob(globKey);
        EgonColaTwoLevelCache handle = handles.get(cacheName);
        if (handle != null) {
            handle.applyPrefixLocally(globKey);
        }
        String tenantPrefix = globKey.substring(0, globKey.indexOf(':') + 1);
        RMapCache<String, Object> l2;
        List<String> doomed;
        try {
            l2 = l2(cacheName);
            doomed = l2.readAllKeySet().stream()
                    .filter(key -> key.startsWith(tenantPrefix))
                    .toList();
        } catch (RuntimeException ex) {
            log.warn("CACHE_L2_OPERATION_FAILED region={} op=PREFIX_EVICT_READ", cacheName, ex);
            return;
        }
        int maxKeys = properties.getBatch().getMaxKeys();
        List<String> remaining = doomed;
        for (int from = 0; from < remaining.size(); from += maxKeys) {
            List<String> chunk = remaining.subList(from, Math.min(from + maxKeys, remaining.size()));
            try {
                l2.fastRemove(chunk.toArray(String[]::new));
            } catch (RuntimeException ex) {
                log.warn("CACHE_L2_OPERATION_FAILED region={} op=PREFIX_EVICT chunkSize={}",
                        cacheName, chunk.size(), ex);
            }
        }
    }

    /**
     * 远端事件仅清 L1，避免迟到的失效事件删除共享 L2 中更新后的值。
     */
    public void applyRemotePrefixEviction(String cacheName, String globKey) {
        KeyGuard.requireGlob(globKey);
        EgonColaTwoLevelCache handle = handles.get(cacheName);
        if (handle != null) {
            handle.applyPrefixLocally(globKey);
        }
    }

    String currentTenant() {
        String raw = MDC.get(properties.getTenantMdcKey());
        if (raw == null || !raw.matches("[0-9]+")) {
            throw new IllegalArgumentException("CACHE_KEY_TENANT_MISMATCH: tenant context missing or invalid");
        }
        try {
            return Long.toString(Long.parseLong(raw));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("CACHE_KEY_TENANT_MISMATCH: tenant context invalid", ex);
        }
    }

    static boolean inTransaction() {
        return TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive();
    }

    /**
     * 登记时已完成租户校验，回调直接使用快照，避免依赖提交阶段的 MDC。
     */
    void afterCommit(Runnable operation) {
        if (inTransaction()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    operation.run();
                }
            });
        } else {
            operation.run();
        }
    }

    @Override
    public void destroy() {
        scheduler.shutdownNow();
        handles.clear();
        l2Handles.clear();
    }

    /** 事件 topic 句柄懒建（订阅与发布共用同一 {@link StringCodec} 通道）。 */
    public RTopic topic() {
        RTopic current = topic;
        if (current == null) {
            current = redissonClient.getTopic(properties.getRedis().getTopic(), StringCodec.INSTANCE);
            topic = current;
        }
        return current;
    }

    private static String abbreviate(String value) {
        return value.length() <= 8 ? value : value.substring(0, 8);
    }
}
