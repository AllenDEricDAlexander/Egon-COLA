package top.egon.cola.component.rpc.ddc.mapping;

import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.model.management.DdcManagementConfig;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigClientInstance;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigDeleteRequest;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigUpsertRequest;
import top.egon.cola.component.ddc.model.management.DdcManagementInstanceQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishRequest;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishResult;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishTarget;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishTask;
import top.egon.cola.component.ddc.model.management.DdcManagementScopeBinding;
import top.egon.cola.component.ddc.model.management.DdcManagementScopeQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceCatalog;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceInstance;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceKey;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceSnapshot;
import top.egon.cola.component.ddc.model.management.DdcResourceAdmissionRevocationRequest;
import top.egon.cola.component.ddc.model.management.DdcResourceAdmissionRevocationResult;
import top.egon.cola.component.ddc.model.registry.DdcServiceKind;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DeleteConfigRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcConfig;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcConfigClientInstance;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcPublishResult;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcPublishTarget;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcPublishTask;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcScope;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcScopeBinding;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcScopeBindingQuery;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.FindConfigRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetConfigClientsRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetConfigClientsResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetInstancesRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetInstancesResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetScopeBindingsRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetScopeBindingsResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetServiceKeysRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetServiceKeysResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.PublishConfigRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.RevokeResourceAdmissionRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.RevokeResourceAdmissionResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.UpsertConfigRequest;

import java.util.List;
import java.util.Locale;

/**
 * DDC 管理 Port 模型与 protobuf 请求响应之间的显式映射。
 * / Explicit mapping between DDC management Port models and protobuf requests
 * and responses.
 */
public final class DdcManagementProtoMapper {

    private final DdcCommonProtoMapper common;
    private final long maxConfigBytes;

    public DdcManagementProtoMapper(
            DdcCommonProtoMapper common,
            long maxConfigBytes) {
        if (common == null || maxConfigBytes <= 0) {
            throw new IllegalArgumentException(
                    "common mapper and positive maxConfigBytes are required");
        }
        this.common = common;
        this.maxConfigBytes = maxConfigBytes;
    }

    public FindConfigRequest toFindRequest(DdcManagementConfigQuery value) {
        require(value, "config query");
        return common.checked(FindConfigRequest.newBuilder()
                .setScope(common.toScope(
                        value.bizCode(), value.env(), value.appCode()))
                .build());
    }

    public DdcManagementConfigQuery fromFindRequest(FindConfigRequest value) {
        common.checked(value);
        scopeRequired(value.hasScope());
        return new DdcManagementConfigQuery(
                value.getScope().getBizCode(),
                value.getScope().getEnv(),
                value.getScope().getAppCode()
        );
    }

    public DdcConfig toConfig(DdcManagementConfig value) {
        require(value, "config");
        common.validateConfigContent(value.content(), maxConfigBytes);
        var builder = DdcConfig.newBuilder()
                .setScope(common.toScope(
                        value.bizCode(), value.env(), value.appCode()))
                .setResourceName(DdcCommonProtoMapper.require(
                        value.resourceName(), "resourceName"))
                .setContent(value.content())
                .setFormat(common.toProtoFormat(value.format()))
                .setEnabled(value.enabled())
                .setDeleted(value.deleted())
                .setUpdatedAt(common.toTimestamp(value.updatedAt()));
        if (value.version() != null) {
            builder.setVersion(value.version());
        }
        return common.checked(builder.build());
    }

    public DdcManagementConfig fromConfig(DdcConfig value) {
        common.checked(value);
        scopeRequired(value.hasScope());
        common.validateConfigContent(value.getContent(), maxConfigBytes);
        return new DdcManagementConfig(
                value.getScope().getBizCode(),
                value.getScope().getEnv(),
                value.getScope().getAppCode(),
                value.getResourceName(),
                value.getContent(),
                common.fromProtoFormat(value.getFormat()),
                value.hasVersion() ? value.getVersion() : null,
                value.getEnabled(),
                value.getDeleted(),
                common.fromTimestamp(value.getUpdatedAt())
        );
    }

