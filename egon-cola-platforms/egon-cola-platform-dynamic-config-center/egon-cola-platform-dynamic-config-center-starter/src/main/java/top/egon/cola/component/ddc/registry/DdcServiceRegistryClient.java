package top.egon.cola.component.ddc.registry;

import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;

import java.util.function.Consumer;

/**
 * DDC 服务注册、租约维护、发现与订阅客户端契约。
 * / Client contract for DDC service registration, lease maintenance, discovery, and subscription.
 */
public interface DdcServiceRegistryClient {

    /**
     * 注册服务实例并获取租约会话。
     * / Registers a service instance and obtains a lease session.
     *
     * @param registration 已校验的服务注册信息 / validated service registration
     * @return Admin 签发的租约会话 / lease session issued by Admin
     */
    DdcLeaseSession register(DdcServiceRegistration registration);

    /**
     * 为指定实例租约续期。
     * / Renews the lease for the specified instance.
     *
     * @param instanceId 实例标识 / instance identifier
     * @param leaseId    租约标识 / lease identifier
     * @return 租约操作结果 / lease operation result
     */
    DdcLeaseOperationResult heartbeat(String instanceId, String leaseId);

    /**
     * 注销指定实例租约。
     * / Deregisters the specified instance lease.
     *
     * @param instanceId 实例标识 / instance identifier
     * @param leaseId    租约标识 / lease identifier
     * @return 租约操作结果 / lease operation result
     */
    DdcLeaseOperationResult deregister(String instanceId, String leaseId);

    /**
     * 获取指定服务键的实例快照。
     * / Gets the instance snapshot for a service key.
     *
     * @param serviceKey 服务键 / service key
     * @return 当前服务实例快照 / current service instance snapshot
     */
    DdcServiceSnapshot getInstances(DdcServiceKey serviceKey);

    /**
     * 订阅指定服务键的实例快照变化。
     * / Subscribes to instance snapshot changes for a service key.
     *
     * @param serviceKey 服务键 / service key
     * @param listener   接收初始及后续快照的监听器 / listener receiving the initial and subsequent snapshots
     * @return 用于取消订阅的句柄 / handle used to cancel the subscription
     */
    DdcRegistrySubscription subscribe(
            DdcServiceKey serviceKey,
            Consumer<DdcServiceSnapshot> listener
    );

    /**
     * 查询服务目录快照。
     * / Queries a service catalog snapshot.
     *
     * @param query 服务目录筛选条件 / service catalog filter
     * @return 当前服务目录快照 / current service catalog snapshot
     */
    DdcServiceCatalogSnapshot getServiceKeys(DdcServiceQuery query);

    /**
     * 订阅匹配查询条件的服务目录变化。
     * / Subscribes to service catalog changes matching a query.
     *
     * @param query    包含精确主题范围的服务查询 / service query containing an exact topic scope
     * @param listener 接收初始及后续目录快照的监听器 / listener receiving the initial and subsequent catalog snapshots
     * @return 用于取消订阅的句柄 / handle used to cancel the subscription
     */
    DdcRegistrySubscription subscribeServices(
            DdcServiceQuery query,
            Consumer<DdcServiceCatalogSnapshot> listener
    );
}
