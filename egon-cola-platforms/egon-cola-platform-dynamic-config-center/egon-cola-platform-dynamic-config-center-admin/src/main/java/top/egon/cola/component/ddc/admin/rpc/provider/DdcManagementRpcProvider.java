package top.egon.cola.component.ddc.admin.rpc.provider;

import org.springframework.beans.factory.annotation.Autowired;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.admin.service.management.DdcManagementFacade;
import top.egon.cola.component.ddc.admin.service.lease.DdcResourceAdmissionRevocationService;
import top.egon.cola.component.ddc.admin.security.rpc.DdcServicePrincipal;
import top.egon.cola.component.ddc.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.ddc.error.management.DdcManagementErrorCode;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigDeleteRequest;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigUpsertRequest;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishRequest;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;
import top.egon.cola.component.rpc.ddc.autoconfigure.DdcRpcProperties;
import top.egon.cola.component.rpc.ddc.contract.DdcManagementRpc;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DeleteConfigRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DeleteConfigResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.FindConfigRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.FindConfigResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetAppRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetAppResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetBizRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetBizResponse;
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
import top.egon.cola.component.rpc.ddc.contract.proto.v1.ListAppsRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.ListAppsResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.ListBizsRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.ListBizsResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.PublishConfigRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.PublishConfigResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.RetryPublishTaskRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.RetryPublishTaskResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.RevokeResourceAdmissionRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.RevokeResourceAdmissionResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.UpsertConfigRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.UpsertConfigResponse;
import top.egon.cola.component.rpc.ddc.mapping.DdcCommonProtoMapper;
import top.egon.cola.component.rpc.ddc.mapping.DdcManagementProtoMapper;

/**
 * 将管理 RPC 契约适配到 DDC 管理应用门面。
 * / Adapts the management RPC contract to the DDC management facade.
 */
@EgonRpcProvider
public class DdcManagementRpcProvider implements DdcManagementRpc {

    private final DdcManagementFacade facade;
    private final DdcResourceAdmissionRevocationService revocations;
    private final DdcManagementProtoMapper mapper;

    /**
     * 使用本地 DDC 和 RPC 限额创建 Provider。
     * / Creates the provider with the local DDC and RPC size limits.
     */
    @Autowired
    public DdcManagementRpcProvider(
            DdcManagementFacade facade,
            DdcResourceAdmissionRevocationService revocations,
            DdcProperties ddcProperties,
            DdcRpcProperties rpcProperties) {
        this(
                facade,
                revocations,
                new DdcManagementProtoMapper(
                        new DdcCommonProtoMapper(
                                rpcProperties.getMaxInboundMessageSize()),
                        ddcProperties.getMaxConfigBytes()
                )
        );
    }

    /** 使用显式映射器创建 Provider。 / Creates the provider with an explicit mapper. */
    public DdcManagementRpcProvider(
            DdcManagementFacade facade,
            DdcResourceAdmissionRevocationService revocations,
            DdcManagementProtoMapper mapper) {
        this.facade = facade;
        this.revocations = revocations;
        this.mapper = mapper;
    }

    @Override
    public GetBizResponse getBiz(GetBizRequest request) {
        DdcServicePrincipal.current();
        return mapper.toBizResponse(facade.getBiz(
                mapper.fromGetBizRequest(request)));
    }

    @Override
    public ListBizsResponse listBizs(ListBizsRequest request) {
        DdcServicePrincipal.current();
        return mapper.toBizsResponse(facade.listBizs(
                mapper.fromListBizsRequest(request)));
    }

    @Override
    public GetAppResponse getApp(GetAppRequest request) {
        DdcServicePrincipal.current();
        return mapper.toAppResponse(facade.getApp(
                mapper.fromGetAppRequest(request)));
    }

    @Override
    public ListAppsResponse listApps(ListAppsRequest request) {
        DdcServicePrincipal.current();
        return mapper.toAppsResponse(facade.listApps(
                mapper.fromListAppsRequest(request)));
    }

    @Override
    public FindConfigResponse findConfig(FindConfigRequest request) {
        DdcServicePrincipal.current();
        try {
            return FindConfigResponse.newBuilder()
                    .setFound(true)
                    .setConfig(mapper.toConfig(
                            facade.findConfig(mapper.fromFindRequest(request))))
                    .build();
        } catch (DdcAdminException exception) {
            if (exception.getCode()
                    != DdcManagementErrorCode.CONFIG_NOT_FOUND.getCode()) {
                throw exception;
            }
            return FindConfigResponse.getDefaultInstance();
        }
    }

