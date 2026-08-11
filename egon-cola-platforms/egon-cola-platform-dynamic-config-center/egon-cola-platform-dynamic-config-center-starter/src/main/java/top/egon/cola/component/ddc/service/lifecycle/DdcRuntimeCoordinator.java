package top.egon.cola.component.ddc.service.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.lang.Nullable;
import top.egon.cola.component.ddc.api.client.DdcConfigClient;
import top.egon.cola.component.ddc.service.refresh.DdcRefreshService;
import top.egon.cola.component.ddc.model.instance.DdcRuntimeState;
import top.egon.cola.component.ddc.error.DdcException;
import top.egon.cola.component.ddc.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.ddc.model.config.DdcPublishMessage;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.observability.DdcTraceSupport;
import top.egon.cola.component.ddc.state.DdcLeaseSessionHolder;
import top.egon.cola.component.ddc.redis.DdcRedisTopicSubscription;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 协调 DDC 客户端注册、初始拉取、心跳续约、周期对账和优雅下线的生命周期。
 * Coordinates the DDC client lifecycle of registration, initial pull, heartbeat renewal, periodic reconciliation, and graceful offline transition.
 */
public class DdcRuntimeCoordinator implements SmartLifecycle {

    /**
     * 当前类的日志记录器。 Logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DdcRuntimeCoordinator.class);

    /**
     * DDC 客户端配置。 DDC client configuration.
     */
    private final DdcProperties properties;

    /**
     * 执行实例租约操作的服务。 Service performing instance lease operations.
     */
    private final DdcInstanceService instanceService;

    /**
     * 拉取配置的管理端客户端。 Administration client used to pull configuration.
     */
    private final DdcConfigClient adminClient;

    /**
     * 应用初始和对账快照的刷新服务。 Refresh service applying initial and reconciled snapshots.
     */
    private final DdcRefreshService refreshService;

    /**
     * Redis 配置变化订阅。 Redis configuration-change subscription.
     */
    private final DdcRedisTopicSubscription<DdcPublishMessage> subscription;

    /**
     * 当前配置客户端租约会话持有器。 Holder of the current configuration-client lease session.
     */
    private final DdcLeaseSessionHolder sessionHolder;

    /**
     * 当前运行时状态的原子引用。 Atomic reference to the current runtime state.
     */
    private final AtomicReference<DdcRuntimeState> state = new AtomicReference<>(DdcRuntimeState.NEW);

    /**
     * 生命周期是否处于运行状态。 Whether the lifecycle is running.
     */
    private final AtomicBoolean running = new AtomicBoolean();

    /**
     * 周期心跳调度器。 Periodic heartbeat scheduler.
     */
    private volatile ScheduledExecutorService heartbeatScheduler;

    /**
     * 周期配置对账调度器。 Periodic configuration reconciliation scheduler.
     */
    private volatile ScheduledExecutorService reconcileScheduler;

    /**
     * 创建 DDC 运行时协调器。
     * Creates the DDC runtime coordinator.
     *
     * @param properties      DDC 客户端配置; DDC client configuration
     * @param instanceService 实例租约服务; instance lease service
     * @param adminClient     DDC 管理端客户端; DDC administration client
     * @param refreshService  配置刷新服务; configuration refresh service
     * @param subscription    Redis 配置变化订阅; Redis configuration-change subscription
     * @param sessionHolder   租约会话持有器; lease session holder
     */
    public DdcRuntimeCoordinator(DdcProperties properties,
                                 DdcInstanceService instanceService,
                                 DdcConfigClient adminClient,
                                 DdcRefreshService refreshService,
                                 DdcRedisTopicSubscription<DdcPublishMessage> subscription,
                                 DdcLeaseSessionHolder sessionHolder) {
        this.properties = properties;
        this.instanceService = instanceService;
        this.adminClient = adminClient;
        this.refreshService = refreshService;
        this.subscription = subscription;
        this.sessionHolder = sessionHolder;
    }

    /**
     * 校验作用域与订阅状态，注册实例并拉取初始配置，然后启动周期任务。
     * Validates scope and subscription state, registers the instance, pulls initial configuration, and starts periodic tasks.
     *
     * <p>启用 fail-fast 时初始化失败会终止启动；否则进入恢复状态并由心跳任务继续恢复。</p>
     * <p>With fail-fast enabled, initialization failure aborts startup; otherwise the runtime enters recovery and heartbeat continues recovery.</p>
     */
    @Override
    public synchronized void start() {
        validateScope();
        if (!subscription.isActive()) {
            throw new DdcException("DDC Redis subscription is not active");
        }
        if (!running.compareAndSet(false, true)) {
            return;
        }
        state.set(DdcRuntimeState.STARTING);
        try {
            initialize();
            state.set(DdcRuntimeState.READY);
        } catch (RuntimeException exception) {
            if (properties.getConsistency().isFailFast()) {
                state.set(DdcRuntimeState.FAILED);
                running.set(false);
                throw exception;
            }
            state.set(DdcRuntimeState.RECOVERING);
        }
        startSchedulers();
    }

