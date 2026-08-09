package top.egon.cola.component.ddc.admin.rpc.provider;

import org.springframework.beans.factory.annotation.Autowired;
import top.egon.cola.component.ddc.admin.service.config.DdcConfigFacade;
import top.egon.cola.component.ddc.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;
import top.egon.cola.component.rpc.ddc.autoconfigure.DdcRpcProperties;
import top.egon.cola.component.rpc.ddc.contract.DdcConfigRuntimeRpc;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.AcknowledgePublishRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.AcknowledgePublishResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.HeartbeatConfigClientRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.HeartbeatConfigClientResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.OfflineConfigClientRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.OfflineConfigClientResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.PullConfigRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.PullConfigResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.RegisterConfigClientRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.RegisterConfigClientResponse;
import top.egon.cola.component.rpc.ddc.mapping.DdcCommonProtoMapper;
import top.egon.cola.component.rpc.ddc.mapping.DdcConfigProtoMapper;

/**
 * 将配置运行时 RPC 契约适配到 DDC 配置应用门面。
 * / Adapts the configuration-runtime RPC contract to the DDC application facade.
 */
@EgonRpcProvider
public class DdcConfigRpcProvider implements DdcConfigRuntimeRpc {

    private final DdcConfigFacade facade;
    private final DdcCommonProtoMapper common;
    private final DdcConfigProtoMapper mapper;

    /**
     * 使用本地 DDC 和 RPC 限额创建 Provider。
     * / Creates the provider with the local DDC and RPC size limits.
     */
    @Autowired
    public DdcConfigRpcProvider(
            DdcConfigFacade facade,
            DdcProperties ddcProperties,
            DdcRpcProperties rpcProperties) {
        this(
                facade,
                new DdcCommonProtoMapper(
                        rpcProperties.getMaxInboundMessageSize()),
                ddcProperties.getMaxConfigBytes()
        );
    }

    /**
     * 使用显式映射器创建 Provider，供精确边界测试使用。
     * / Creates the provider with explicit mappers for precise boundary tests.
     */
    public DdcConfigRpcProvider(
            DdcConfigFacade facade,
            DdcCommonProtoMapper common,
            DdcConfigProtoMapper mapper) {
        this.facade = facade;
        this.common = common;
        this.mapper = mapper;
    }

    private DdcConfigRpcProvider(
            DdcConfigFacade facade,
            DdcCommonProtoMapper common,
            long maxConfigBytes) {
        this(facade, common, new DdcConfigProtoMapper(common, maxConfigBytes));
    }

    @Override
    public RegisterConfigClientResponse registerConfigClient(
            RegisterConfigClientRequest request) {
        return RegisterConfigClientResponse.newBuilder()
                .setSession(common.toProto(
                        facade.register(mapper.fromRegisterRequest(request))))
                .build();
    }

    @Override
    public HeartbeatConfigClientResponse heartbeatConfigClient(
            HeartbeatConfigClientRequest request) {
        return HeartbeatConfigClientResponse.newBuilder()
                .setResult(common.toProto(
                        facade.heartbeat(mapper.fromHeartbeatRequest(request))))
                .build();
    }

    @Override
    public OfflineConfigClientResponse offlineConfigClient(
            OfflineConfigClientRequest request) {
        return OfflineConfigClientResponse.newBuilder()
                .setResult(common.toProto(
                        facade.offline(mapper.fromOfflineRequest(request))))
                .build();
    }

    @Override
    public PullConfigResponse pullConfig(PullConfigRequest request) {
        common.checked(request);
        if (!request.hasScope()) {
            throw new IllegalArgumentException("scope is required");
        }
        return mapper.toPullResponse(facade.pull(
                request.getScope().getBizCode(),
                request.getScope().getEnv(),
                request.getScope().getAppCode()
        ));
    }

    @Override
    public AcknowledgePublishResponse acknowledgePublish(
            AcknowledgePublishRequest request) {
        facade.ack(mapper.fromAcknowledgeRequest(request));
        return AcknowledgePublishResponse.getDefaultInstance();
    }
}