    public UpsertConfigRequest toUpsertRequest(
            DdcManagementConfigUpsertRequest value) {
        require(value, "upsert request");
        common.validateConfigContent(value.content(), maxConfigBytes);
        var builder = UpsertConfigRequest.newBuilder()
                .setScope(common.toScope(
                        value.bizCode(), value.env(), value.appCode()))
                .setResourceName(value.resourceName())
                .setContent(value.content())
                .setFormat(common.toProtoFormat(value.format()))
                .setRequestedOperator(value.operator());
        set(builder::setDescription, value.description());
        if (value.expectedVersion() != null) {
            builder.setExpectedVersion(value.expectedVersion());
        }
        return common.checked(builder.build());
    }

    public DdcManagementConfigUpsertRequest fromUpsertRequest(
            UpsertConfigRequest value) {
        common.checked(value);
        scopeRequired(value.hasScope());
        common.validateConfigContent(value.getContent(), maxConfigBytes);
        return new DdcManagementConfigUpsertRequest(
                value.getScope().getBizCode(),
                value.getScope().getEnv(),
                value.getScope().getAppCode(),
                value.getResourceName(),
                value.getContent(),
                common.fromProtoFormat(value.getFormat()),
                value.hasDescription() ? value.getDescription() : null,
                value.hasExpectedVersion() ? value.getExpectedVersion() : null,
                value.getRequestedOperator()
        );
    }

    public DeleteConfigRequest toDeleteRequest(
            DdcManagementConfigDeleteRequest value) {
        require(value, "delete request");
        var builder = DeleteConfigRequest.newBuilder()
                .setScope(common.toScope(
                        value.bizCode(), value.env(), value.appCode()))
                .setRequestedOperator(value.operator());
        if (value.expectedVersion() != null) {
            builder.setExpectedVersion(value.expectedVersion());
        }
        set(builder::setReason, value.reason());
        return common.checked(builder.build());
    }

    public DdcManagementConfigDeleteRequest fromDeleteRequest(
            DeleteConfigRequest value) {
        common.checked(value);
        scopeRequired(value.hasScope());
        return new DdcManagementConfigDeleteRequest(
                value.getScope().getBizCode(),
                value.getScope().getEnv(),
                value.getScope().getAppCode(),
                value.hasExpectedVersion() ? value.getExpectedVersion() : null,
                value.getRequestedOperator(),
                value.hasReason() ? value.getReason() : null
        );
    }

    public PublishConfigRequest toPublishRequest(
            DdcManagementPublishRequest value) {
        require(value, "publish request");
        common.validateConfigContent(value.content(), maxConfigBytes);
        var builder = PublishConfigRequest.newBuilder()
                .setScope(common.toScope(
                        value.bizCode(), value.env(), value.appCode()))
                .setResourceName(value.resourceName())
                .setContent(value.content())
                .setFormat(common.toProtoFormat(value.format()))
                .setRequestedOperator(value.operator());
        if (value.expectedVersion() != null) {
            builder.setExpectedVersion(value.expectedVersion());
        }
        set(builder::setChangeId, value.changeId());
        if (value.timeoutMs() != null) {
            builder.setTimeoutMs(value.timeoutMs());
        }
        return common.checked(builder.build());
    }

    public DdcManagementPublishRequest fromPublishRequest(
            PublishConfigRequest value) {
        common.checked(value);
        scopeRequired(value.hasScope());
        common.validateConfigContent(value.getContent(), maxConfigBytes);
        return new DdcManagementPublishRequest(
                value.getScope().getBizCode(),
                value.getScope().getEnv(),
                value.getScope().getAppCode(),
                value.getResourceName(),
                value.getContent(),
                common.fromProtoFormat(value.getFormat()),
                value.hasExpectedVersion() ? value.getExpectedVersion() : null,
                value.hasChangeId() ? value.getChangeId() : null,
                value.hasTimeoutMs() ? value.getTimeoutMs() : null,
                value.getRequestedOperator()
        );
    }

