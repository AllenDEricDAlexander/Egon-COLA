package top.egon.cola.component.rpc.ddc.contract;

import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcManagementServiceGrpc;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DeleteConfigRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DeleteConfigResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.FindConfigRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.FindConfigResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetConfigClientsRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetConfigClientsResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetInstancesRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetInstancesResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetPublishTaskRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetPublishTaskResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetScopeBindingsRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetScopeBindingsResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetServiceKeysRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetServiceKeysResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.PublishConfigRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.PublishConfigResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.RetryPublishTaskRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.RetryPublishTaskResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.UpsertConfigRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.UpsertConfigResponse;

/**
 * DDC 管理 RPC 门面契约。
 * / RPC facade contract for DDC management operations.
 */
@EgonRpcService(
        grpcClass = DdcManagementServiceGrpc.class,
        group = "ddc",
        version = "1.0.0"
)
public interface DdcManagementRpc {

    /**
     * 查询配置。 / Finds a configuration.
     *
     * @param request 查询请求 / lookup request
     * @return 查询结果 / lookup result
     */
    @EgonRpcMethod(name = "FindConfig", idempotent = true)
    FindConfigResponse findConfig(FindConfigRequest request);

    /**
     * 新增或更新配置。 / Creates or updates a configuration.
     *
     * @param request 写入请求 / upsert request
     * @return 写入结果 / upsert result
     */
    @EgonRpcMethod(name = "UpsertConfig")
    UpsertConfigResponse upsertConfig(UpsertConfigRequest request);

    /**
     * 删除配置。 / Deletes a configuration.
     *
     * @param request 删除请求 / deletion request
     * @return 空删除响应 / empty deletion response
     */
    @EgonRpcMethod(name = "DeleteConfig")
    DeleteConfigResponse deleteConfig(DeleteConfigRequest request);

    /**
     * 发布配置。 / Publishes a configuration.
     *
     * @param request 发布请求 / publication request
     * @return 发布结果 / publication result
     */
    @EgonRpcMethod(name = "PublishConfig")
    PublishConfigResponse publishConfig(PublishConfigRequest request);

    /**
     * 查询发布任务。 / Gets a publication task.
     *
     * @param request 查询请求 / lookup request
     * @return 发布任务 / publication task
     */
    @EgonRpcMethod(name = "GetPublishTask", idempotent = true)
    GetPublishTaskResponse getPublishTask(GetPublishTaskRequest request);

    /**
     * 重试发布任务。 / Retries a publication task.
     *
     * @param request 重试请求 / retry request
     * @return 发布结果 / publication result
     */
    @EgonRpcMethod(name = "RetryPublishTask")
    RetryPublishTaskResponse retryPublishTask(RetryPublishTaskRequest request);

    /**
     * 查询配置客户端租约。 / Gets configuration client leases.
     *
     * @param request 查询请求 / query request
     * @return 客户端集合 / clients
     */
    @EgonRpcMethod(name = "GetConfigClients", idempotent = true)
    GetConfigClientsResponse getConfigClients(
            GetConfigClientsRequest request);

    /**
     * 查询作用域绑定。 / Gets scope bindings.
     *
     * @param request 查询请求 / query request
     * @return 绑定集合 / bindings
     */
    @EgonRpcMethod(name = "GetScopeBindings", idempotent = true)
    GetScopeBindingsResponse getScopeBindings(
            GetScopeBindingsRequest request);

    /**
     * 查询服务目录。 / Gets service keys.
     *
     * @param request 查询请求 / query request
     * @return 服务键快照 / service key snapshot
     */
    @EgonRpcMethod(name = "GetServiceKeys", idempotent = true)
    GetServiceKeysResponse getServiceKeys(GetServiceKeysRequest request);

    /**
     * 查询服务实例。 / Gets service instances.
     *
     * @param request 查询请求 / query request
     * @return 实例快照 / instance snapshot
     */
    @EgonRpcMethod(name = "GetInstances", idempotent = true)
    GetInstancesResponse getInstances(GetInstancesRequest request);
}
