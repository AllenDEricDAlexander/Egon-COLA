package top.egon.cola.component.ddc.registry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.egon.cola.component.ddc.common.DdcKeys;
import top.egon.cola.component.ddc.model.registry.DdcRegistryEvent;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.trace.DdcTraceSupport;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 组合 Redis 注册事件与定时全量协调，维护服务实例和服务目录订阅。
 * / Maintains service-instance and service-catalog subscriptions by combining
 * Redis registry events with scheduled full reconciliation.
 */
public final class DdcRegistrySubscriptionManager implements AutoCloseable {

    /** 订阅与协调故障日志记录器。 / Logger for subscription and reconciliation failures. */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(DdcRegistrySubscriptionManager.class);

    /** 反序列化 Redis 注册事件的共享映射器。 / Shared mapper for deserializing Redis registry events. */
    private static final ObjectMapper EVENT_MAPPER =
            new ObjectMapper().registerModule(new JavaTimeModule());

    /** 执行全量快照查询的注册中心客户端。 / Registry client used to load full snapshots. */
    private final DdcServiceRegistryClient loader;

    /** 提供注册事件主题的 Redisson 客户端。 / Redisson client providing registry event topics. */
    private final RedissonClient redissonClient;

    /** 判断本地租约到期使用的时钟。 / Clock used to determine local lease expiration. */
    private final Clock clock;

    /** 串行执行事件刷新与定时协调的调度器。 / Scheduler serializing event refreshes and periodic reconciliation. */
    private final ScheduledExecutorService scheduler;

    /** 定时协调间隔毫秒数。 / Periodic reconciliation interval in milliseconds. */
    private final long reconcileIntervalMillis;

    /** 当前由管理器持有的订阅集合。 / Subscriptions currently owned by the manager. */
    private final Set<ManagedSubscription> subscriptions = ConcurrentHashMap.newKeySet();

