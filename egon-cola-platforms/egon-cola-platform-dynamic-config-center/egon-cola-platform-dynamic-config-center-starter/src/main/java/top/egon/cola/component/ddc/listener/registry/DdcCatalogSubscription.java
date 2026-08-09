package top.egon.cola.component.ddc.listener.registry;

import top.egon.cola.component.ddc.service.registry.DdcRegistrySnapshotLoader;

import org.redisson.api.RedissonClient;
import org.springframework.lang.Nullable;
import top.egon.cola.component.ddc.redis.DdcRedisKeys;
import top.egon.cola.component.ddc.model.registry.DdcRegistryEvent;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

/**
 * 跟踪一个精确注册主题范围内的服务目录。 / Tracks the service catalog within one exact registry topic scope.
 */
final class DdcCatalogSubscription extends DdcManagedRegistrySubscription {

    private final DdcRegistrySnapshotLoader loader;

    private final DdcServiceQuery query;

    private final Consumer<DdcServiceCatalogSnapshot> listener;

    private volatile DdcServiceCatalogSnapshot current;

    DdcCatalogSubscription(DdcRegistrySnapshotLoader loader,
                           DdcServiceQuery query,
                           Consumer<DdcServiceCatalogSnapshot> listener,
                           RedissonClient redissonClient,
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
        this.query = query;
        this.listener = listener;
    }

    void start() {
        if (!query.hasExactCatalogScope()) {
            throw new IllegalArgumentException(
                    "service catalog subscription requires bizCode, env, appCode, serviceKind and protocol"
            );
        }
        start(List.of(
                DdcRedisKeys.registryTopic(
                        query.bizCode(),
                        query.env(),
                        query.appCode(),
                        query.serviceKind(),
                        query.protocol()
                )
        ));
    }

    @Override
    protected boolean relevant(@Nullable DdcRegistryEvent event) {
        return event != null
                && event.serviceKey() != null
                && query.matches(event.serviceKey());
    }

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

    @Override
    protected synchronized void expireLocal() {
        // Service catalog membership cannot be inferred safely without Admin.
    }
}