    @Override
    public UpsertConfigResponse upsertConfig(UpsertConfigRequest request) {
        DdcServicePrincipal principal = DdcServicePrincipal.current();
        var command = mapper.fromUpsertRequest(request);
        command = new DdcManagementConfigUpsertRequest(
                command.bizCode(),
                command.env(),
                command.appCode(),
                command.resourceName(),
                command.content(),
                command.format(),
                command.description(),
                command.expectedVersion(),
                principal.auditOperator(command.operator())
        );
        return UpsertConfigResponse.newBuilder()
                .setConfig(mapper.toConfig(
                        facade.upsert(command)))
                .build();
    }

    @Override
    public DeleteConfigResponse deleteConfig(DeleteConfigRequest request) {
        DdcServicePrincipal principal = DdcServicePrincipal.current();
        var command = mapper.fromDeleteRequest(request);
        facade.delete(new DdcManagementConfigDeleteRequest(
                command.bizCode(),
                command.env(),
                command.appCode(),
                command.expectedVersion(),
                principal.auditOperator(command.operator()),
                command.reason()
        ));
        return DeleteConfigResponse.getDefaultInstance();
    }

    @Override
    public PublishConfigResponse publishConfig(PublishConfigRequest request) {
        DdcServicePrincipal principal = DdcServicePrincipal.current();
        var command = mapper.fromPublishRequest(request);
        command = new DdcManagementPublishRequest(
                command.bizCode(),
                command.env(),
                command.appCode(),
                command.resourceName(),
                command.content(),
                command.format(),
                command.expectedVersion(),
                command.changeId(),
                command.timeoutMs(),
                principal.auditOperator(command.operator())
        );
        return PublishConfigResponse.newBuilder()
                .setResult(mapper.toPublishResult(
                        facade.publish(command)))
                .build();
    }

    @Override
    public GetPublishTaskResponse getPublishTask(
            GetPublishTaskRequest request) {
        DdcServicePrincipal.current();
        return GetPublishTaskResponse.newBuilder()
                .setFound(true)
                .setTask(mapper.toPublishTask(
                        facade.getPublishTask(request.getChangeId())))
                .build();
    }

    @Override
    public RetryPublishTaskResponse retryPublishTask(
            RetryPublishTaskRequest request) {
        DdcServicePrincipal principal = DdcServicePrincipal.current();
        return RetryPublishTaskResponse.newBuilder()
                .setResult(mapper.toPublishResult(
                        facade.retry(
                                request.getChangeId(),
                                principal.auditOperator(
                                        request.getRequestedOperator()))))
                .build();
    }

    @Override
    public GetConfigClientsResponse getConfigClients(
            GetConfigClientsRequest request) {
        DdcServicePrincipal.current();
        return mapper.toConfigClientsResponse(facade.getConfigClients(
                mapper.fromConfigClientsRequest(request)));
    }

    /**
     * 撤销精确 Resource Server 三元组的准入租约。
     * / Revokes admission leases for an exact Resource Server triple.
     *
     * @param request protobuf 撤销请求 / protobuf revocation request
     * @return 幂等撤销统计 / idempotent revocation counts
     */
    @Override
    public RevokeResourceAdmissionResponse revokeResourceAdmission(
            RevokeResourceAdmissionRequest request) {
        DdcServicePrincipal.current();
        return mapper.toResourceAdmissionRevocationResponse(
                revocations.revoke(
                        mapper.fromResourceAdmissionRevocationRequest(
                                request
                        )
                )
        );
    }

    @Override
    public GetScopeBindingsResponse getScopeBindings(
            GetScopeBindingsRequest request) {
        DdcServicePrincipal.current();
        return mapper.toScopeBindingsResponse(facade.getScopeBindings(
                mapper.fromScopeBindingsRequest(request)));
    }

    @Override
    public GetServiceKeysResponse getServiceKeys(GetServiceKeysRequest request) {
        DdcServicePrincipal.current();
        return mapper.toServiceKeysResponse(facade.getServiceKeys(
                mapper.fromServiceKeysRequest(request)));
    }

    @Override
    public GetInstancesResponse getInstances(GetInstancesRequest request) {
        DdcServicePrincipal.current();
        return mapper.toInstancesResponse(facade.getInstances(
                mapper.fromInstancesRequest(request)));
    }
}
