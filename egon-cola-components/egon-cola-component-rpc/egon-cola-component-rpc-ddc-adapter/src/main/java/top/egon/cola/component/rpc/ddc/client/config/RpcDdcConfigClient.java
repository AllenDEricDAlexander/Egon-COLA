package top.egon.cola.component.rpc.ddc.client.config;

import top.egon.cola.component.ddc.api.client.DdcConfigClient;
import top.egon.cola.component.ddc.model.config.DdcAckRequest;
import top.egon.cola.component.ddc.model.config.DdcConfigValue;
import top.egon.cola.component.ddc.model.config.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.config.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.rpc.ddc.contract.DdcConfigRuntimeRpc;
import top.egon.cola.component.rpc.ddc.mapping.DdcCommonProtoMapper;
import top.egon.cola.component.rpc.ddc.mapping.DdcConfigProtoMapper;
import top.egon.cola.component.rpc.ddc.mapping.DdcRpcStatusExceptionMapper;
import top.egon.cola.component.rpc.ddc.security.DdcRpcOperation;

import java.util.List;

/** DDC 配置运行时 Port 的 Direct RPC 适配器。 / Direct RPC adapter for the DDC config-runtime Port. */
public final class RpcDdcConfigClient implements DdcConfigClient {

    private final DdcConfigRuntimeRpc rpc;
    private final DdcConfigProtoMapper mapper;
    private final DdcCommonProtoMapper common;
    private final DdcRpcStatusExceptionMapper errors;
    private final String bizCode;
    private final String env;
    private final String appCode;

    public RpcDdcConfigClient(
            DdcConfigRuntimeRpc rpc,
            DdcConfigProtoMapper mapper,
            DdcCommonProtoMapper common,
            DdcRpcStatusExceptionMapper errors,
            String bizCode,
            String env,
            String appCode) {
        this.rpc = rpc;
        this.mapper = mapper;
        this.common = common;
        this.errors = errors;
        this.bizCode = bizCode;
        this.env = env;
        this.appCode = appCode;
    }

    @Override
    public DdcLeaseSession register(DdcInstanceRegisterRequest request) {
        return invoke(DdcRpcOperation.SDK_REGISTER, () -> {
            var response = rpc.registerConfigClient(mapper.toRegisterRequest(request));
            if (!response.hasSession()) throw new IllegalArgumentException("session is required");
            return common.fromProto(response.getSession());
        });
    }

    @Override
    public DdcLeaseOperationResult heartbeat(DdcHeartbeatRequest request) {
        return invoke(DdcRpcOperation.SDK_HEARTBEAT, () -> {
            var response = rpc.heartbeatConfigClient(mapper.toHeartbeatRequest(request));
            if (!response.hasResult()) throw new IllegalArgumentException("result is required");
            return common.fromProto(response.getResult());
        });
    }

    @Override
    public DdcLeaseOperationResult offline(DdcHeartbeatRequest request) {
        return invoke(DdcRpcOperation.SDK_OFFLINE, () -> {
            var response = rpc.offlineConfigClient(mapper.toOfflineRequest(request));
            if (!response.hasResult()) throw new IllegalArgumentException("result is required");
            return common.fromProto(response.getResult());
        });
    }

    @Override
    public List<DdcConfigValue> pull() {
        return invoke(DdcRpcOperation.CONFIG_PULL, () -> mapper.fromPullResponse(
                rpc.pullConfig(mapper.toPullRequest(bizCode, env, appCode))));
    }

    @Override
    public void ack(DdcAckRequest request) {
        invoke(DdcRpcOperation.PUBLISH_ACK, () -> {
            rpc.acknowledgePublish(mapper.toAcknowledgeRequest(request));
            return null;
        });
    }

    private <T> T invoke(DdcRpcOperation operation, Invocation<T> invocation) {
        try {
            return invocation.call();
        } catch (RuntimeException failure) {
            throw errors.restore(failure, operation);
        }
    }

    @FunctionalInterface
    private interface Invocation<T> { T call(); }
}