    public DdcPublishTarget toPublishTarget(DdcManagementPublishTarget value) {
        require(value, "publish target");
        var builder = DdcPublishTarget.newBuilder()
                .setInstanceId(value.instanceId())
                .setLeaseId(value.leaseId())
                .setStatus(common.toProtoAckStatus(value.status()));
        if (value.currentVersion() != null) {
            builder.setCurrentVersion(value.currentVersion());
        }
        set(builder::setErrorMessage, value.errorMessage());
        if (value.ackAt() != null) {
            builder.setAcknowledgedAt(common.toTimestamp(value.ackAt()));
        }
        return common.checked(builder.build());
    }

    public DdcManagementPublishTarget fromPublishTarget(DdcPublishTarget value) {
        common.checked(value);
        return new DdcManagementPublishTarget(
                value.getInstanceId(),
                value.getLeaseId(),
                value.hasCurrentVersion() ? value.getCurrentVersion() : null,
                common.fromProtoAckStatusText(value.getStatus()),
                value.hasErrorMessage() ? value.getErrorMessage() : null,
                value.hasAcknowledgedAt()
                        ? common.fromTimestamp(value.getAcknowledgedAt()) : null
        );
    }

    public DdcPublishResult toPublishResult(DdcManagementPublishResult value) {
        require(value, "publish result");
        var builder = DdcPublishResult.newBuilder()
                .setChangeId(value.changeId())
                .setStatus(common.toProto(value.status()))
                .setTargetCount(value.targetCount())
                .setCreatedAt(common.toTimestamp(value.createdAt()));
        if (value.dispatchedAt() != null) {
            builder.setDispatchedAt(common.toTimestamp(value.dispatchedAt()));
        }
        if (value.completedAt() != null) {
            builder.setCompletedAt(common.toTimestamp(value.completedAt()));
        }
        if (value.targetVersion() != null) {
            builder.setTargetVersion(value.targetVersion());
        }
        set(builder::setResourceChecksum, value.resourceChecksum());
        value.targets().forEach(target -> builder.addTargets(toPublishTarget(target)));
        set(builder::setErrorMessage, value.errorMessage());
        return common.checked(builder.build());
    }

    public DdcManagementPublishResult fromPublishResult(DdcPublishResult value) {
        common.checked(value);
        return new DdcManagementPublishResult(
                value.getChangeId(),
                common.fromProtoPublishStatus(value.getStatus()),
                value.hasTargetVersion() ? value.getTargetVersion() : null,
                value.hasResourceChecksum() ? value.getResourceChecksum() : null,
                value.getTargetCount(),
                value.getTargetsList().stream()
                        .map(this::fromPublishTarget).toList(),
                value.hasErrorMessage() ? value.getErrorMessage() : null,
                common.fromTimestamp(value.getCreatedAt()),
                value.hasDispatchedAt()
                        ? common.fromTimestamp(value.getDispatchedAt()) : null,
                value.hasCompletedAt()
                        ? common.fromTimestamp(value.getCompletedAt()) : null
        );
    }

    public DdcPublishTask toPublishTask(DdcManagementPublishTask value) {
        require(value, "publish task");
        var builder = DdcPublishTask.newBuilder()
                .setChangeId(value.changeId())
                .setStatus(common.toProto(value.status()))
                .setTargetCount(value.targetCount())
                .setAcknowledgedCount(value.ackCount())
                .setFailedCount(value.failedCount())
                .setIgnoredCount(value.ignoredCount())
                .setTimeoutCount(value.timeoutCount())
                .setAttemptCount(value.attemptCount())
                .setCreatedAt(common.toTimestamp(value.createdAt()));
        if (value.dispatchedAt() != null) {
            builder.setDispatchedAt(common.toTimestamp(value.dispatchedAt()));
        }
        if (value.completedAt() != null) {
            builder.setCompletedAt(common.toTimestamp(value.completedAt()));
        }
        if (value.targetVersion() != null) {
            builder.setTargetVersion(value.targetVersion());
        }
        set(builder::setResourceChecksum, value.resourceChecksum());
        value.targets().forEach(target -> builder.addTargets(toPublishTarget(target)));
        set(builder::setErrorMessage, value.errorMessage());
        return common.checked(builder.build());
    }

