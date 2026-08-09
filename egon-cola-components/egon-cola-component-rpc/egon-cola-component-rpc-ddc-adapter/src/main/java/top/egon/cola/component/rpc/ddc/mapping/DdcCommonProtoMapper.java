package top.egon.cola.component.rpc.ddc.mapping;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import top.egon.cola.component.ddc.format.ServiceInstanceMetaCodec;
import top.egon.cola.component.ddc.model.config.DdcAckStatus;
import top.egon.cola.component.ddc.model.config.DdcConfigFormat;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishStatus;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * DDC 共享领域类型与 protobuf 类型之间的显式转换和边界校验。
 * / Explicit conversion and boundary validation between shared DDC domain and
 * protobuf types.
 */
public final class DdcCommonProtoMapper {

    private static final int MAX_METADATA_ENTRIES = 32;
    private static final int MAX_METADATA_KEY_LENGTH = 64;
    private static final int MAX_METADATA_VALUE_LENGTH = 512;

    private final long maxMessageBytes;

    public DdcCommonProtoMapper(long maxMessageBytes) {
        if (maxMessageBytes <= 0) {
            throw new IllegalArgumentException("maxMessageBytes must be positive");
        }
        this.maxMessageBytes = maxMessageBytes;
    }

    public top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcScope toScope(
            String bizCode,
            String env,
            String appCode) {
        return top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcScope
                .newBuilder()
                .setBizCode(require(bizCode, "bizCode"))
                .setEnv(require(env, "env"))
                .setAppCode(require(appCode, "appCode"))
                .build();
    }

    public top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcLeaseSession toProto(
            top.egon.cola.component.ddc.model.lease.DdcLeaseSession value) {
        requireObject(value, "leaseSession");
        return checked(top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcLeaseSession
                .newBuilder()
                .setInstanceId(require(value.instanceId(), "instanceId"))
                .setLeaseId(require(value.leaseId(), "leaseId"))
                .setRole(toProto(value.role()))
                .setLeaseSeconds(value.leaseSeconds())
                .setHeartbeatIntervalSeconds(value.heartbeatIntervalSeconds())
                .setRegisteredAt(toTimestamp(value.registeredAt()))
                .setLeaseExpireAt(toTimestamp(value.leaseExpireAt()))
                .build());
    }

    public top.egon.cola.component.ddc.model.lease.DdcLeaseSession fromProto(
            top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcLeaseSession value) {
        checked(value);
        return new top.egon.cola.component.ddc.model.lease.DdcLeaseSession(
                require(value.getInstanceId(), "instanceId"),
                require(value.getLeaseId(), "leaseId"),
                fromProtoLeaseRole(value.getRole()),
                value.getLeaseSeconds(),
                value.getHeartbeatIntervalSeconds(),
                fromTimestamp(value.getRegisteredAt()),
                fromTimestamp(value.getLeaseExpireAt())
        );
    }

    public top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcLeaseOperationResult toProto(
            DdcLeaseOperationResult value) {
        requireObject(value, "leaseOperationResult");
        var builder = top.egon.cola.component.rpc.ddc.contract.proto.v1
                .DdcLeaseOperationResult.newBuilder()
                .setStatus(toProto(value.status()));
        if (value.leaseExpireAt() != null) {
            builder.setLeaseExpireAt(toTimestamp(value.leaseExpireAt()));
        }
        return checked(builder.build());
    }

    public DdcLeaseOperationResult fromProto(
            top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcLeaseOperationResult value) {
        checked(value);
        return new DdcLeaseOperationResult(
                fromProtoLeaseOperationStatus(value.getStatus()),
                value.hasLeaseExpireAt()
                        ? fromTimestamp(value.getLeaseExpireAt())
                        : null
        );
    }

    public top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcServiceKey toProto(
            DdcServiceKey value) {
        requireObject(value, "serviceKey");
        return checked(top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcServiceKey
                .newBuilder()
                .setScope(toScope(value.bizCode(), value.env(), value.appCode()))
                .setServiceId(value.serviceId())
                .setServiceKind(toProto(value.serviceKind()))
                .setServiceName(value.serviceName())
                .setGroup(value.group())
                .setVersion(value.version())
                .setProtocol(value.protocol())
                .build());
    }

