package top.egon.cola.component.ddc.registry.subscription;

import org.redisson.api.RedissonClient;
import top.egon.cola.component.ddc.common.DdcKeys;
import top.egon.cola.component.ddc.model.registry.DdcRegistryEvent;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

/**
 * 跟踪一个完整服务键的实例快照。 / Tracks the instance snapshot for one complete service key.
 */
final class DdcInstanceSubscription extends DdcManagedRegistrySubscription {

    private final DdcRegistrySnapshotLoader loader;

    private final DdcServiceKey serviceKey;

    private final Consumer<DdcServiceSnapshot> listener;

    private final Clock clock;

    private volatile DdcServiceSnapshot current;

    DdcInstanceSubscription(DdcRegistrySnapshotLoader loader,
                            DdcServiceKey serviceKey,
                            Consumer<DdcServiceSnapshot> listener,
                            RedissonClient redissonClient,
                            Clock clock,
                            ScheduledExecutorService scheduler,
                            long reconcileIntervalMillis,
                            Consumer<DdcManagedRegistrySubscription> closedCallback) {
        super(
                redissonClient,
                scheduler,
                reconcileIntervalMillis,
                closedCallback
        );
        this.loader = loader;
        this.serviceKey = serviceKey;
        this.listener = listener;
        this.clock = clock;
    }

    void start() {
        start(List.of(
                DdcKeys.registryTopic(
                        serviceKey.bizCode(),
                        serviceKey.env(),
                        serviceKey.appCode(),
                        serviceKey.serviceKind(),
                        serviceKey.protocol()
                )
        ));
    }

    @Override
    protected boolean relevant(DdcRegistryEvent event) {
        return event != null && serviceKey.equals(event.serviceKey());
    }

    @Override
    protected synchronized void refresh() {
        publishIfChanged(loader.getInstances(serviceKey));
    }

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
