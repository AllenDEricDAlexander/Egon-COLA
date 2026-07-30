package top.egon.cola.component.ddc.registry;

import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;

import java.util.function.Consumer;

public interface DdcServiceRegistryClient {

    DdcLeaseSession register(DdcServiceRegistration registration);

    DdcLeaseOperationResult heartbeat(String instanceId, String leaseId);

    DdcLeaseOperationResult deregister(String instanceId, String leaseId);

    DdcServiceSnapshot getInstances(DdcServiceKey serviceKey);

    DdcRegistrySubscription subscribe(
            DdcServiceKey serviceKey,
            Consumer<DdcServiceSnapshot> listener
    );

    DdcServiceCatalogSnapshot getServiceKeys(DdcServiceQuery query);

    DdcRegistrySubscription subscribeServices(
            DdcServiceQuery query,
            Consumer<DdcServiceCatalogSnapshot> listener
    );
}
