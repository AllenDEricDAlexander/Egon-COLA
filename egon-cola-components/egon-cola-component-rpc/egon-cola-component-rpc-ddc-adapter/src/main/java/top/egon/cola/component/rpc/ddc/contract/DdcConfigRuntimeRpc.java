package top.egon.cola.component.rpc.ddc.contract;

import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.AcknowledgePublishRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.AcknowledgePublishResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcConfigRuntimeServiceGrpc;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.HeartbeatConfigClientRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.HeartbeatConfigClientResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.OfflineConfigClientRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.OfflineConfigClientResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.PullConfigRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.PullConfigResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.RegisterConfigClientRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.RegisterConfigClientResponse;

/**
 * DDC 配置运行时 RPC 门面契约。
 * / RPC facade contract for the DDC configuration runtime.
 */
@EgonRpcService(
        grpcClass = DdcConfigRuntimeServiceGrpc.class,
        group = "ddc",
        version = "1.0.0"
)
public interface DdcConfigRuntimeRpc {

    /**
     * 注册配置客户端租约。 / Registers a configuration client lease.
     *
     * @param request 注册请求 / registration request
     * @return 注册结果 / registration result
     */
    @EgonRpcMethod(name = "RegisterConfigClient")
    RegisterConfigClientResponse registerConfigClient(
            RegisterConfigClientRequest request);

    /**
     * 续期配置客户端租约。 / Renews a configuration client lease.
     *
     * @param request 心跳请求 / heartbeat request
     * @return 续期结果 / renewal result
     */
    @EgonRpcMethod(name = "HeartbeatConfigClient", idempotent = true)
    HeartbeatConfigClientResponse heartbeatConfigClient(
            HeartbeatConfigClientRequest request);

    /**
     * 注销配置客户端租约。 / Takes a configuration client lease offline.
     *
     * @param request 注销请求 / offline request
     * @return 注销结果 / offline result
     */
    @EgonRpcMethod(name = "OfflineConfigClient", idempotent = true)
    OfflineConfigClientResponse offlineConfigClient(
            OfflineConfigClientRequest request);

    /**
     * 拉取作用域内的配置。 / Pulls configurations within a scope.
     *
     * @param request 拉取请求 / pull request
     * @return 配置集合 / configurations
     */
    @EgonRpcMethod(name = "PullConfig", idempotent = true)
    PullConfigResponse pullConfig(PullConfigRequest request);

    /**
     * 确认配置发布结果。 / Acknowledges a configuration publication result.
     *
     * @param request 确认请求 / acknowledgement request
     * @return 空确认响应 / empty acknowledgement response
     */
    @EgonRpcMethod(name = "AcknowledgePublish", idempotent = true)
    AcknowledgePublishResponse acknowledgePublish(
            AcknowledgePublishRequest request);
}