    public DdcServiceKey fromProto(
            top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcServiceKey value) {
        checked(value);
        requireScope(value.hasScope(), "serviceKey.scope");
        return new DdcServiceKey(
                value.getScope().getBizCode(),
                value.getScope().getEnv(),
                value.getScope().getAppCode(),
                fromProtoServiceKind(value.getServiceKind()),
                value.getServiceName(),
                value.getGroup(),
                value.getVersion(),
                value.getProtocol()
        );
    }

    public top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcServiceQuery toProto(
            DdcServiceQuery value) {
        requireObject(value, "serviceQuery");
        var builder = top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcServiceQuery
                .newBuilder();
        set(builder::setBizCode, value.bizCode());
        set(builder::setEnv, value.env());
        set(builder::setAppCode, value.appCode());
        if (value.serviceKind() != null) {
            builder.setServiceKind(toProto(value.serviceKind()));
        }
        set(builder::setProtocol, value.protocol());
        set(builder::setServiceName, value.serviceName());
        set(builder::setGroup, value.group());
        set(builder::setVersion, value.version());
        return checked(builder.build());
    }

    public DdcServiceQuery fromProto(
            top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcServiceQuery value) {
        checked(value);
        return new DdcServiceQuery(
                value.hasBizCode() ? value.getBizCode() : null,
                value.hasEnv() ? value.getEnv() : null,
                value.hasAppCode() ? value.getAppCode() : null,
                value.hasServiceKind()
                        ? fromProtoServiceKind(value.getServiceKind())
                        : null,
                value.hasProtocol() ? value.getProtocol() : null,
                value.hasServiceName() ? value.getServiceName() : null,
                value.hasGroup() ? value.getGroup() : null,
                value.hasVersion() ? value.getVersion() : null
        );
    }

    public top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcLeaseRole toProto(
            DdcLeaseRole value) {
        requireObject(value, "lease role");
        return switch (value) {
            case CONFIG_CLIENT -> top.egon.cola.component.rpc.ddc.contract.proto.v1
                    .DdcLeaseRole.DDC_LEASE_ROLE_CONFIG_CLIENT;
            case RPC_PROVIDER -> top.egon.cola.component.rpc.ddc.contract.proto.v1
                    .DdcLeaseRole.DDC_LEASE_ROLE_RPC_PROVIDER;
            case HTTP_PROVIDER -> top.egon.cola.component.rpc.ddc.contract.proto.v1
                    .DdcLeaseRole.DDC_LEASE_ROLE_HTTP_PROVIDER;
            case INTERNAL_GATEWAY -> top.egon.cola.component.rpc.ddc.contract.proto.v1
                    .DdcLeaseRole.DDC_LEASE_ROLE_INTERNAL_GATEWAY;
        };
    }

    public DdcLeaseRole fromProtoLeaseRole(
            top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcLeaseRole value) {
        return switch (value) {
            case DDC_LEASE_ROLE_CONFIG_CLIENT -> DdcLeaseRole.CONFIG_CLIENT;
            case DDC_LEASE_ROLE_RPC_PROVIDER -> DdcLeaseRole.RPC_PROVIDER;
            case DDC_LEASE_ROLE_HTTP_PROVIDER -> DdcLeaseRole.HTTP_PROVIDER;
            case DDC_LEASE_ROLE_INTERNAL_GATEWAY -> DdcLeaseRole.INTERNAL_GATEWAY;
            default -> throw unknown("lease role", value);
        };
    }

    public top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcLeaseOperationStatus toProto(
            DdcLeaseOperationStatus value) {
        requireObject(value, "lease operation status");
        return switch (value) {
            case RENEWED -> top.egon.cola.component.rpc.ddc.contract.proto.v1
                    .DdcLeaseOperationStatus.DDC_LEASE_OPERATION_STATUS_RENEWED;
            case DELETED -> top.egon.cola.component.rpc.ddc.contract.proto.v1
                    .DdcLeaseOperationStatus.DDC_LEASE_OPERATION_STATUS_DELETED;
            case NOT_FOUND -> top.egon.cola.component.rpc.ddc.contract.proto.v1
                    .DdcLeaseOperationStatus.DDC_LEASE_OPERATION_STATUS_NOT_FOUND;
            case LEASE_MISMATCH -> top.egon.cola.component.rpc.ddc.contract.proto.v1
                    .DdcLeaseOperationStatus.DDC_LEASE_OPERATION_STATUS_LEASE_MISMATCH;
            case NOT_DELETED -> top.egon.cola.component.rpc.ddc.contract.proto.v1
                    .DdcLeaseOperationStatus.DDC_LEASE_OPERATION_STATUS_NOT_DELETED;
        };
    }

