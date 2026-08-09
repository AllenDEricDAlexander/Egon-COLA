package top.egon.cola.component.ddc.admin.service.registry;

import org.springframework.stereotype.Service;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceLeaseRequest;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;

/**
 * 收敛 DDC 服务注册与发现用例的应用门面。
 * / Application facade that consolidates DDC service registration and discovery use cases.
 */
@Service
public class DdcRegistryFacade {

    private final DdcServiceRegistryService registryService;

    /**
     * 创建注册中心门面。 / Creates the service-registry facade.
     *
     * @param registryService 注册中心领域服务 / registry domain service
     */
    public DdcRegistryFacade(DdcServiceRegistryService registryService) {
        this.registryService = registryService;
    }

    /** 注册服务实例。 / Registers a service instance. */
    public DdcLeaseSession register(DdcServiceRegistration registration) {
        return registryService.register(registration);
    }

    /** 续期服务实例租约。 / Renews a service-instance lease. */
    public DdcLeaseOperationResult heartbeat(DdcServiceLeaseRequest request) {
        return registryService.heartbeat(request);
    }

    /** 注销服务实例租约。 / Deregisters a service-instance lease. */
    public DdcLeaseOperationResult deregister(DdcServiceLeaseRequest request) {
        return registryService.deregister(request);
    }

    /** 查询服务实例快照。 / Retrieves a service-instance snapshot. */
    public DdcServiceSnapshot getInstances(DdcServiceKey serviceKey) {
        return registryService.getInstances(serviceKey);
    }

    /** 查询服务目录快照。 / Retrieves a service-catalog snapshot. */
    public DdcServiceCatalogSnapshot getServiceKeys(DdcServiceQuery query) {
        return registryService.getServiceKeys(query);
    }
}