    /**
     * 停止周期任务、尽力通知实例下线、关闭订阅并清除租约。
     * Stops periodic tasks, best-effort reports the instance offline, closes the subscription, and clears the lease.
     */
    @Override
    public synchronized void stop() {
        if (!running.getAndSet(false) && state.get() == DdcRuntimeState.STOPPED) {
            return;
        }
        state.set(DdcRuntimeState.STOPPING);
        ScheduledExecutorService currentHeartbeatScheduler = heartbeatScheduler;
        ScheduledExecutorService currentReconcileScheduler = reconcileScheduler;
        heartbeatScheduler = null;
        reconcileScheduler = null;
        requestShutdown(currentHeartbeatScheduler);
        requestShutdown(currentReconcileScheduler);
        awaitShutdown(currentHeartbeatScheduler);
        awaitShutdown(currentReconcileScheduler);
        sessionHolder.current().ifPresent(session -> {
            try {
                instanceService.offline(session);
            } catch (RuntimeException ignored) {
                // Deregistration is best effort during application shutdown.
            }
        });
        subscription.close();
        sessionHolder.current().ifPresent(sessionHolder::compareAndClear);
        state.set(DdcRuntimeState.STOPPED);
    }

    /**
     * {@inheritDoc} 中文：同步完成运行时停止后调用回调。 English: Invokes the callback after synchronous runtime shutdown.
     */
    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    /**
     * {@inheritDoc} 中文：返回协调器是否处于运行状态。 English: Returns whether the coordinator is running.
     */
    @Override
    public boolean isRunning() {
        return running.get();
    }

    /**
     * {@inheritDoc} 中文：声明由 Spring 生命周期自动启动。 English: Declares automatic startup by the Spring lifecycle.
     */
    @Override
    public boolean isAutoStartup() {
        return true;
    }

    /**
     * {@inheritDoc} 中文：使用最高阶段以便最后启动并最先停止。 English: Uses the highest phase to start last and stop first.
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    /**
     * 返回当前 DDC 运行时状态。
     * Returns the current DDC runtime state.
     *
     * @return 当前运行时状态; current runtime state
     */
    public DdcRuntimeState state() {
        return state.get();
    }

    /**
     * 返回当前租约会话。
     * Returns the current lease session.
     *
     * @return 当前会话可选值; optional current session
     */
    public Optional<DdcLeaseSession> currentSession() {
        return sessionHolder.current();
    }

    /**
     * 在独立追踪操作中执行一次心跳检查，供调度器和测试调用。
     * Executes one heartbeat check in a dedicated trace operation for scheduler and test invocation.
     */
    void heartbeatOnce() {
        try (DdcTraceSupport.Scope ignored =
                     DdcTraceSupport.openOperation("heartbeat")) {
            doHeartbeatOnce();
        }
    }

    /**
     * 续约当前租约；租约缺失或不匹配时重新注册并拉取配置。
     * Renews the current lease, re-registering and pulling configuration when the lease is missing or mismatched.
     */
    private void doHeartbeatOnce() {
        if (!running.get() || state.get() == DdcRuntimeState.STOPPING) {
            return;
        }
        try {
            DdcLeaseSession session = sessionHolder.current().orElse(null);
            if (session == null) {
                recover();
                return;
            }
            DdcLeaseOperationResult result = instanceService.heartbeat(session);
            if (result.status() == DdcLeaseOperationStatus.NOT_FOUND
                    || result.status() == DdcLeaseOperationStatus.LEASE_MISMATCH) {
                recover();
            } else if (result.renewed()
                    && result.leaseExpireAt() != null) {
                sessionHolder.replace(renewedSession(
                        session,
                        result.leaseExpireAt()
                ));
                state.set(DdcRuntimeState.READY);
            } else {
                state.set(DdcRuntimeState.RECOVERING);
            }
        } catch (RuntimeException exception) {
            state.set(DdcRuntimeState.RECOVERING);
        }
    }

