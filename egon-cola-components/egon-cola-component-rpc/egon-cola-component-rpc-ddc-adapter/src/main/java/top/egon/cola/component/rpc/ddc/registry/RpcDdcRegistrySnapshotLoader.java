package top.egon.cola.component.rpc.ddc.registry;

import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.service.registry.DdcRegistrySnapshotLoader;
import top.egon.cola.component.rpc.ddc.client.registry.RpcDdcServiceRegistryClient;

/** 为 Redis Topic 订阅对账提供 Direct RPC 全量快照。 / Direct RPC snapshots for Redis-topic reconciliation. */
public final class RpcDdcRegistrySnapshotLoader implements DdcRegistrySnapshotLoader {

    private final RpcDdcServiceRegistryClient client;

    public RpcDdcRegistrySnapshotLoader(RpcDdcServiceRegistryClient client) {
        this.client = client;
    }

    @Override
    public DdcServiceSnapshot getInstances(DdcServiceKey serviceKey) {
        return client.getInstances(serviceKey);
    }

    @Override
    public DdcServiceCatalogSnapshot getServiceKeys(DdcServiceQuery query) {
        return client.getServiceKeys(query);
    }
}