    public DdcManagementPublishTask fromPublishTask(DdcPublishTask value) {
        common.checked(value);
        return new DdcManagementPublishTask(
                value.getChangeId(),
                common.fromProtoPublishStatus(value.getStatus()),
                value.hasTargetVersion() ? value.getTargetVersion() : null,
                value.hasResourceChecksum() ? value.getResourceChecksum() : null,
                value.getTargetCount(),
                value.getAcknowledgedCount(),
                value.getFailedCount(),
                value.getIgnoredCount(),
                value.getTimeoutCount(),
                value.getAttemptCount(),
                value.getTargetsList().stream()
                        .map(this::fromPublishTarget).toList(),
                value.hasErrorMessage() ? value.getErrorMessage() : null,
                common.fromTimestamp(value.getCreatedAt()),
                value.hasDispatchedAt()
                        ? common.fromTimestamp(value.getDispatchedAt()) : null,
                value.hasCompletedAt()
                        ? common.fromTimestamp(value.getCompletedAt()) : null
        );
    }

    public GetConfigClientsRequest toConfigClientsRequest(
            DdcManagementInstanceQuery value) {
        require(value, "instance query");
        return common.checked(GetConfigClientsRequest.newBuilder()
                .setScope(optionalScope(
                        value.bizCode(), value.env(), value.appCode()))
                .build());
    }

    public DdcManagementInstanceQuery fromConfigClientsRequest(
            GetConfigClientsRequest value) {
        common.checked(value);
        scopeRequired(value.hasScope());
        return new DdcManagementInstanceQuery(
                emptyToNull(value.getScope().getBizCode()),
                emptyToNull(value.getScope().getEnv()),
                emptyToNull(value.getScope().getAppCode())
        );
    }

    public GetConfigClientsResponse toConfigClientsResponse(
            List<DdcManagementConfigClientInstance> values) {
        var builder = GetConfigClientsResponse.newBuilder();
        if (values != null) {
            values.forEach(value -> builder.addClients(toConfigClient(value)));
        }
        return common.checked(builder.build());
    }

    public List<DdcManagementConfigClientInstance> fromConfigClientsResponse(
            GetConfigClientsResponse value) {
        common.checked(value);
        return value.getClientsList().stream()
                .map(this::fromConfigClient).toList();
    }

    /**
     * 将 Resource 准入撤销命令转换为 protobuf 请求。
     * / Converts a Resource admission-revocation command to protobuf.
     *
     * @param value 共享管理命令 / shared management command
     * @return protobuf 撤销请求 / protobuf revocation request
     */
    public RevokeResourceAdmissionRequest toResourceAdmissionRevocationRequest(
            DdcResourceAdmissionRevocationRequest value) {
        require(value, "resource admission revocation request");
        return common.checked(RevokeResourceAdmissionRequest.newBuilder()
                .setResourceServerId(value.resourceServerId())
                .setScope(common.toScope(
                        value.bizCode(), value.env(), value.appCode()))
                .setResourceVersion(value.resourceVersion())
                .build());
    }

    /**
     * 将 protobuf Resource 准入撤销请求转换为共享管理命令。
     * / Converts a protobuf Resource admission-revocation request to the shared command.
     *
     * @param value protobuf 撤销请求 / protobuf revocation request
     * @return 共享管理命令 / shared management command
     */
    public DdcResourceAdmissionRevocationRequest
            fromResourceAdmissionRevocationRequest(
                    RevokeResourceAdmissionRequest value) {
        common.checked(value);
        scopeRequired(value.hasScope());
        return new DdcResourceAdmissionRevocationRequest(
                value.getResourceServerId(),
                value.getScope().getBizCode(),
                value.getScope().getAppCode(),
                value.getScope().getEnv(),
                value.getResourceVersion()
        );
    }

    /**
     * 将幂等撤销统计转换为 protobuf 响应。
     * / Converts idempotent revocation counts to protobuf.
     *
     * @param value 共享撤销结果 / shared revocation result
     * @return protobuf 撤销响应 / protobuf revocation response
     */
    public RevokeResourceAdmissionResponse toResourceAdmissionRevocationResponse(
            DdcResourceAdmissionRevocationResult value) {
        require(value, "resource admission revocation result");
        return common.checked(RevokeResourceAdmissionResponse.newBuilder()
                .setConfigLeaseCount(value.configLeaseCount())
                .setProviderLeaseCount(value.providerLeaseCount())
                .setPersistedInstanceCount(value.persistedInstanceCount())
                .build());
    }

