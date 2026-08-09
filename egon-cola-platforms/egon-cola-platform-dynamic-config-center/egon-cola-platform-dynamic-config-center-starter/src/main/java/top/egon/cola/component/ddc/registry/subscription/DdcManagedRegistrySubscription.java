package top.egon.cola.component.ddc.registry.subscription;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.egon.cola.component.ddc.model.registry.DdcRegistryEvent;
import top.egon.cola.component.ddc.registry.DdcRegistrySubscription;
import top.egon.cola.component.ddc.trace.DdcTraceSupport;
import top.egon.cola.component.ddc.transport.redis.DdcRedisTopicSubscription;

import java.util.List;
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
abstract class DdcManagedRegistrySubscription implements DdcRegistrySubscription {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DdcManagedRegistrySubscription.class);

    private static final ObjectMapper EVENT_MAPPER =
            new ObjectMapper().registerModule(new JavaTimeModule());

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
                                   ScheduledExecutorService scheduler,
                                   long reconcileIntervalMillis,
                                   Consumer<DdcManagedRegistrySubscription> closedCallback) {
        this.redissonClient = Objects.requireNonNull(redissonClient, "redissonClient");
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
        refreshWithTrace();
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

    protected abstract boolean relevant(DdcRegistryEvent event);

    protected abstract void refresh();

    protected abstract void expireLocal();

    private void onEvent(CharSequence channel, String payload) {
        try {
            DdcRegistryEvent event =
                    EVENT_MAPPER.readValue(payload, DdcRegistryEvent.class);
            if (relevant(event)) {
                queueRefresh();
            }
        } catch (JsonProcessingException exception) {
            LOGGER.warn("DDC registry event is invalid", exception);
        }
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
        try {
            refreshWithTrace();
        } catch (RuntimeException exception) {
            LOGGER.warn("DDC registry reconciliation failed", exception);
            expireLocal();
        }
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
            LOGGER.warn("DDC registry listener failed", exception);
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
