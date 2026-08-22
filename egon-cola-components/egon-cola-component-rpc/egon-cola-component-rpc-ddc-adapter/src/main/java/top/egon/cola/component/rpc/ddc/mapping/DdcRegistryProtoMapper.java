package top.egon.cola.component.rpc.ddc.mapping;

import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceInstance;
import top.egon.cola.component.ddc.model.registry.DdcServiceLeaseRequest;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DeregisterServiceRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetServiceInstancesResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetServicesResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.HeartbeatServiceRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.RegisterServiceRequest;

/**
 * DDC 服务注册 Port 模型与 protobuf 请求响应之间的显式映射。
 * / Explicit mapping between DDC registry Port models and protobuf requests
 * and responses.
 */
public final class DdcRegistryProtoMapper {

    private final DdcCommonProtoMapper common;

    public DdcRegistryProtoMapper(DdcCommonProtoMapper common) {
        if (common == null) {
            throw new IllegalArgumentException("common mapper is required");
        }
        this.common = common;
    }

    public RegisterServiceRequest toRegisterRequest(
            DdcServiceRegistration value) {
        require(value, "registration");
        return common.checked(RegisterServiceRequest.newBuilder()
                .setServiceKey(common.toProto(value.serviceKey()))
                .setInstanceId(value.instanceId())
                .setHost(value.host())
                .setPort(value.port())
                .setSecure(value.secure())
                .putAllMetadata(common.validatedMetadata(value.metadata()))
                .setLeaseSeconds(value.leaseSeconds())
                .setHeartbeatIntervalSeconds(
                        value.heartbeatIntervalSeconds())
                .setRegistrationToken(value.registrationToken())
                .build());
    }

    public DdcServiceRegistration fromRegisterRequest(
            RegisterServiceRequest value) {
        common.checked(value);
        if (!value.hasServiceKey()) {
            throw new IllegalArgumentException("serviceKey is required");
        }
        return new DdcServiceRegistration(
                value.getInstanceId(),
                common.fromProto(value.getServiceKey()),
                value.getHost(),
                value.getPort(),
                value.getSecure(),
                common.validatedMetadata(value.getMetadataMap()),
                value.getLeaseSeconds(),
                value.getHeartbeatIntervalSeconds(),
                DdcCommonProtoMapper.require(
                        value.getRegistrationToken(), "registrationToken")
        );
    }

    public HeartbeatServiceRequest toHeartbeatRequest(
            DdcServiceLeaseRequest value) {
        requireLease(value);
        return common.checked(HeartbeatServiceRequest.newBuilder()
                .setServiceKey(common.toProto(value.getServiceKey()))
                .setInstanceId(value.getInstanceId())
                .setLeaseId(value.getLeaseId())
                .setRegistrationToken(DdcCommonProtoMapper.require(
                        value.getRegistrationToken(), "registrationToken"))
                .build());
    }

    public DdcServiceLeaseRequest fromHeartbeatRequest(
            HeartbeatServiceRequest value) {
        common.checked(value);
        DdcServiceLeaseRequest result = lease(
                value.hasServiceKey() ? common.fromProto(value.getServiceKey()) : null,
                value.getInstanceId(),
                value.getLeaseId()
        );
        result.setRegistrationToken(DdcCommonProtoMapper.require(
                value.getRegistrationToken(), "registrationToken"));
        return result;
    }

    public DeregisterServiceRequest toDeregisterRequest(
            DdcServiceLeaseRequest value) {
        requireLeaseIdentity(value);
        return common.checked(DeregisterServiceRequest.newBuilder()
                .setServiceKey(common.toProto(value.getServiceKey()))
                .setInstanceId(value.getInstanceId())
                .setLeaseId(value.getLeaseId())
                .build());
    }

    public DdcServiceLeaseRequest fromDeregisterRequest(
            DeregisterServiceRequest value) {
        common.checked(value);
        return lease(
                value.hasServiceKey() ? common.fromProto(value.getServiceKey()) : null,
                value.getInstanceId(),
                value.getLeaseId()
        );
    }

    public top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcServiceInstance toProto(
            DdcServiceInstance value) {
        require(value, "service instance");
        var builder = top.egon.cola.component.rpc.ddc.contract.proto.v1
                .DdcServiceInstance.newBuilder()
                .setInstanceId(value.instanceId())
                .setLeaseId(value.leaseId())
                .setServiceKey(common.toProto(value.serviceKey()))
                .setHost(value.host())
                .setPort(value.port())
                .setSecure(value.secure())
                .putAllMetadata(common.validatedMetadata(value.metadata()))
                .setLeaseSeconds(value.leaseSeconds())
                .setHeartbeatIntervalSeconds(
                        value.heartbeatIntervalSeconds())
                .setRegisteredAt(common.toTimestamp(value.registeredAt()))
                .setLastHeartbeatAt(common.toTimestamp(value.lastHeartbeatAt()))
                .setLeaseExpireAt(common.toTimestamp(value.leaseExpireAt()))
                .setStatus(value.status())
                .setRevision(value.revision());
        if (value.resourceServerId() != null) {
            builder.setResourceServerId(value.resourceServerId());
            builder.setResourceVersion(value.resourceVersion());
            builder.setCredentialId(value.credentialId());
            builder.setAdmissionExpiresAt(common.toTimestamp(
                    value.admissionExpiresAt()));
        }
        return common.checked(builder.build());
    }

