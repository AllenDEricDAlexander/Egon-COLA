package top.egon.cola.component.ddc.service.registry;

import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;

/**
 * 为注册中心订阅提供只读全量快照。 / Provides read-only full snapshots to registry subscriptions.
 */
public interface DdcRegistrySnapshotLoader {

    /**
     * 加载一个完整服务键的实例快照。 / Loads the instance snapshot for one complete service key.
     *
     * @param serviceKey 服务键 / service key
     * @return 服务实例快照 / service-instance snapshot
     */
    DdcServiceSnapshot getInstances(DdcServiceKey serviceKey);

    /**
     * 加载匹配条件的服务目录快照。 / Loads the service catalog matching the query.
     *
     * @param query 服务目录查询 / service-catalog query
     * @return 服务目录快照 / service-catalog snapshot
     */
    DdcServiceCatalogSnapshot getServiceKeys(DdcServiceQuery query);
}