    /**
     * 将 protobuf 撤销响应转换为共享统计。
     * / Converts a protobuf revocation response to shared counts.
     *
     * @param value protobuf 撤销响应 / protobuf revocation response
     * @return 共享撤销结果 / shared revocation result
     */
    public DdcResourceAdmissionRevocationResult
            fromResourceAdmissionRevocationResponse(
                    RevokeResourceAdmissionResponse value) {
        common.checked(value);
        return new DdcResourceAdmissionRevocationResult(
                value.getConfigLeaseCount(),
                value.getProviderLeaseCount(),
                value.getPersistedInstanceCount()
        );
    }

    public GetScopeBindingsRequest toScopeBindingsRequest(
            DdcManagementScopeQuery value) {
        require(value, "scope query");
        var query = DdcScopeBindingQuery.newBuilder();
        set(query::setBizCode, value.bizCode());
        set(query::setNamespaceCode, value.namespaceCode());
        set(query::setEnv, value.env());
        set(query::setAppCode, value.appCode());
        return common.checked(GetScopeBindingsRequest.newBuilder()
                .setQuery(query)
                .build());
    }

    public DdcManagementScopeQuery fromScopeBindingsRequest(
            GetScopeBindingsRequest value) {
        common.checked(value);
        if (!value.hasQuery()) {
            throw new IllegalArgumentException("scope query is required");
        }
        DdcScopeBindingQuery query = value.getQuery();
        return new DdcManagementScopeQuery(
                query.hasBizCode() ? query.getBizCode() : null,
                query.hasNamespaceCode() ? query.getNamespaceCode() : null,
                query.hasEnv() ? query.getEnv() : null,
                query.hasAppCode() ? query.getAppCode() : null
        );
    }

    public GetScopeBindingsResponse toScopeBindingsResponse(
            List<DdcManagementScopeBinding> values) {
        var builder = GetScopeBindingsResponse.newBuilder();
        if (values != null) {
            values.forEach(value -> builder.addBindings(toScopeBinding(value)));
        }
        return common.checked(builder.build());
    }

    public List<DdcManagementScopeBinding> fromScopeBindingsResponse(
            GetScopeBindingsResponse value) {
        common.checked(value);
        return value.getBindingsList().stream()
                .map(this::fromScopeBinding).toList();
    }

    public GetServiceKeysRequest toServiceKeysRequest(
            DdcManagementServiceQuery value) {
        return common.checked(GetServiceKeysRequest.newBuilder()
                .setQuery(toServiceQuery(value))
                .build());
    }

    public DdcManagementServiceQuery fromServiceKeysRequest(
            GetServiceKeysRequest value) {
        common.checked(value);
        if (!value.hasQuery()) {
            throw new IllegalArgumentException("service query is required");
        }
        return fromServiceQuery(value.getQuery());
    }

    public GetInstancesRequest toInstancesRequest(
            DdcManagementServiceQuery value) {
        return common.checked(GetInstancesRequest.newBuilder()
                .setQuery(toServiceQuery(value))
                .build());
    }

    public DdcManagementServiceQuery fromInstancesRequest(
            GetInstancesRequest value) {
        common.checked(value);
        if (!value.hasQuery()) {
            throw new IllegalArgumentException("service query is required");
        }
        return fromServiceQuery(value.getQuery());
    }

    public GetServiceKeysResponse toServiceKeysResponse(
            DdcManagementServiceCatalog value) {
        require(value, "service catalog");
        var builder = GetServiceKeysResponse.newBuilder()
                .setGeneration(value.generation())
                .setObservedAt(common.toTimestamp(value.observedAt()));
        value.services().forEach(service -> builder.addServiceKeys(
                toServiceKey(service)));
        return common.checked(builder.build());
    }

    public DdcManagementServiceCatalog fromServiceKeysResponse(
            GetServiceKeysResponse value) {
        common.checked(value);
        return new DdcManagementServiceCatalog(
                value.getGeneration(),
                common.fromTimestamp(value.getObservedAt()),
                value.getServiceKeysList().stream()
                        .map(this::fromServiceKey).toList()
        );
    }

