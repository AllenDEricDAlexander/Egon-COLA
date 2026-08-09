package top.egon.cola.component.ddc.registry.subscription;

import org.redisson.api.RedissonClient;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.api.registry.DdcRegistrySubscription;
import top.egon.cola.component.ddc.service.registry.DdcRegistrySnapshotLoader;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 创建并持有实例与目录订阅，共享 Redis 连接和串行对账调度器。 /
 * Creates and owns instance and catalog subscriptions sharing a Redis connection and serial reconciliation scheduler.
 */
public final class DdcRegistrySubscriptionCoordinator implements AutoCloseable {

    private final DdcRegistrySnapshotLoader loader;

    private final RedissonClient redissonClient;

    private final Clock clock;

    private final ScheduledExecutorService scheduler;

    private final long reconcileIntervalMillis;

    private final Set<DdcManagedRegistrySubscription> subscriptions =
            ConcurrentHashMap.newKeySet();

    /**
     * 使用 UTC 时钟和单线程守护调度器创建协调器。 /
     * Creates the coordinator with a UTC clock and single daemon scheduler.
     *
     * @param loader                   只读快照加载器 / read-only snapshot loader
     * @param redissonClient           Redis Topic 客户端 / Redis topic client
     * @param reconcileIntervalSeconds 定时对账间隔秒数 / reconciliation interval in seconds
     */
    public DdcRegistrySubscriptionCoordinator(
            DdcRegistrySnapshotLoader loader,
            RedissonClient redissonClient,
            int reconcileIntervalSeconds) {
        this(
                loader,
                redissonClient,
                Clock.systemUTC(),
                Executors.newSingleThreadScheduledExecutor(runnable -> {
                    Thread thread = new Thread(
                            runnable,
                            "egon-cola-ddc-registry"
                    );
                    thread.setDaemon(true);
                    return thread;
                }),
                TimeUnit.SECONDS.toMillis(reconcileIntervalSeconds)
        );
    }

    DdcRegistrySubscriptionCoordinator(
            DdcRegistrySnapshotLoader loader,
            RedissonClient redissonClient,
            Clock clock,
            ScheduledExecutorService scheduler,
            long reconcileIntervalMillis) {
        if (reconcileIntervalMillis <= 0) {
            throw new IllegalArgumentException(
                    "reconcile interval must be positive"
            );
        }
        this.loader = Objects.requireNonNull(loader, "loader");
        this.redissonClient = Objects.requireNonNull(
                redissonClient,
                "redissonClient"
        );
        this.clock = Objects.requireNonNull(clock, "clock");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.reconcileIntervalMillis = reconcileIntervalMillis;
    }

    /**
     * 创建并启动一个服务键的实例快照订阅。 / Creates and starts an instance snapshot subscription for one service key.
     *
     * @param serviceKey 服务键 / service key
     * @param listener   实例快照监听器 / instance-snapshot listener
     * @return 已启动订阅 / started subscription
     */
    public DdcRegistrySubscription subscribe(
            DdcServiceKey serviceKey,
            Consumer<DdcServiceSnapshot> listener) {
        DdcInstanceSubscription subscription = new DdcInstanceSubscription(
                loader,
                Objects.requireNonNull(serviceKey, "serviceKey"),
                Objects.requireNonNull(listener, "listener"),
                redissonClient,
                clock,
                scheduler,
                reconcileIntervalMillis,
                subscriptions::remove
        );
        return start(subscription, subscription::start);
    }

    /**
     * 创建并启动精确主题范围的服务目录订阅。 / Creates and starts a service-catalog subscription for an exact topic scope.
     *
     * @param query    服务目录查询 / service-catalog query
     * @param listener 服务目录监听器 / service-catalog listener
     * @return 已启动订阅 / started subscription
     */
    public DdcRegistrySubscription subscribeServices(
            DdcServiceQuery query,
            Consumer<DdcServiceCatalogSnapshot> listener) {
        DdcCatalogSubscription subscription = new DdcCatalogSubscription(
                loader,
                Objects.requireNonNull(query, "query"),
                Objects.requireNonNull(listener, "listener"),
                redissonClient,
                scheduler,
                reconcileIntervalMillis,
                subscriptions::remove
        );
        return start(subscription, subscription::start);
    }

    private DdcRegistrySubscription start(
            DdcManagedRegistrySubscription subscription,
            Runnable starter) {
        subscriptions.add(subscription);
        try {
            starter.run();
            return subscription;
        } catch (RuntimeException exception) {
            subscription.close();
            throw exception;
        }
    }

    /**
     * 关闭全部订阅并立即停止对账调度器。 / Closes all subscriptions and immediately stops the reconciliation scheduler.
     */
    @Override
    public void close() {
        List.copyOf(subscriptions)
                .forEach(DdcManagedRegistrySubscription::close);
        scheduler.shutdownNow();
    }
}
