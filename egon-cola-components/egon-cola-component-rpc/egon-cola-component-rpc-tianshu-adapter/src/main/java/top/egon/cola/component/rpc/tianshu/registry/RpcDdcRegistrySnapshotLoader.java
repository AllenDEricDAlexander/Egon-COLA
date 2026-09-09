package top.egon.cola.component.rpc.tianshu.registry;

import top.egon.cola.component.tianshu.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.tianshu.model.registry.DdcServiceKey;
import top.egon.cola.component.tianshu.model.registry.DdcServiceQuery;
import top.egon.cola.component.tianshu.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.tianshu.service.registry.DdcRegistrySnapshotLoader;
import top.egon.cola.component.rpc.tianshu.client.registry.RpcDdcServiceRegistryClient;

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
