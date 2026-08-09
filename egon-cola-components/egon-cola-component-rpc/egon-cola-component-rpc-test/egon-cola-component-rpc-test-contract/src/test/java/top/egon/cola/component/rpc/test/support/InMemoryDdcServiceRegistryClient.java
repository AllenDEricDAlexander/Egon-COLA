package top.egon.cola.component.rpc.test.support;

import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.api.registry.DdcRegistrySubscription;
import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class InMemoryDdcServiceRegistryClient
        implements DdcServiceRegistryClient {

    private final InMemoryDdcRegistryBackend backend;

    private final CopyOnWriteArrayList<DdcServiceKey> subscribedKeys =
            new CopyOnWriteArrayList<>();

    public InMemoryDdcServiceRegistryClient(
            InMemoryDdcRegistryBackend backend) {
        this.backend = backend;
    }

    @Override
    public DdcLeaseSession register(DdcServiceRegistration registration) {
        return backend.register(registration);
    }

    @Override
    public DdcLeaseOperationResult heartbeat(
            String instanceId,
            String leaseId) {
        return backend.heartbeat(instanceId, leaseId);
    }

    @Override
    public DdcLeaseOperationResult deregister(
            String instanceId,
            String leaseId) {
        return backend.deregister(instanceId, leaseId);
    }

    @Override
    public DdcServiceSnapshot getInstances(DdcServiceKey serviceKey) {
        return backend.instances(serviceKey);
    }

    @Override
    public DdcRegistrySubscription subscribe(
            DdcServiceKey serviceKey,
            Consumer<DdcServiceSnapshot> listener) {
        subscribedKeys.add(serviceKey);
        DdcRegistrySubscription subscription =
                backend.subscribe(serviceKey, listener);
        return () -> {
            subscribedKeys.remove(serviceKey);
            subscription.close();
        };
    }

    @Override
    public DdcServiceCatalogSnapshot getServiceKeys(
            DdcServiceQuery query) {
        return backend.catalog(query);
    }

    @Override
    public DdcRegistrySubscription subscribeServices(
            DdcServiceQuery query,
            Consumer<DdcServiceCatalogSnapshot> listener) {
        return backend.subscribeServices(query, listener);
    }

    public List<DdcServiceKey> subscribedKeys() {
        return List.copyOf(subscribedKeys);
    }
}
