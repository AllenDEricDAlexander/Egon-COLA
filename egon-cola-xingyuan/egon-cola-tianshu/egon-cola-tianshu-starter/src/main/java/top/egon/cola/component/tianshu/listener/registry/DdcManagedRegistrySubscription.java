package top.egon.cola.component.tianshu.listener.registry;

import jakarta.validation.ConstraintViolationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.lang.Nullable;
import org.redisson.client.codec.StringCodec;
import org.slf4j.MDC;
import top.egon.cola.component.tianshu.model.event.DdcRegistryRevisionEvent;
import top.egon.cola.component.tianshu.model.registry.DdcRegistryEvent;
import top.egon.cola.component.tianshu.api.registry.DdcRegistrySubscription;
import top.egon.cola.component.tianshu.model.tenant.DdcTenantMessageDTO;
import top.egon.cola.component.tianshu.observability.DdcTraceSupport;
import top.egon.cola.component.tianshu.redis.DdcRedisTopicSubscription;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 统一管理注册主题监听、刷新合并、定时协调和幂等关闭。 /
 * Manages registry topic listeners, refresh coalescing, periodic reconciliation, and idempotent closure.
 */
@Slf4j
abstract class DdcManagedRegistrySubscription implements DdcRegistrySubscription {

    private static final TypeReference<DdcTenantMessageDTO<JsonNode>> ENVELOPE_JSON_TYPE =
            new TypeReference<>() { };

    protected final long tenantId;

    private final ObjectMapper eventMapper;

    private final ValidationUtils validationUtils;

    private final RedissonClient redissonClient;

    private final ScheduledExecutorService scheduler;

    private final long reconcileIntervalMillis;

    private final Consumer<DdcManagedRegistrySubscription> closedCallback;

    private final AtomicBoolean closed = new AtomicBoolean();

    private final AtomicBoolean refreshQueued = new AtomicBoolean();

    private DdcRedisTopicSubscription<String> topicSubscription;

    private ScheduledFuture<?> reconciliation;

    /**
     * 创建受协调器管理的订阅生命周期。 / Creates a subscription lifecycle managed by a coordinator.
     *
     * @param redissonClient          Redis Topic 客户端 / Redis topic client
     * @param scheduler               串行刷新调度器 / serial refresh scheduler
     * @param reconcileIntervalMillis 对账间隔毫秒数 / reconciliation interval in milliseconds
     * @param closedCallback          关闭后注销回调 / post-close removal callback
     */
    DdcManagedRegistrySubscription(RedissonClient redissonClient,
                                   long tenantId,
                                   ObjectMapper eventMapper,
                                   ValidationUtils validationUtils,
                                   ScheduledExecutorService scheduler,
                                   long reconcileIntervalMillis,
                                   Consumer<DdcManagedRegistrySubscription> closedCallback) {
        this.redissonClient = Objects.requireNonNull(redissonClient, "redissonClient");
        if (tenantId <= 0) {
            throw new IllegalArgumentException("tenantId must be positive");
        }
        this.tenantId = tenantId;
        this.eventMapper = Objects.requireNonNull(eventMapper, "eventMapper");
        this.validationUtils = Objects.requireNonNull(validationUtils, "validationUtils");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.reconcileIntervalMillis = reconcileIntervalMillis;
        this.closedCallback = Objects.requireNonNull(closedCallback, "closedCallback");
    }

    /**
     * 注册主题监听器，执行首次刷新并安排定时协调。 /
     * Registers topic listeners, performs the initial refresh, and schedules reconciliation.
     *
     * @param topicNames 待监听的 Redis 主题名 / Redis topic names
     */
    protected final void start(List<String> topicNames) {
        List<RTopic> topics = topicNames.stream()
                .map(topicName -> redissonClient.getTopic(
                        topicName,
                        StringCodec.INSTANCE
                ))
                .toList();
        topicSubscription = new DdcRedisTopicSubscription<>(
                topics,
                String.class,
                this::onEvent
        );
        runWithTenant(this::refreshWithTrace);
        reconciliation = scheduler.scheduleWithFixedDelay(
                DdcTraceSupport.wrapNewOperation(
                        "registry-reconcile",
                        this::safeRefresh
                ),
                reconcileIntervalMillis,
                reconcileIntervalMillis,
                TimeUnit.MILLISECONDS
        );
    }

    protected abstract boolean relevant(@Nullable DdcRegistryEvent event);

    protected abstract void refresh();

    protected abstract void expireLocal();