    public DdcLeaseOperationStatus fromProtoLeaseOperationStatus(
            top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcLeaseOperationStatus value) {
        return switch (value) {
            case DDC_LEASE_OPERATION_STATUS_RENEWED -> DdcLeaseOperationStatus.RENEWED;
            case DDC_LEASE_OPERATION_STATUS_DELETED -> DdcLeaseOperationStatus.DELETED;
            case DDC_LEASE_OPERATION_STATUS_NOT_FOUND -> DdcLeaseOperationStatus.NOT_FOUND;
            case DDC_LEASE_OPERATION_STATUS_LEASE_MISMATCH -> DdcLeaseOperationStatus.LEASE_MISMATCH;
            case DDC_LEASE_OPERATION_STATUS_NOT_DELETED -> DdcLeaseOperationStatus.NOT_DELETED;
            default -> throw unknown("lease operation status", value);
        };
    }

    public top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcServiceKind toProto(
            DdcServiceKind value) {
        requireObject(value, "service kind");
        return switch (value) {
            case RPC_PROVIDER -> top.egon.cola.component.rpc.ddc.contract.proto.v1
                    .DdcServiceKind.DDC_SERVICE_KIND_RPC_PROVIDER;
            case HTTP_PROVIDER -> top.egon.cola.component.rpc.ddc.contract.proto.v1
                    .DdcServiceKind.DDC_SERVICE_KIND_HTTP_PROVIDER;
            case INTERNAL_GATEWAY -> top.egon.cola.component.rpc.ddc.contract.proto.v1
                    .DdcServiceKind.DDC_SERVICE_KIND_INTERNAL_GATEWAY;
        };
    }

    public DdcServiceKind fromProtoServiceKind(
            top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcServiceKind value) {
        return switch (value) {
            case DDC_SERVICE_KIND_RPC_PROVIDER -> DdcServiceKind.RPC_PROVIDER;
            case DDC_SERVICE_KIND_HTTP_PROVIDER -> DdcServiceKind.HTTP_PROVIDER;
            case DDC_SERVICE_KIND_INTERNAL_GATEWAY -> DdcServiceKind.INTERNAL_GATEWAY;
            default -> throw unknown("service kind", value);
        };
    }

    public top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcAckStatus toProto(
            DdcAckStatus value) {
        requireObject(value, "ack status");
        return switch (value) {
            case SUCCESS -> top.egon.cola.component.rpc.ddc.contract.proto.v1
                    .DdcAckStatus.DDC_ACK_STATUS_SUCCESS;
            case FAILED -> top.egon.cola.component.rpc.ddc.contract.proto.v1
                    .DdcAckStatus.DDC_ACK_STATUS_FAILED;
            case IGNORED -> top.egon.cola.component.rpc.ddc.contract.proto.v1
                    .DdcAckStatus.DDC_ACK_STATUS_IGNORED;
            case TIMEOUT -> top.egon.cola.component.rpc.ddc.contract.proto.v1
                    .DdcAckStatus.DDC_ACK_STATUS_TIMEOUT;
        };
    }

    public DdcAckStatus fromProtoAckStatus(
            top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcAckStatus value) {
        return switch (value) {
            case DDC_ACK_STATUS_SUCCESS -> DdcAckStatus.SUCCESS;
            case DDC_ACK_STATUS_FAILED -> DdcAckStatus.FAILED;
            case DDC_ACK_STATUS_IGNORED -> DdcAckStatus.IGNORED;
            case DDC_ACK_STATUS_TIMEOUT -> DdcAckStatus.TIMEOUT;
            default -> throw unknown("ack status", value);
        };
    }

