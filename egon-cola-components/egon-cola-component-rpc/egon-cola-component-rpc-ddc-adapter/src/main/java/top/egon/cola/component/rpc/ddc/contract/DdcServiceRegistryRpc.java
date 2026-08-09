package top.egon.cola.component.rpc.ddc.contract;

import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcServiceRegistryServiceGrpc;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DeregisterServiceRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DeregisterServiceResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetServiceInstancesRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetServiceInstancesResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetServicesRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetServicesResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.HeartbeatServiceRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.HeartbeatServiceResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.RegisterServiceRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.RegisterServiceResponse;

/**
 * DDC 服务注册 RPC 门面契约。
 * / RPC facade contract for the DDC service registry.
 */
@EgonRpcService(
        grpcClass = DdcServiceRegistryServiceGrpc.class,
        group = "ddc",
        version = "1.0.0"
)
public interface DdcServiceRegistryRpc {

    /**
     * 注册服务实例租约。 / Registers a service instance lease.
     *
     * @param request 注册请求 / registration request
     * @return 注册结果 / registration result
     */
    @EgonRpcMethod(name = "RegisterService")
    RegisterServiceResponse registerService(RegisterServiceRequest request);

    /**
     * 续期服务实例租约。 / Renews a service instance lease.
     *
     * @param request 心跳请求 / heartbeat request
     * @return 续期结果 / renewal result
     */
    @EgonRpcMethod(name = "HeartbeatService", idempotent = true)
    HeartbeatServiceResponse heartbeatService(HeartbeatServiceRequest request);

    /**
     * 注销服务实例租约。 / Deregisters a service instance lease.
     *
     * @param request 注销请求 / deregistration request
     * @return 注销结果 / deregistration result
     */
    @EgonRpcMethod(name = "DeregisterService", idempotent = true)
    DeregisterServiceResponse deregisterService(
            DeregisterServiceRequest request);

    /**
     * 查询服务实例快照。 / Gets a service instance snapshot.
     *
     * @param request 服务键请求 / service key request
     * @return 实例快照 / instance snapshot
     */
    @EgonRpcMethod(name = "GetServiceInstances", idempotent = true)
    GetServiceInstancesResponse getServiceInstances(
            GetServiceInstancesRequest request);

    /**
     * 查询服务目录快照。 / Gets a service catalog snapshot.
     *
     * @param request 查询条件 / query
     * @return 服务目录 / service catalog
     */
    @EgonRpcMethod(name = "GetServices", idempotent = true)
    GetServicesResponse getServices(GetServicesRequest request);
}