    private void onEvent(CharSequence channel, String payload) {
        if (payload == null) {
            log.warn("Discarding empty Tianshu registry message; tenantId={}", tenantId);
            return;
        }
        try {
            DdcTenantMessageDTO<JsonNode> rawEnvelope =
                    eventMapper.readValue(payload, ENVELOPE_JSON_TYPE);
            if (rawEnvelope == null) {
                log.warn("Discarding Tianshu registry message without a tenant envelope; tenantId={}",
                        tenantId);
                return;
            }
            try {
                validationUtils.validate(rawEnvelope);
            } catch (ConstraintViolationException invalidEnvelope) {
                log.warn("Discarding Tianshu registry message with invalid envelope constraints; tenantId={}",
                        tenantId);
                return;
            }
            if (!rawEnvelope.belongsToTenant(tenantId)) {
                log.warn("Discarding Tianshu registry message with an invalid tenant envelope; tenantId={}",
                        tenantId);
                return;
            }
            DdcRegistryEvent registryEvent = registryEvent(rawEnvelope);
            if (registryEvent == null) {
                return;
            }
            runWithTenant(() -> {
                if (relevant(registryEvent)) {
                    queueRefresh();
                }
            });
        } catch (JsonProcessingException exception) {
            log.warn("Discarding invalid Tianshu registry event; tenantId={}, reason={}",
                    tenantId, exception.getClass().getSimpleName());
        }
    }

    private DdcRegistryEvent registryEvent(DdcTenantMessageDTO<JsonNode> rawEnvelope)
            throws JsonProcessingException {
        JsonNode payload = rawEnvelope.getPayload();
        if (payload == null || payload.isNull()) {
            log.warn("Discarding empty Tianshu registry payload; tenantId={}", tenantId);
            return null;
        }
        if (payload.has("eventId")) {
            DdcRegistryRevisionEvent event = eventMapper.treeToValue(payload, DdcRegistryRevisionEvent.class);
            DdcTenantMessageDTO<DdcRegistryRevisionEvent> typedEnvelope =
                    new DdcTenantMessageDTO<>(rawEnvelope.getTenantId(), event);
            try {
                validationUtils.validate(typedEnvelope);
            } catch (ConstraintViolationException invalidEvent) {
                log.warn("Discarding Tianshu registry event with invalid schema or revision; tenantId={}",
                        tenantId);
                return null;
            }
            if (!Long.valueOf(tenantId).equals(event.getTenantId()) || !event.hasValidIdentity()) {
                log.warn("Discarding Tianshu registry event with invalid identity; tenantId={}, eventId={}",
                        tenantId, event.getEventId());
                return null;
            }
            return event.getEvent();
        }

        DdcRegistryEvent legacyEvent = eventMapper.treeToValue(payload, DdcRegistryEvent.class);
        DdcTenantMessageDTO<DdcRegistryEvent> legacyEnvelope =
                new DdcTenantMessageDTO<>(rawEnvelope.getTenantId(), legacyEvent);
        try {
            validationUtils.validate(legacyEnvelope);
        } catch (ConstraintViolationException invalidLegacyEvent) {
            log.warn("Discarding legacy Tianshu registry event with invalid revision; tenantId={}", tenantId);
            return null;
        }
        return legacyEvent;
    }

    protected final void queueRefresh() {
        if (!closed.get() && refreshQueued.compareAndSet(false, true)) {
            scheduler.execute(() -> {
                try {
                    try (DdcTraceSupport.Scope ignored =
                                 DdcTraceSupport.openOperation("registry-event")) {
                        safeRefresh();
                    }
                } finally {
                    refreshQueued.set(false);
                }
            });
        }
    }

    protected final void safeRefresh() {
        if (closed.get()) {
            return;
        }
        runWithTenant(() -> {
            try {
                refreshWithTrace();
            } catch (RuntimeException exception) {
                log.warn("Tianshu registry reconciliation failed for tenantId={}, reason={}",
                        tenantId, exception.getClass().getSimpleName());
                expireLocal();
            }
        });
    }

    private void refreshWithTrace() {
        try (DdcTraceSupport.Scope ignored =
                     DdcTraceSupport.openOperation("registry-refresh")) {
            refresh();
        }
    }

    protected final <T> void notifySafely(Consumer<T> listener, T value) {
        try {
            listener.accept(value);
        } catch (RuntimeException exception) {
            log.warn("Tianshu registry listener failed for tenantId={}, reason={}",
                    tenantId, exception.getClass().getSimpleName());
        }
    }

    private void runWithTenant(Runnable action) {
        Map<String, String> previous = MDC.getCopyOfContextMap();
        try {
            MDC.put("tenantId", Long.toString(tenantId));
            action.run();
        } finally {
            if (previous == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(previous);
            }
        }
    }

    /**
     * 取消协调任务、移除 Topic 监听器并从协调器注销。 /
     * Cancels reconciliation, removes topic listeners, and unregisters from the coordinator.
     */
    @Override
    public final void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        if (reconciliation != null) {
            reconciliation.cancel(false);
        }
        if (topicSubscription != null) {
            topicSubscription.close();
        }
        closedCallback.accept(this);
    }
}