    public GetInstancesResponse toInstancesResponse(
            DdcManagementServiceSnapshot value) {
        require(value, "service snapshot");
        var builder = GetInstancesResponse.newBuilder()
                .setServiceKey(toServiceKey(value.serviceKey()))
                .setGeneration(value.generation())
                .setObservedAt(common.toTimestamp(value.observedAt()));
        value.instances().forEach(instance -> builder.addInstances(
                toServiceInstance(instance)));
        return common.checked(builder.build());
    }

    public DdcManagementServiceSnapshot fromInstancesResponse(
            GetInstancesResponse value) {
        common.checked(value);
        if (!value.hasServiceKey()) {
            throw new IllegalArgumentException("serviceKey is required");
        }
        return new DdcManagementServiceSnapshot(
                fromServiceKey(value.getServiceKey()),
                value.getGeneration(),
                common.fromTimestamp(value.getObservedAt()),
                value.getInstancesList().stream()
                        .map(this::fromServiceInstance).toList()
        );
    }

    private DdcConfigClientInstance toConfigClient(
            DdcManagementConfigClientInstance value) {
        var builder = DdcConfigClientInstance.newBuilder()
                .setScope(common.toScope(
                        value.bizCode(), value.env(), value.appCode()))
                .setInstanceId(value.instanceId())
                .setLeaseId(value.leaseId())
                .setHost(value.host())
                .setLeaseRole(common.toProto(DdcLeaseRole.valueOf(
                        value.leaseRole().toUpperCase(Locale.ROOT))))
                .setStatus(value.status())
                .setRegisteredAt(common.toTimestamp(value.registeredAt()))
                .setLastHeartbeatAt(common.toTimestamp(value.lastHeartbeatAt()))
                .setExpireAt(common.toTimestamp(value.expireAt()))
                .putAllMetadata(common.validatedMetadata(value.metadata()));
        if (value.port() != null) {
            builder.setPort(value.port());
        }
        return common.checked(builder.build());
    }

    private DdcManagementConfigClientInstance fromConfigClient(
            DdcConfigClientInstance value) {
        common.checked(value);
        scopeRequired(value.hasScope());
        return new DdcManagementConfigClientInstance(
                value.getScope().getBizCode(),
                value.getScope().getEnv(),
                value.getScope().getAppCode(),
                value.getInstanceId(),
                value.getLeaseId(),
                value.getHost(),
                value.hasPort() ? value.getPort() : null,
                common.fromProtoLeaseRole(value.getLeaseRole()).name(),
                value.getStatus(),
                common.fromTimestamp(value.getRegisteredAt()),
                common.fromTimestamp(value.getLastHeartbeatAt()),
                common.fromTimestamp(value.getExpireAt()),
                common.validatedMetadata(value.getMetadataMap())
        );
    }

    private DdcScopeBinding toScopeBinding(DdcManagementScopeBinding value) {
        return common.checked(DdcScopeBinding.newBuilder()
                .setBindingId(value.bindingId())
                .setBizCode(value.bizCode())
                .setNamespaceCode(value.namespaceCode())
                .setEnv(value.env())
                .setAppId(value.appId())
                .setAppCode(value.appCode())
                .setAppName(value.appName())
                .setEnabled(value.enabled())
                .build());
    }

    private DdcManagementScopeBinding fromScopeBinding(DdcScopeBinding value) {
        common.checked(value);
        return new DdcManagementScopeBinding(
                value.getBindingId(), value.getBizCode(), value.getNamespaceCode(),
                value.getEnv(), value.getAppId(), value.getAppCode(),
                value.getAppName(), value.getEnabled()
        );
    }

    private top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcManagementServiceQuery toServiceQuery(
            DdcManagementServiceQuery value) {
        require(value, "service query");
        var builder = top.egon.cola.component.rpc.ddc.contract.proto.v1
                .DdcManagementServiceQuery.newBuilder();
        set(builder::setBizCode, value.bizCode());
        set(builder::setNamespaceCode, value.namespaceCode());
        set(builder::setEnv, value.env());
        set(builder::setAppCode, value.appCode());
        if (value.serviceKind() != null) {
            builder.setServiceKind(common.toProto(DdcServiceKind.valueOf(
                    value.serviceKind().toUpperCase(Locale.ROOT))));
        }
        set(builder::setProtocol, value.protocol());
        set(builder::setServiceName, value.serviceName());
        set(builder::setGroup, value.group());
        set(builder::setVersion, value.version());
        return common.checked(builder.build());
    }

