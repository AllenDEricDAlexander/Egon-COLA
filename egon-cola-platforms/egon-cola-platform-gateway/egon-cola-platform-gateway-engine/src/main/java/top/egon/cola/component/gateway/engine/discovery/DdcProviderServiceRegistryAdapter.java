package top.egon.cola.component.gateway.engine.discovery;

import top.egon.cola.component.ddc.model.registry.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceInstance;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.api.registry.DdcRegistrySubscription;
import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.gateway.core.provider.ProviderCatalogSnapshot;
import top.egon.cola.component.gateway.core.provider.ProviderHealthState;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderQuery;
import top.egon.cola.component.gateway.core.provider.ProviderRegistryState;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.provider.ProviderServiceRegistry;
import top.egon.cola.component.gateway.core.provider.ProviderServiceSnapshot;
import top.egon.cola.component.gateway.core.provider.ProviderSubscription;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DdcProviderServiceRegistryAdapter
        implements ProviderServiceRegistry {

    private final DdcServiceRegistryClient delegate;

    public DdcProviderServiceRegistryAdapter(
            DdcServiceRegistryClient delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public ProviderCatalogSnapshot getServiceKeys(ProviderQuery query) {
        List<ProviderServiceKey> keys = new ArrayList<>();
        long revision = 0;
        java.time.Instant observedAt = java.time.Instant.EPOCH;
        for (DdcServiceQuery ddcQuery : queries(query)) {
            DdcServiceCatalogSnapshot snapshot =
                    delegate.getServiceKeys(ddcQuery);
            keys.addAll(snapshot.serviceKeys().stream()
                    .map(key -> serviceKey(key, query.namespace()))
                    .toList());
            revision = Math.max(revision, snapshot.revision());
            if (snapshot.observedAt().isAfter(observedAt)) {
                observedAt = snapshot.observedAt();
            }
        }
        return new ProviderCatalogSnapshot(revision, observedAt, keys);
    }

    @Override
    public ProviderServiceSnapshot getInstances(ProviderServiceKey key) {
        return snapshot(delegate.getInstances(ddcKey(key)), key);
    }

    @Override
    public ProviderSubscription subscribeServices(
            ProviderQuery query,
            ProviderCatalogListener listener) {
        List<DdcRegistrySubscription> subscriptions = queries(query)
                .stream()
                .map(ddcQuery -> delegate.subscribeServices(
                        ddcQuery,
                        ignored -> listener.onSnapshot(getServiceKeys(query))
                ))
                .toList();
        return composite(subscriptions);
    }

    @Override
    public ProviderSubscription subscribe(
            ProviderServiceKey key,
            ProviderSnapshotListener listener) {
        DdcRegistrySubscription subscription = delegate.subscribe(
                ddcKey(key),
                value -> listener.onSnapshot(snapshot(value, key))
        );
        return composite(List.of(subscription));
    }

    private List<DdcServiceQuery> queries(ProviderQuery query) {
        if (query.protocolType() == ProviderProtocolType.RPC) {
            return List.of(query(query, DdcServiceKind.RPC_PROVIDER, "grpc"));
        }
        if (query.protocolType() == ProviderProtocolType.HTTP) {
            return List.of(
                    query(query, DdcServiceKind.HTTP_PROVIDER, "http"),
                    query(query, DdcServiceKind.HTTP_PROVIDER, "https")
            );
        }
        return List.of(
                query(query, DdcServiceKind.HTTP_PROVIDER, "http"),
                query(query, DdcServiceKind.HTTP_PROVIDER, "https"),
                query(query, DdcServiceKind.RPC_PROVIDER, "grpc")
        );
    }

    private DdcServiceQuery query(
            ProviderQuery query,
            DdcServiceKind kind,
            String protocol) {
        return new DdcServiceQuery(
                query.bizCode(),
                query.env(),
                query.appCode(),
                kind,
                protocol,
                null,
                null,
                null
        );
    }

    private ProviderServiceSnapshot snapshot(
            DdcServiceSnapshot value,
            ProviderServiceKey key) {
        Instant now = Instant.now();
        return new ProviderServiceSnapshot(
                key,
                value.revision(),
                value.observedAt(),
                value.instances().stream()
                        .map(instance -> instance(key, instance, now))
                        .toList()
        );
    }

    private ProviderInstance instance(
            ProviderServiceKey key,
            DdcServiceInstance value,
            Instant now) {
        return new ProviderInstance(
                key,
                value.instanceId(),
                value.leaseId(),
                value.host(),
                value.port(),
                value.secure(),
                value.metadata(),
                value.leaseExpireAt(),
                value.normalizedStatus().isAvailable(
                        now,
                        value.leaseExpireAt()
                )
                        ? ProviderRegistryState.REGISTERED
                        : ProviderRegistryState.EXPIRED,
                ProviderHealthState.UNKNOWN,
                ProviderHealthState.UNKNOWN
        );
    }

    private ProviderServiceKey serviceKey(
            DdcServiceKey key,
            String namespace) {
        return new ProviderServiceKey(
                key.bizCode(),
                key.appCode(),
                key.env(),
                namespace,
                key.serviceKind() == DdcServiceKind.HTTP_PROVIDER
                        ? ProviderProtocolType.HTTP
                        : ProviderProtocolType.RPC,
                key.serviceName(),
                key.group(),
                key.version(),
                key.protocol()
        );
    }

    private DdcServiceKey ddcKey(ProviderServiceKey key) {
        return new DdcServiceKey(
                key.bizCode(),
                key.env(),
                key.appCode(),
                key.protocolType() == ProviderProtocolType.HTTP
                        ? DdcServiceKind.HTTP_PROVIDER
                        : DdcServiceKind.RPC_PROVIDER,
                key.serviceName(),
                key.group(),
                key.version(),
                key.transport()
        );
    }

    private ProviderSubscription composite(
            List<DdcRegistrySubscription> subscriptions) {
        return new ProviderSubscription() {
            private volatile boolean active = true;

            @Override
            public boolean active() {
                return active;
            }

            @Override
            public void close() {
                if (active) {
                    active = false;
                    subscriptions.forEach(DdcRegistrySubscription::close);
                }
            }
        };
    }
}