    public top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcAckStatus toProtoAckStatus(
            String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ack status is required");
        }
        if ("PENDING".equalsIgnoreCase(value)) {
            return top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcAckStatus
                    .DDC_ACK_STATUS_PENDING;
        }
        return toProto(DdcAckStatus.valueOf(value.toUpperCase(java.util.Locale.ROOT)));
    }

    public String fromProtoAckStatusText(
            top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcAckStatus value) {
        if (value == top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcAckStatus
                .DDC_ACK_STATUS_PENDING) {
            return "PENDING";
        }
        return fromProtoAckStatus(value).name();
    }

    public top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcConfigFormat toProtoFormat(
            String value) {
        try {
            DdcConfigFormat format = DdcConfigFormat.valueOf(
                    require(value, "config format").toUpperCase(java.util.Locale.ROOT));
            return switch (format) {
                case YAML -> top.egon.cola.component.rpc.ddc.contract.proto.v1
                        .DdcConfigFormat.DDC_CONFIG_FORMAT_YAML;
            };
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported config format: " + value, exception);
        }
    }

    public String fromProtoFormat(
            top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcConfigFormat value) {
        return switch (value) {
            case DDC_CONFIG_FORMAT_YAML -> DdcConfigFormat.YAML.name();
            default -> throw unknown("config format", value);
        };
    }

    public top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcPublishStatus toProto(
            DdcManagementPublishStatus value) {
        requireObject(value, "publish status");
        return top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcPublishStatus
                .valueOf("DDC_PUBLISH_STATUS_" + value.name());
    }

    public DdcManagementPublishStatus fromProtoPublishStatus(
            top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcPublishStatus value) {
        if (value == top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcPublishStatus
                .DDC_PUBLISH_STATUS_UNSPECIFIED
                || value == top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcPublishStatus
                .UNRECOGNIZED) {
            throw unknown("publish status", value);
        }
        return DdcManagementPublishStatus.valueOf(
                value.name().substring("DDC_PUBLISH_STATUS_".length()));
    }

    public Timestamp toTimestamp(Instant value) {
        requireObject(value, "timestamp");
        return Timestamp.newBuilder()
                .setSeconds(value.getEpochSecond())
                .setNanos(value.getNano())
                .build();
    }

    public Instant fromTimestamp(Timestamp value) {
        requireObject(value, "timestamp");
        try {
            return Instant.ofEpochSecond(value.getSeconds(), value.getNanos());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid protobuf timestamp", exception);
        }
    }

    public Map<String, String> validatedMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        long businessEntries = metadata.keySet().stream()
                .filter(key -> key == null
                        || !ServiceInstanceMetaCodec.isReservedKey(key))
                .count();
        if (businessEntries > MAX_METADATA_ENTRIES) {
            throw new IllegalArgumentException(
                    "metadata must contain at most " + MAX_METADATA_ENTRIES + " entries");
        }
        long reservedEntries = metadata.size() - businessEntries;
        if (reservedEntries > ServiceInstanceMetaCodec.RESERVED_KEY_COUNT) {
            throw new IllegalArgumentException(
                    "metadata contains too many reserved entries");
        }
        TreeMap<String, String> copy = new TreeMap<>();
        metadata.forEach((key, value) -> {
            String checkedKey = require(key, "metadata key");
            String checkedValue = value == null ? "" : value;
            if (checkedKey.length() > MAX_METADATA_KEY_LENGTH) {
                throw new IllegalArgumentException("metadata key is too long");
            }
            if (checkedValue.length() > MAX_METADATA_VALUE_LENGTH) {
                throw new IllegalArgumentException("metadata value is too long");
            }
            copy.put(checkedKey, checkedValue);
        });
        return Collections.unmodifiableMap(copy);
    }

    public void validateConfigContent(String content, long maxConfigBytes) {
        requireObject(content, "config content");
        if (content.getBytes(StandardCharsets.UTF_8).length > maxConfigBytes) {
            throw new IllegalArgumentException(
                    "config content exceeds " + maxConfigBytes + " bytes");
        }
    }

    public <T extends Message> T checked(T message) {
        requireObject(message, "message");
        if (message.getSerializedSize() > maxMessageBytes) {
            throw new IllegalArgumentException(
                    "DDC RPC message exceeds " + maxMessageBytes + " bytes");
        }
        return message;
    }

    public static String require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private static void requireScope(boolean present, String name) {
        if (!present) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    private static void requireObject(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    private static IllegalArgumentException unknown(String name, Object value) {
        return new IllegalArgumentException("Unknown " + name + ": " + value);
    }

    private static void set(
            java.util.function.Consumer<String> setter,
            String value) {
        if (value != null) {
            setter.accept(value);
        }
    }
}