    private DdcManagementServiceQuery fromServiceQuery(
            top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcManagementServiceQuery value) {
        common.checked(value);
        return new DdcManagementServiceQuery(
                value.hasBizCode() ? value.getBizCode() : null,
                value.hasNamespaceCode() ? value.getNamespaceCode() : null,
                value.hasEnv() ? value.getEnv() : null,
                value.hasAppCode() ? value.getAppCode() : null,
                value.hasServiceKind()
                        ? common.fromProtoServiceKind(value.getServiceKind()).name()
                        : null,
                value.hasProtocol() ? value.getProtocol() : null,
                value.hasServiceName() ? value.getServiceName() : null,
                value.hasGroup() ? value.getGroup() : null,
                value.hasVersion() ? value.getVersion() : null
        );
    }

    private top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcManagementServiceKey toServiceKey(
            DdcManagementServiceKey value) {
        return common.checked(top.egon.cola.component.rpc.ddc.contract.proto.v1
                .DdcManagementServiceKey.newBuilder()
                .setScope(common.toScope(
                        value.bizCode(), value.env(), value.appCode()))
                .setServiceId(value.serviceId())
                .setServiceKind(common.toProto(DdcServiceKind.valueOf(
                        value.serviceKind().toUpperCase(Locale.ROOT))))
                .setServiceName(value.serviceName())
                .setGroup(value.group())
                .setVersion(value.version())
                .setProtocol(value.protocol())
                .build());
    }

    private DdcManagementServiceKey fromServiceKey(
            top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcManagementServiceKey value) {
        common.checked(value);
        scopeRequired(value.hasScope());
        return new DdcManagementServiceKey(
                value.getScope().getBizCode(),
                value.getScope().getEnv(),
                value.getScope().getAppCode(),
                value.getServiceId(),
                common.fromProtoServiceKind(value.getServiceKind()).name(),
                value.getServiceName(),
                value.getGroup(),
                value.getVersion(),
                value.getProtocol()
        );
    }

    private top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcManagementServiceInstance toServiceInstance(
            DdcManagementServiceInstance value) {
        return common.checked(top.egon.cola.component.rpc.ddc.contract.proto.v1
                .DdcManagementServiceInstance.newBuilder()
                .setInstanceId(value.instanceId())
                .setLeaseId(value.leaseId())
                .setHost(value.host())
                .setPort(value.port())
                .setSecure(value.secure())
                .putAllMetadata(common.validatedMetadata(value.metadata()))
                .setStatus(value.status())
                .setRegisteredAt(common.toTimestamp(value.registeredAt()))
                .setLastHeartbeatAt(common.toTimestamp(value.lastHeartbeatAt()))
                .setExpireAt(common.toTimestamp(value.expireAt()))
                .build());
    }

    private DdcManagementServiceInstance fromServiceInstance(
            top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcManagementServiceInstance value) {
        common.checked(value);
        return new DdcManagementServiceInstance(
                value.getInstanceId(), value.getLeaseId(), value.getHost(),
                value.getPort(), value.getSecure(),
                common.validatedMetadata(value.getMetadataMap()), value.getStatus(),
                common.fromTimestamp(value.getRegisteredAt()),
                common.fromTimestamp(value.getLastHeartbeatAt()),
                common.fromTimestamp(value.getExpireAt())
        );
    }

    private DdcScope optionalScope(String bizCode, String env, String appCode) {
        return DdcScope.newBuilder()
                .setBizCode(nullToEmpty(bizCode))
                .setEnv(nullToEmpty(env))
                .setAppCode(nullToEmpty(appCode))
                .build();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static void scopeRequired(boolean present) {
        if (!present) {
            throw new IllegalArgumentException("scope is required");
        }
    }

    private static void require(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    private static void set(
            java.util.function.Consumer<String> setter,
            String value) {
        if (value != null) {
            setter.accept(value);
        }
    }
}
