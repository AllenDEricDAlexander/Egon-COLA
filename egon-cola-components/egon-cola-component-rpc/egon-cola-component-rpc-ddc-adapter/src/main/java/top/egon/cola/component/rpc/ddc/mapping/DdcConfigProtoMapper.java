package top.egon.cola.component.rpc.ddc.mapping;

import top.egon.cola.component.ddc.model.config.DdcAckRequest;
import top.egon.cola.component.ddc.model.config.DdcConfigValue;
import top.egon.cola.component.ddc.model.config.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.config.DdcInstanceRegisterRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.AcknowledgePublishRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcConfig;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.HeartbeatConfigClientRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.OfflineConfigClientRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.PullConfigRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.PullConfigResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.RegisterConfigClientRequest;

import java.time.Instant;
import java.util.List;

/**
 * DDC 配置运行时 Port 模型与 protobuf 请求响应之间的显式映射。
 * / Explicit mapping between DDC configuration-runtime Port models and
 * protobuf requests and responses.
 */
public final class DdcConfigProtoMapper {

    private final DdcCommonProtoMapper common;
    private final long maxConfigBytes;

    public DdcConfigProtoMapper(
            DdcCommonProtoMapper common,
            long maxConfigBytes) {
        if (common == null || maxConfigBytes <= 0) {
            throw new IllegalArgumentException(
                    "common mapper and positive maxConfigBytes are required");
        }
        this.common = common;
        this.maxConfigBytes = maxConfigBytes;
    }

    public RegisterConfigClientRequest toRegisterRequest(
            DdcInstanceRegisterRequest value) {
        require(value, "registration");
        var builder = RegisterConfigClientRequest.newBuilder()
                .setScope(common.toScope(
                        value.getBizCode(), value.getEnv(), value.getAppCode()))
                .setInstanceId(DdcCommonProtoMapper.require(
                        value.getInstanceId(), "instanceId"))
                .setHost(DdcCommonProtoMapper.require(value.getHost(), "host"))
                .setPid(DdcCommonProtoMapper.require(value.getPid(), "pid"))
                .setSdkVersion(DdcCommonProtoMapper.require(
                        value.getSdkVersion(), "sdkVersion"))
                .setLeaseSeconds(value.getLeaseSeconds())
                .setHeartbeatIntervalSeconds(
                        value.getHeartbeatIntervalSeconds())
                .putAllMetadata(common.validatedMetadata(value.getMetadata()))
                .setAdmissionTicket(DdcCommonProtoMapper.require(
                        value.getAdmissionTicket(), "admissionTicket"));
        if (value.getPort() != null) {
            builder.setPort(value.getPort());
        }
        return common.checked(builder.build());
    }

    public DdcInstanceRegisterRequest fromRegisterRequest(
            RegisterConfigClientRequest value) {
        common.checked(value);
        scopeRequired(value.hasScope());
        DdcInstanceRegisterRequest result = new DdcInstanceRegisterRequest();
        result.setInstanceId(value.getInstanceId());
        result.setBizCode(value.getScope().getBizCode());
        result.setEnv(value.getScope().getEnv());
        result.setAppCode(value.getScope().getAppCode());
        result.setHost(value.getHost());
        result.setPort(value.hasPort() ? value.getPort() : null);
        result.setPid(value.getPid());
        result.setSdkVersion(value.getSdkVersion());
        result.setLeaseSeconds(value.getLeaseSeconds());
        result.setHeartbeatIntervalSeconds(value.getHeartbeatIntervalSeconds());
        result.setMetadata(common.validatedMetadata(value.getMetadataMap()));
        result.setAdmissionTicket(value.getAdmissionTicket());
        return result;
    }

    public HeartbeatConfigClientRequest toHeartbeatRequest(
            DdcHeartbeatRequest value) {
        require(value, "heartbeat");
        var builder = HeartbeatConfigClientRequest.newBuilder()
                .setScope(common.toScope(
                        value.getBizCode(), value.getEnv(), value.getAppCode()))
                .setInstanceId(DdcCommonProtoMapper.require(
                        value.getInstanceId(), "instanceId"))
                .setLeaseId(DdcCommonProtoMapper.require(
                        value.getLeaseId(), "leaseId"))
                .setHost(DdcCommonProtoMapper.require(value.getHost(), "host"))
                .setPid(DdcCommonProtoMapper.require(value.getPid(), "pid"))
                .setSdkVersion(DdcCommonProtoMapper.require(
                        value.getSdkVersion(), "sdkVersion"))
                .putAllMetadata(common.validatedMetadata(value.getMetadata()))
                .setAdmissionTicket(DdcCommonProtoMapper.require(
                        value.getAdmissionTicket(), "admissionTicket"));
        if (value.getPort() != null) {
            builder.setPort(value.getPort());
        }
        return common.checked(builder.build());
    }

    public DdcHeartbeatRequest fromHeartbeatRequest(
            HeartbeatConfigClientRequest value) {
        common.checked(value);
        scopeRequired(value.hasScope());
        DdcHeartbeatRequest result = scopeHeartbeat(
                value.getScope().getBizCode(),
                value.getScope().getEnv(),
                value.getScope().getAppCode(),
                value.getInstanceId(),
                value.getLeaseId()
        );
        result.setHost(value.getHost());
        result.setPort(value.hasPort() ? value.getPort() : null);
        result.setPid(value.getPid());
        result.setSdkVersion(value.getSdkVersion());
        result.setMetadata(common.validatedMetadata(value.getMetadataMap()));
        result.setAdmissionTicket(value.getAdmissionTicket());
        return result;
    }