    public DdcServiceInstance fromProto(
            top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcServiceInstance value) {
        common.checked(value);
        if (!value.hasServiceKey()) {
            throw new IllegalArgumentException("serviceKey is required");
        }
        return new DdcServiceInstance(
                value.getInstanceId(),
                value.getLeaseId(),
                common.fromProto(value.getServiceKey()),
                value.getHost(),
                value.getPort(),
                value.getSecure(),
                common.validatedMetadata(value.getMetadataMap()),
                value.getLeaseSeconds(),
                value.getHeartbeatIntervalSeconds(),
                common.fromTimestamp(value.getRegisteredAt()),
                common.fromTimestamp(value.getLastHeartbeatAt()),
                common.fromTimestamp(value.getLeaseExpireAt()),
                value.getStatus(),
                value.getRevision(),
                value.hasResourceServerId() ? value.getResourceServerId() : null,
                value.hasResourceVersion() ? value.getResourceVersion() : null,
                value.hasCredentialId() ? value.getCredentialId() : null,
                value.hasAdmissionExpiresAt()
                        ? common.fromTimestamp(value.getAdmissionExpiresAt())
                        : null
        );
    }

    public GetServiceInstancesResponse toInstancesResponse(
            DdcServiceSnapshot value) {
        require(value, "service snapshot");
        var builder = GetServiceInstancesResponse.newBuilder()
                .setServiceKey(common.toProto(value.serviceKey()))
                .setRevision(value.revision());
        value.instances().forEach(instance -> builder.addInstances(toProto(instance)));
        if (value.observedAt() != null) {
            builder.setObservedAt(common.toTimestamp(value.observedAt()));
        }
        return common.checked(builder.build());
    }

    public DdcServiceSnapshot fromInstancesResponse(
            GetServiceInstancesResponse value) {
        common.checked(value);
        if (!value.hasServiceKey()) {
            throw new IllegalArgumentException("serviceKey is required");
        }
        return new DdcServiceSnapshot(
                common.fromProto(value.getServiceKey()),
                value.getRevision(),
                value.getInstancesList().stream().map(this::fromProto).toList(),
                value.hasObservedAt()
                        ? common.fromTimestamp(value.getObservedAt()) : null
        );
    }

    public GetServicesResponse toServicesResponse(
            DdcServiceCatalogSnapshot value) {
        require(value, "service catalog");
        var builder = GetServicesResponse.newBuilder()
                .setQuery(common.toProto(value.query()))
                .setRevision(value.revision());
        value.serviceKeys().forEach(key -> builder.addServiceKeys(common.toProto(key)));
        if (value.observedAt() != null) {
            builder.setObservedAt(common.toTimestamp(value.observedAt()));
        }
        return common.checked(builder.build());
    }

    public DdcServiceCatalogSnapshot fromServicesResponse(
            GetServicesResponse value) {
        common.checked(value);
        if (!value.hasQuery()) {
            throw new IllegalArgumentException("service query is required");
        }
        return new DdcServiceCatalogSnapshot(
                common.fromProto(value.getQuery()),
                value.getRevision(),
                value.getServiceKeysList().stream()
                        .map(common::fromProto).toList(),
                value.hasObservedAt()
                        ? common.fromTimestamp(value.getObservedAt()) : null
        );
    }

    private DdcServiceLeaseRequest lease(
            top.egon.cola.component.ddc.model.registry.DdcServiceKey serviceKey,
            String instanceId,
            String leaseId) {
        if (serviceKey == null) {
            throw new IllegalArgumentException("serviceKey is required");
        }
        DdcServiceLeaseRequest result = new DdcServiceLeaseRequest();
        result.setServiceKey(serviceKey);
        result.setInstanceId(instanceId);
        result.setLeaseId(leaseId);
        return result;
    }

    private static void requireLease(DdcServiceLeaseRequest value) {
        requireLeaseIdentity(value);
        DdcCommonProtoMapper.require(
                value.getRegistrationToken(), "registrationToken");
    }

    private static void requireLeaseIdentity(DdcServiceLeaseRequest value) {
        require(value, "lease request");
        require(value.getServiceKey(), "serviceKey");
        DdcCommonProtoMapper.require(value.getInstanceId(), "instanceId");
        DdcCommonProtoMapper.require(value.getLeaseId(), "leaseId");
    }

    private static void require(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
