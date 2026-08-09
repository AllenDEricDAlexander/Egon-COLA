package top.egon.cola.component.rpc.ddc.client.management;

import top.egon.cola.component.ddc.api.client.DdcManagementClient;
import top.egon.cola.component.ddc.model.management.*;
import top.egon.cola.component.rpc.ddc.contract.DdcManagementRpc;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetPublishTaskRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.RetryPublishTaskRequest;
import top.egon.cola.component.rpc.ddc.mapping.DdcManagementProtoMapper;
import top.egon.cola.component.rpc.ddc.mapping.DdcRpcStatusExceptionMapper;
import top.egon.cola.component.rpc.ddc.security.DdcRpcOperation;

import java.util.List;
import java.util.Optional;

/** DDC 管理 Port 的按需 Direct RPC 适配器。 / On-demand Direct RPC adapter for the DDC management Port. */
public final class RpcDdcManagementClient implements DdcManagementClient {

    private final DdcManagementRpc rpc;
    private final DdcManagementProtoMapper mapper;
    private final DdcRpcStatusExceptionMapper errors;

    public RpcDdcManagementClient(
            DdcManagementRpc rpc,
            DdcManagementProtoMapper mapper,
            DdcRpcStatusExceptionMapper errors) {
        this.rpc = rpc;
        this.mapper = mapper;
        this.errors = errors;
    }

    @Override
    public Optional<DdcManagementConfig> findConfig(DdcManagementConfigQuery query) {
        return invoke(DdcRpcOperation.MANAGEMENT_CONFIG_READ, () -> {
            var response = rpc.findConfig(mapper.toFindRequest(query));
            return response.getFound() && response.hasConfig()
                    ? Optional.of(mapper.fromConfig(response.getConfig())) : Optional.empty();
        });
    }

    @Override
    public DdcManagementConfig upsert(DdcManagementConfigUpsertRequest request) {
        return invoke(DdcRpcOperation.MANAGEMENT_CONFIG_WRITE, () -> {
            var response = rpc.upsertConfig(mapper.toUpsertRequest(request));
            if (!response.hasConfig()) throw new IllegalArgumentException("config is required");
            return mapper.fromConfig(response.getConfig());
        });
    }

    @Override
    public void delete(DdcManagementConfigDeleteRequest request) {
        invoke(DdcRpcOperation.MANAGEMENT_CONFIG_WRITE, () -> {
            rpc.deleteConfig(mapper.toDeleteRequest(request)); return null;
        });
    }

    @Override
    public DdcManagementPublishResult publish(DdcManagementPublishRequest request) {
        return invoke(DdcRpcOperation.MANAGEMENT_PUBLISH, () -> {
            var response = rpc.publishConfig(mapper.toPublishRequest(request));
            if (!response.hasResult()) throw new IllegalArgumentException("result is required");
            return mapper.fromPublishResult(response.getResult());
        });
    }

    @Override
    public DdcManagementPublishTask getPublishTask(String changeId) {
        return invoke(DdcRpcOperation.MANAGEMENT_TASK_READ, () -> {
            var response = rpc.getPublishTask(GetPublishTaskRequest.newBuilder()
                    .setChangeId(required(changeId, "changeId")).build());
            if (!response.getFound() || !response.hasTask()) {
                throw new IllegalArgumentException("publish task is required");
            }
            return mapper.fromPublishTask(response.getTask());
        });
    }

    @Override
    public DdcManagementPublishResult retry(String changeId) {
        return invoke(DdcRpcOperation.MANAGEMENT_TASK_RETRY, () -> {
            var response = rpc.retryPublishTask(RetryPublishTaskRequest.newBuilder()
                    .setChangeId(required(changeId, "changeId"))
                    .setRequestedOperator("rpc-client").build());
            if (!response.hasResult()) throw new IllegalArgumentException("result is required");
            return mapper.fromPublishResult(response.getResult());
        });
    }

    @Override
    public List<DdcManagementConfigClientInstance> getConfigClients(DdcManagementInstanceQuery query) {
        return invoke(DdcRpcOperation.MANAGEMENT_INSTANCE_READ, () ->
                mapper.fromConfigClientsResponse(rpc.getConfigClients(
                        mapper.toConfigClientsRequest(query))));
    }

    @Override
    public List<DdcManagementScopeBinding> getScopeBindings(DdcManagementScopeQuery query) {
        return invoke(DdcRpcOperation.MANAGEMENT_SCOPE_READ, () ->
                mapper.fromScopeBindingsResponse(rpc.getScopeBindings(
                        mapper.toScopeBindingsRequest(query))));
    }

    @Override
    public DdcManagementServiceCatalog getServiceKeys(DdcManagementServiceQuery query) {
        return invoke(DdcRpcOperation.MANAGEMENT_REGISTRY_READ, () ->
                mapper.fromServiceKeysResponse(rpc.getServiceKeys(
                        mapper.toServiceKeysRequest(query))));
    }

    @Override
    public DdcManagementServiceSnapshot getInstances(DdcManagementServiceQuery query) {
        return invoke(DdcRpcOperation.MANAGEMENT_REGISTRY_READ, () ->
                mapper.fromInstancesResponse(rpc.getInstances(
                        mapper.toInstancesRequest(query))));
    }

    private <T> T invoke(DdcRpcOperation operation, Invocation<T> invocation) {
        try { return invocation.call(); }
        catch (RuntimeException failure) { throw errors.restore(failure, operation); }
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value;
    }

    @FunctionalInterface private interface Invocation<T> { T call(); }
}