    /**
     * 使用服务端确认的到期时间更新本地租约，不改变租约身份。
     * Updates the local lease with the server-confirmed expiry without changing its identity.
     *
     * @param current 当前租约; current lease
     * @param leaseExpireAt 服务端确认的到期时间; server-confirmed expiry
     * @return 更新后的租约; updated lease
     */
    private DdcLeaseSession renewedSession(
            DdcLeaseSession current,
            Instant leaseExpireAt) {
        return new DdcLeaseSession(
                current.instanceId(),
                current.leaseId(),
                current.role(),
                current.leaseSeconds(),
                current.heartbeatIntervalSeconds(),
                current.registeredAt(),
                leaseExpireAt
        );
    }

    /**
     * 在独立追踪操作中执行一次配置对账，供调度器和测试调用。
     * Executes one configuration reconciliation in a dedicated trace operation for scheduler and test invocation.
     */
    void reconcileOnce() {
        try (DdcTraceSupport.Scope ignored =
                     DdcTraceSupport.openOperation("pull")) {
            doReconcileOnce();
        }
    }

    /**
     * 在运行且启用对账时拉取并应用最新配置快照。
     * Pulls and applies the latest configuration snapshot when running with reconciliation enabled.
     */
    private void doReconcileOnce() {
        if (!running.get()
                || !properties.getConsistency().isReconcileEnabled()
                || state.get() == DdcRuntimeState.STOPPING) {
            return;
        }
        try {
            refreshService.applySnapshots(adminClient.pull());
        } catch (RuntimeException exception) {
            LOGGER.warn("DDC config reconciliation failed", exception);
        }
    }

    /**
     * 注册实例并拉取、应用当前作用域快照。
     * Registers the instance and pulls and applies the current scope snapshot.
     */
    private void initialize() {
        instanceService.register();
        refreshService.applySnapshots(adminClient.pull());
    }

    /**
     * 将状态切换为恢复中，重新初始化成功后恢复为就绪。
     * Switches to recovering, reinitializes, and returns to ready on success.
     */
    private void recover() {
        state.set(DdcRuntimeState.RECOVERING);
        initialize();
        state.set(DdcRuntimeState.READY);
    }

    /**
     * 创建心跳调度器，并在启用时创建配置对账调度器。
     * Creates the heartbeat scheduler and, when enabled, the configuration reconciliation scheduler.
     */
    private void startSchedulers() {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "egon-cola-ddc-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        int interval = properties.getInstance().getHeartbeatIntervalSeconds();
        heartbeatScheduler.scheduleWithFixedDelay(
                this::heartbeatOnce,
                interval,
                interval,
                TimeUnit.SECONDS
        );
        if (!properties.getConsistency().isReconcileEnabled()) {
            return;
        }
        reconcileScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "egon-cola-ddc-config-reconcile");
            thread.setDaemon(true);
            return thread;
        });
        int reconcileInterval = properties.getConsistency().getReconcileIntervalSeconds();
        reconcileScheduler.scheduleWithFixedDelay(
                this::reconcileOnce,
                reconcileInterval,
                reconcileInterval,
                TimeUnit.SECONDS
        );
    }

    /**
     * 请求立即停止指定调度器。
     * Requests immediate shutdown of the specified scheduler.
     *
     * @param currentScheduler 待停止调度器，可为空; scheduler to stop, possibly null
     */
    private void requestShutdown(
            @Nullable ScheduledExecutorService currentScheduler) {
        if (currentScheduler != null) {
            currentScheduler.shutdownNow();
        }
    }

    /**
     * 最多等待一秒使调度器结束，并保留线程中断状态。
     * Waits up to one second for scheduler termination and preserves thread interruption.
     *
     * @param currentScheduler 待等待调度器，可为空; scheduler to await, possibly null
     */
    private void awaitShutdown(
            @Nullable ScheduledExecutorService currentScheduler) {
        if (currentScheduler == null) {
            return;
        }
        try {
            currentScheduler.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 校验 DDC 作用域、实例时序和可选对账间隔。
     * Validates DDC scope, instance timing, and the optional reconciliation interval.
     *
     * @throws DdcException 必填范围为空或时序配置无效时抛出; thrown when required scope values are blank or timing settings are invalid
     */
    private void validateScope() {
        requireText(properties.getBizCode(), "bizCode");
        requireText(properties.getAppCode(), "appCode");
        requireText(properties.getEnv(), "env");
        properties.getInstance().validate();
        if (properties.getConsistency().isReconcileEnabled()
                && properties.getConsistency().getReconcileIntervalSeconds() <= 0) {
            throw new DdcException("reconcileIntervalSeconds must be positive");
        }
    }

    /**
     * 要求指定作用域文本不为空白。
     * Requires a scope text value to be nonblank.
     *
     * @param value     待校验值; value to validate
     * @param fieldName 配置字段名; configuration field name
     * @throws DdcException 值为空白时抛出; thrown when the value is blank
     */
    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new DdcException(fieldName + " is required");
        }
    }
}