    public OfflineConfigClientRequest toOfflineRequest(DdcHeartbeatRequest value) {
        require(value, "offline request");
        return common.checked(OfflineConfigClientRequest.newBuilder()
                .setScope(common.toScope(
                        value.getBizCode(), value.getEnv(), value.getAppCode()))
                .setInstanceId(DdcCommonProtoMapper.require(
                        value.getInstanceId(), "instanceId"))
                .setLeaseId(DdcCommonProtoMapper.require(
                        value.getLeaseId(), "leaseId"))
                .build());
    }

    public DdcHeartbeatRequest fromOfflineRequest(OfflineConfigClientRequest value) {
        common.checked(value);
        scopeRequired(value.hasScope());
        return scopeHeartbeat(
                value.getScope().getBizCode(),
                value.getScope().getEnv(),
                value.getScope().getAppCode(),
                value.getInstanceId(),
                value.getLeaseId()
        );
    }

    public PullConfigRequest toPullRequest(
            String bizCode,
            String env,
            String appCode) {
        return common.checked(PullConfigRequest.newBuilder()
                .setScope(common.toScope(bizCode, env, appCode))
                .build());
    }

    public DdcConfig toConfig(DdcConfigValue value) {
        require(value, "config");
        common.validateConfigContent(value.getContent(), maxConfigBytes);
        var builder = DdcConfig.newBuilder()
                .setResourceName(DdcCommonProtoMapper.require(
                        value.getResourceName(), "resourceName"))
                .setContent(value.getContent())
                .setFormat(common.toProtoFormat(value.getFormat()));
        if (value.getVersion() != null) {
            builder.setVersion(value.getVersion());
        }
        return common.checked(builder.build());
    }

    public DdcConfigValue fromConfig(DdcConfig value) {
        common.checked(value);
        common.validateConfigContent(value.getContent(), maxConfigBytes);
        DdcConfigValue result = new DdcConfigValue();
        result.setResourceName(DdcCommonProtoMapper.require(
                value.getResourceName(), "resourceName"));
        result.setContent(value.getContent());
        result.setFormat(common.fromProtoFormat(value.getFormat()));
        result.setVersion(value.hasVersion() ? value.getVersion() : null);
        return result;
    }

    public PullConfigResponse toPullResponse(List<DdcConfigValue> values) {
        var builder = PullConfigResponse.newBuilder();
        if (values != null) {
            values.forEach(value -> builder.addConfigs(toConfig(value)));
        }
        return common.checked(builder.build());
    }

    public List<DdcConfigValue> fromPullResponse(PullConfigResponse value) {
        common.checked(value);
        return value.getConfigsList().stream().map(this::fromConfig).toList();
    }

    public AcknowledgePublishRequest toAcknowledgeRequest(DdcAckRequest value) {
        require(value, "acknowledgement");
        var builder = AcknowledgePublishRequest.newBuilder()
                .setChangeId(DdcCommonProtoMapper.require(
                        value.getChangeId(), "changeId"))
                .setScope(common.toScope(
                        value.getBizCode(), value.getEnv(), value.getAppCode()))
                .setInstanceId(DdcCommonProtoMapper.require(
                        value.getInstanceId(), "instanceId"))
                .setLeaseId(DdcCommonProtoMapper.require(
                        value.getLeaseId(), "leaseId"))
                .setResourceName(DdcCommonProtoMapper.require(
                        value.getResourceName(), "resourceName"))
                .setStatus(common.toProto(value.getStatus()));
        if (value.getTargetVersion() != null) {
            builder.setTargetVersion(value.getTargetVersion());
        }
        if (value.getCurrentVersion() != null) {
            builder.setCurrentVersion(value.getCurrentVersion());
        }
        set(builder::setResourceChecksum, value.getResourceChecksum());
        set(builder::setErrorMessage, value.getErrorMessage());
        if (value.getAckTime() != null) {
            builder.setAcknowledgedAt(common.toTimestamp(
                    Instant.ofEpochMilli(value.getAckTime())));
        }
        return common.checked(builder.build());
    }

    public DdcAckRequest fromAcknowledgeRequest(AcknowledgePublishRequest value) {
        common.checked(value);
        scopeRequired(value.hasScope());
        DdcAckRequest result = new DdcAckRequest();
        result.setChangeId(value.getChangeId());
        result.setBizCode(value.getScope().getBizCode());
        result.setEnv(value.getScope().getEnv());
        result.setAppCode(value.getScope().getAppCode());
        result.setInstanceId(value.getInstanceId());
        result.setLeaseId(value.getLeaseId());
        result.setResourceName(value.getResourceName());
        result.setTargetVersion(value.hasTargetVersion()
                ? value.getTargetVersion() : null);
        result.setCurrentVersion(value.hasCurrentVersion()
                ? value.getCurrentVersion() : null);
        result.setResourceChecksum(value.hasResourceChecksum()
                ? value.getResourceChecksum() : null);
        result.setStatus(common.fromProtoAckStatus(value.getStatus()));
        result.setErrorMessage(value.hasErrorMessage()
                ? value.getErrorMessage() : null);
        result.setAckTime(value.hasAcknowledgedAt()
                ? common.fromTimestamp(value.getAcknowledgedAt()).toEpochMilli()
                : null);
        return result;
    }

    private DdcHeartbeatRequest scopeHeartbeat(
            String bizCode,
            String env,
            String appCode,
            String instanceId,
            String leaseId) {
        DdcHeartbeatRequest result = new DdcHeartbeatRequest();
        result.setBizCode(bizCode);
        result.setEnv(env);
        result.setAppCode(appCode);
        result.setInstanceId(instanceId);
        result.setLeaseId(leaseId);
        return result;
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
