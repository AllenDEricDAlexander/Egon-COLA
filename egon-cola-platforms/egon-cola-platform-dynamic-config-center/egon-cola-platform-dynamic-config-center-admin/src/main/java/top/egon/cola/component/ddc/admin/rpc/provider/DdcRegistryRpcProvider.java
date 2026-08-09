package top.egon.cola.component.ddc.admin.rpc.provider;

import org.springframework.beans.factory.annotation.Autowired;
import top.egon.cola.component.ddc.admin.service.registry.DdcRegistryFacade;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;
import top.egon.cola.component.rpc.ddc.autoconfigure.DdcRpcProperties;
import top.egon.cola.component.rpc.ddc.contract.DdcServiceRegistryRpc;
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
import top.egon.cola.component.rpc.ddc.mapping.DdcCommonProtoMapper;
import top.egon.cola.component.rpc.ddc.mapping.DdcRegistryProtoMapper;

/**
 * 将服务注册 RPC 契约适配到 DDC 注册中心应用门面。
 * / Adapts the service-registry RPC contract to the DDC registry facade.
 */
@EgonRpcProvider
public class DdcRegistryRpcProvider implements DdcServiceRegistryRpc {

    private final DdcRegistryFacade facade;
    private final DdcCommonProtoMapper common;
    private final DdcRegistryProtoMapper mapper;

    /** 使用本地 RPC 消息限额创建 Provider。 / Creates the provider with the local RPC message limit. */
    @Autowired
    public DdcRegistryRpcProvider(
            DdcRegistryFacade facade,
            DdcRpcProperties rpcProperties) {
        this(facade, new DdcCommonProtoMapper(
                rpcProperties.getMaxInboundMessageSize()));
    }

    /** 使用显式映射器创建 Provider。 / Creates the provider with explicit mappers. */
    public DdcRegistryRpcProvider(
            DdcRegistryFacade facade,
            DdcCommonProtoMapper common,
            DdcRegistryProtoMapper mapper) {
        this.facade = facade;
        this.common = common;
        this.mapper = mapper;
    }

    private DdcRegistryRpcProvider(
            DdcRegistryFacade facade,
            DdcCommonProtoMapper common) {
        this(facade, common, new DdcRegistryProtoMapper(common));
    }

    @Override
    public RegisterServiceResponse registerService(RegisterServiceRequest request) {
        return RegisterServiceResponse.newBuilder()
                .setSession(common.toProto(
                        facade.register(mapper.fromRegisterRequest(request))))
                .build();
    }

    @Override
    public HeartbeatServiceResponse heartbeatService(
            HeartbeatServiceRequest request) {
        return HeartbeatServiceResponse.newBuilder()
                .setResult(common.toProto(
                        facade.heartbeat(mapper.fromHeartbeatRequest(request))))
                .build();
    }

    @Override
    public DeregisterServiceResponse deregisterService(
            DeregisterServiceRequest request) {
        return DeregisterServiceResponse.newBuilder()
                .setResult(common.toProto(
                        facade.deregister(mapper.fromDeregisterRequest(request))))
                .build();
    }

    @Override
    public GetServiceInstancesResponse getServiceInstances(
            GetServiceInstancesRequest request) {
        common.checked(request);
        if (!request.hasServiceKey()) {
            throw new IllegalArgumentException("serviceKey is required");
        }
        return mapper.toInstancesResponse(
                facade.getInstances(common.fromProto(request.getServiceKey())));
    }

    @Override
    public GetServicesResponse getServices(GetServicesRequest request) {
        common.checked(request);
        if (!request.hasQuery()) {
            throw new IllegalArgumentException("service query is required");
        }
        return mapper.toServicesResponse(
                facade.getServiceKeys(common.fromProto(request.getQuery())));
    }
}