    /**
     * 使用 UTC 时钟和单线程守护调度器创建订阅管理器。
     * / Creates a subscription manager with a UTC clock and single-thread daemon scheduler.
     *
     * @param loader 加载全量快照的注册中心客户端 / registry client used to load full snapshots
     * @param redissonClient Redisson 客户端 / Redisson client
     * @param reconcileIntervalSeconds 定时协调间隔秒数 / reconciliation interval in seconds
     * @throws IllegalArgumentException 协调间隔不为正数时抛出 / if the reconciliation interval is not positive
     * @throws NullPointerException 必填依赖为空时抛出 / if a required dependency is {@code null}
     */
    public DdcRegistrySubscriptionManager(DdcServiceRegistryClient loader,
                                          RedissonClient redissonClient,
                                          int reconcileIntervalSeconds) {
        this(
                loader,
                redissonClient,
                Clock.systemUTC(),
                Executors.newSingleThreadScheduledExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "egon-cola-ddc-registry");
                    thread.setDaemon(true);
                    return thread;
                }),
                TimeUnit.SECONDS.toMillis(reconcileIntervalSeconds)
        );
    }

    /**
     * 使用显式时钟和调度器创建订阅管理器，供内部测试与定制使用。
     * / Creates a subscription manager with an explicit clock and scheduler for internal testing and customization.
     *
     * @param loader 加载全量快照的注册中心客户端 / registry client used to load full snapshots
     * @param redissonClient Redisson 客户端 / Redisson client
     * @param clock 租约到期判断时钟 / clock used for lease expiration
     * @param scheduler 刷新与协调调度器 / refresh and reconciliation scheduler
     * @param reconcileIntervalMillis 定时协调间隔毫秒数 / reconciliation interval in milliseconds
     * @throws IllegalArgumentException 协调间隔不为正数时抛出 / if the reconciliation interval is not positive
     * @throws NullPointerException 必填依赖为空时抛出 / if a required dependency is {@code null}
     */
    DdcRegistrySubscriptionManager(DdcServiceRegistryClient loader,
                                   RedissonClient redissonClient,
                                   Clock clock,
                                   ScheduledExecutorService scheduler,
                                   long reconcileIntervalMillis) {
        if (reconcileIntervalMillis <= 0) {
            throw new IllegalArgumentException("reconcile interval must be positive");
        }
        this.loader = Objects.requireNonNull(loader, "loader");
        this.redissonClient = Objects.requireNonNull(redissonClient, "redissonClient");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.reconcileIntervalMillis = reconcileIntervalMillis;
    }

    /**
     * 创建并启动单个服务键的实例订阅。
     * / Creates and starts an instance subscription for one service key.
     *
     * @param serviceKey 服务键 / service key
     * @param listener 快照监听器 / snapshot listener
     * @return 已启动的可关闭订阅 / started closeable subscription
     * @throws RuntimeException 主题注册或首次刷新失败时抛出 / if topic registration or the initial refresh fails
     */
    public DdcRegistrySubscription subscribe(
            DdcServiceKey serviceKey,
            Consumer<DdcServiceSnapshot> listener) {
        InstanceSubscription subscription =
                new InstanceSubscription(serviceKey, listener);
        subscriptions.add(subscription);
        try {
            subscription.start();
            return subscription;
        } catch (RuntimeException exception) {
            subscription.close();
            throw exception;
        }
    }

    /**
     * 创建并启动服务目录订阅。
     * / Creates and starts a service catalog subscription.
     *
     * @param query 包含精确主题范围的服务查询 / service query containing an exact topic scope
     * @param listener 服务目录监听器 / service catalog listener
     * @return 已启动的可关闭订阅 / started closeable subscription
     * @throws IllegalArgumentException 查询缺少精确主题范围时抛出 / if the query lacks an exact topic scope
     * @throws RuntimeException 主题注册或首次刷新失败时抛出 / if topic registration or the initial refresh fails
     */
    public DdcRegistrySubscription subscribeServices(
            DdcServiceQuery query,
            Consumer<DdcServiceCatalogSnapshot> listener) {
        CatalogSubscription subscription =
                new CatalogSubscription(query, listener);
        subscriptions.add(subscription);
        try {
            subscription.start();
            return subscription;
        } catch (RuntimeException exception) {
            subscription.close();
            throw exception;
        }
    }

    /**
     * 关闭全部订阅并立即停止调度器。
     * / Closes all subscriptions and immediately shuts down the scheduler.
     */
    @Override
    public void close() {
        List.copyOf(subscriptions).forEach(ManagedSubscription::close);
        scheduler.shutdownNow();
    }

    /**
     * 统一管理主题监听、刷新合并、定时协调和幂等关闭的订阅基类。
     * / Subscription base class that manages topic listeners, refresh coalescing,
     * periodic reconciliation, and idempotent closure.
     */
    private abstract class ManagedSubscription implements DdcRegistrySubscription {

        /** 订阅是否已关闭。 / Whether the subscription has been closed. */
        private final AtomicBoolean closed = new AtomicBoolean();

        /** 是否已有事件刷新任务排队。 / Whether an event-driven refresh is already queued. */
        private final AtomicBoolean refreshQueued = new AtomicBoolean();

        /** 当前订阅注册的 Redis 主题监听器。 / Redis topic listeners registered by this subscription. */
        private List<TopicRegistration> topicRegistrations = List.of();

        /** 定时协调任务句柄。 / Periodic reconciliation task handle. */
        private ScheduledFuture<?> reconciliation;

        /**
         * 注册主题监听器，执行首次刷新并安排定时协调。
         * / Registers topic listeners, performs the initial refresh, and schedules reconciliation.
         *
         * @param topicNames 待监听的 Redis 主题名 / Redis topic names to listen to
         * @throws RuntimeException 监听器注册或首次刷新失败时抛出 / if listener registration or initial refresh fails
         */
        protected void start(List<String> topicNames) {
            List<TopicRegistration> registered = new ArrayList<>();
            try {
                for (String topicName : topicNames) {
                    RTopic topic = redissonClient.getTopic(topicName, StringCodec.INSTANCE);
                    registered.add(new TopicRegistration(
                            topic,
                            topic.addListener(String.class, this::onEvent)
                    ));
                }
            } catch (RuntimeException exception) {
                registered.forEach(TopicRegistration::remove);
                throw exception;
            }
            topicRegistrations = List.copyOf(registered);
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

        /**
         * 判断注册事件是否与当前订阅相关。
         * / Determines whether a registry event is relevant to this subscription.
         *
         * @param event 注册事件 / registry event
         * @return 需要刷新时为 {@code true} / {@code true} when a refresh is required
         */
        protected abstract boolean relevant(DdcRegistryEvent event);

        /** 执行远端全量刷新。 / Performs a full remote refresh. */
        protected abstract void refresh();

        /**
         * 在远端刷新失败时按本地可证明信息淘汰状态。
         * / Expires state using locally provable information after a remote refresh failure.
         */
        protected abstract void expireLocal();

        /**
         * 解析主题事件，并在事件相关时合并排队一次刷新。
         * / Parses a topic event and queues a coalesced refresh when relevant.
         *
         * @param channel 事件来源主题 / source topic of the event
         * @param payload JSON 事件负载 / JSON event payload
         */
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

        /**
         * 在调度器中合并排队一次带追踪的刷新。
         * / Queues one coalesced traced refresh on the scheduler.
         */
        protected void queueRefresh() {
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

        /**
         * 在未关闭时执行刷新；失败时记录告警并触发本地淘汰。
         * / Refreshes while open; on failure, logs a warning and triggers local expiration.
         */
        protected void safeRefresh() {
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

        /**
         * 在独立注册中心刷新追踪范围内执行刷新。
         * / Performs a refresh within a dedicated registry-refresh trace scope.
         */
        private void refreshWithTrace() {
            try (DdcTraceSupport.Scope ignored =
                         DdcTraceSupport.openOperation("registry-refresh")) {
                refresh();
            }
        }

        /**
         * 调用监听器并隔离其运行时异常。
         * / Invokes a listener while isolating its runtime exceptions.
         *
         * @param listener 目标监听器 / target listener
         * @param value 通知值 / notification value
         * @param <T> 通知值类型 / notification value type
         */
        protected <T> void notifySafely(Consumer<T> listener, T value) {
            try {
                listener.accept(value);
            } catch (RuntimeException exception) {
                LOGGER.warn("DDC registry listener failed", exception);
            }
        }

        /**
         * 幂等关闭订阅、取消协调任务、移除主题监听并从管理器注销。
         * / Idempotently closes the subscription, cancels reconciliation, removes topic listeners,
         * and unregisters it from the manager.
         */
        @Override
        public void close() {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            if (reconciliation != null) {
                reconciliation.cancel(false);
            }
            topicRegistrations.forEach(TopicRegistration::remove);
            subscriptions.remove(this);
        }
    }

    /**
     * 跟踪单个完整服务键实例快照的订阅。
     * / Subscription tracking the instance snapshot of one complete service key.
     */
    private final class InstanceSubscription extends ManagedSubscription {

        /** 被跟踪的完整服务键。 / Complete service key being tracked. */
        private final DdcServiceKey serviceKey;

        /** 接收实例快照变化的监听器。 / Listener receiving instance snapshot changes. */
        private final Consumer<DdcServiceSnapshot> listener;

        /** 最近一次已发布的实例快照。 / Most recently published instance snapshot. */
        private volatile DdcServiceSnapshot current;

        /**
         * 创建实例快照订阅。
         * / Creates an instance snapshot subscription.
         *
         * @param serviceKey 被跟踪的服务键 / service key to track
         * @param listener 快照监听器 / snapshot listener
         */
        private InstanceSubscription(DdcServiceKey serviceKey,
                                     Consumer<DdcServiceSnapshot> listener) {
            this.serviceKey = serviceKey;
            this.listener = listener;
        }

        /** 使用服务键的精确注册主题启动订阅。 / Starts the subscription on the service key's exact registry topic. */
        private void start() {
            start(List.of(
                    DdcKeys.v3RegistryTopic(
                            serviceKey.bizCode(),
                            serviceKey.env(),
                            serviceKey.appCode(),
                            serviceKey.serviceKind(),
                            serviceKey.protocol()
                    )
            ));
        }

        /**
         * 判断事件是否属于被跟踪的服务键。
         * / Determines whether an event belongs to the tracked service key.
         *
         * @param event 注册事件 / registry event
         * @return 服务键相同时为 {@code true} / {@code true} when the service keys are equal
         */
        @Override
        protected boolean relevant(DdcRegistryEvent event) {
            return event != null && serviceKey.equals(event.serviceKey());
        }

        /** 从远端加载实例快照并在发生变化时发布。 / Loads the remote instance snapshot and publishes it when changed. */
        @Override
        protected synchronized void refresh() {
            publishIfChanged(loader.getInstances(serviceKey));
        }

        /**
         * 远端不可用时，仅从当前快照移除本地可判定已到期的实例。
         * / When the remote source is unavailable, removes only instances locally proven expired.
         */
        @Override
        protected synchronized void expireLocal() {
            DdcServiceSnapshot snapshot = current;
            if (snapshot == null) {
                return;
            }
            var live = snapshot.instances().stream()
                    .filter(instance -> instance.leaseExpireAt() != null
                            && instance.leaseExpireAt().isAfter(clock.instant()))
                    .toList();
            if (live.size() != snapshot.instances().size()) {
                publishIfChanged(new DdcServiceSnapshot(
                        serviceKey,
                        snapshot.revision(),
                        live,
                        clock.instant()
                ));
            }
        }

        /**
         * 当修订号或实例列表变化时保存并通知新快照。
         * / Stores and notifies a new snapshot when its revision or instance list changes.
         *
         * @param next 候选新快照 / candidate next snapshot
         */
        private void publishIfChanged(DdcServiceSnapshot next) {
            DdcServiceSnapshot previous = current;
            if (previous != null
                    && previous.revision() == next.revision()
                    && previous.instances().equals(next.instances())) {
                return;
            }
            current = next;
            notifySafely(listener, next);
        }
    }

    /**
     * 跟踪精确注册主题范围内服务目录的订阅。
     * / Subscription tracking a service catalog within an exact registry topic scope.
     */
    private final class CatalogSubscription extends ManagedSubscription {

        /** 服务目录筛选条件。 / Service catalog filter. */
        private final DdcServiceQuery query;

        /** 接收服务目录快照变化的监听器。 / Listener receiving service catalog snapshot changes. */
        private final Consumer<DdcServiceCatalogSnapshot> listener;

        /** 最近一次已发布的服务目录快照。 / Most recently published service catalog snapshot. */
        private volatile DdcServiceCatalogSnapshot current;

        /**
         * 创建服务目录订阅。
         * / Creates a service catalog subscription.
         *
         * @param query 服务目录查询 / service catalog query
         * @param listener 服务目录监听器 / service catalog listener
         */
        private CatalogSubscription(DdcServiceQuery query,
                                    Consumer<DdcServiceCatalogSnapshot> listener) {
            this.query = query;
            this.listener = listener;
        }

        /**
         * 校验精确主题范围并启动目录订阅。
         * / Validates the exact topic scope and starts the catalog subscription.
         *
         * @throws IllegalArgumentException 查询缺少业务、环境、应用、服务类型或协议时抛出
         * / if the query lacks business, environment, application, service kind, or protocol
         */
        private void start() {
            if (!query.hasExactCatalogScope()) {
                throw new IllegalArgumentException(
                        "service catalog subscription requires bizCode, env, appCode, serviceKind and protocol"
                );
            }
            start(List.of(
                    DdcKeys.v3RegistryTopic(
                            query.bizCode(),
                            query.env(),
                            query.appCode(),
                            query.serviceKind(),
                            query.protocol()
                    )
            ));
        }

        /**
         * 判断事件服务键是否满足目录查询。
         * / Determines whether an event's service key matches the catalog query.
         *
         * @param event 注册事件 / registry event
         * @return 服务键存在且匹配时为 {@code true} / {@code true} when a service key exists and matches
         */
        @Override
        protected boolean relevant(DdcRegistryEvent event) {
            return event != null
                    && event.serviceKey() != null
                    && query.matches(event.serviceKey());
        }

        /**
         * 从远端加载目录快照，并在修订号或服务键变化时发布。
         * / Loads the remote catalog and publishes it when revision or service keys change.
         */
        @Override
        protected synchronized void refresh() {
            DdcServiceCatalogSnapshot next = loader.getServiceKeys(query);
            DdcServiceCatalogSnapshot previous = current;
            if (previous != null
                    && previous.revision() == next.revision()
                    && previous.serviceKeys().equals(next.serviceKeys())) {
                return;
            }
            current = next;
            notifySafely(listener, next);
        }

        /**
         * 保留当前目录，因为 Admin 不可用时无法可靠推断目录成员变化。
         * / Retains the current catalog because membership changes cannot be inferred safely without Admin.
         */
        @Override
        protected synchronized void expireLocal() {
            // Service catalog membership cannot be inferred safely without Admin.
        }
    }

    /**
     * Redis 主题及其监听器标识的注册句柄。
     * / Registration handle for a Redis topic and listener identifier.
     *
     * @param topic 已注册监听器的 Redis 主题 / Redis topic with the registered listener
     * @param listenerId Redisson 监听器标识 / Redisson listener identifier
     */
    private record TopicRegistration(RTopic topic, int listenerId) {

        /** 从 Redis 主题移除监听器。 / Removes the listener from the Redis topic. */
        private void remove() {
            topic.removeListener(listenerId);
        }
    }
}
