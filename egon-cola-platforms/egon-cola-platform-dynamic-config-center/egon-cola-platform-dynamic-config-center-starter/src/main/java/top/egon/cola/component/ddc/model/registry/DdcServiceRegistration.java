package top.egon.cola.component.ddc.model.registry;

import top.egon.cola.component.ddc.management.model.ServiceInstanceMetaCodec;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * 服务实例注册请求，包含物理服务身份、端点和租约参数。
 * / Service instance registration request containing physical service identity,
 * endpoint, and lease settings.
 *
 * @param instanceId 实例标识 / instance identifier
 * @param serviceKey 服务键 / service key
 * @param host 实例主机地址 / instance host address
 * @param port 服务端口 / service port
 * @param secure 是否使用安全传输 / whether secure transport is used
 * @param metadata 不可变的实例元数据 / immutable instance metadata
 * @param leaseSeconds 租约有效期秒数 / lease duration in seconds
 * @param heartbeatIntervalSeconds 心跳间隔秒数 / heartbeat interval in seconds
 */
public record DdcServiceRegistration(
        String instanceId,
        DdcServiceKey serviceKey,
        String host,
        int port,
        boolean secure,
        Map<String, String> metadata,
        int leaseSeconds,
        int heartbeatIntervalSeconds
) {

    /**
     * 校验并规范化注册信息。
     * / Validates and normalizes the registration.
     *
     * @throws IllegalArgumentException 必填值、端口、协议、元数据或租约参数无效时抛出
     * / if required values, port, protocol, metadata, or lease settings are invalid
     */
    public DdcServiceRegistration {
        instanceId = require(instanceId, "instanceId");
        if (serviceKey == null) {
            throw new IllegalArgumentException("serviceKey is required");
        }
        host = require(host, "host");
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        if (serviceKey.serviceKind() == DdcServiceKind.HTTP_PROVIDER
                && secure != "https".equals(serviceKey.protocol())) {
            throw new IllegalArgumentException(
                    "HTTP_PROVIDER secure flag must match its protocol"
            );
        }
        metadata = validatedMetadata(metadata);
        if (leaseSeconds <= 0) {
            throw new IllegalArgumentException("leaseSeconds must be positive");
        }
        if (heartbeatIntervalSeconds <= 0 || heartbeatIntervalSeconds >= leaseSeconds) {
            throw new IllegalArgumentException(
                    "heartbeatIntervalSeconds must be positive and less than leaseSeconds"
            );
        }
    }

    /**
     * 单个实例可使用的业务元数据条目上限，不包含保留命名空间。
     * / Maximum business metadata entries an instance may use, excluding the reserved namespace.
     */
    public static final int MAX_BUSINESS_METADATA_ENTRIES = 32;

    /**
     * 校验、排序并冻结实例元数据。
     * / Validates, sorts, and freezes instance metadata.
     *
     * @param metadata 待校验元数据，可为空 / metadata to validate, nullable
     * @return 不可变且按键排序的元数据 / immutable metadata sorted by key
     * @throws IllegalArgumentException 条目数量、键、值或敏感信息规则不满足时抛出
     * / if entry counts, keys, values, or sensitive-information rules are violated
     */
    static Map<String, String> validatedMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        // Structured gateway.* keys are counted separately. Folding them into the same budget
        // would mean adopting typed instance metadata silently shrinks the caller's allowance,
        // so a registration that fits today could start failing purely because the platform
        // began reporting health and warm-up. The business allowance stays at 32 either way.
        long businessEntries = metadata.keySet().stream()
                .filter(key -> !ServiceInstanceMetaCodec.isReservedKey(key))
                .count();
        if (businessEntries > MAX_BUSINESS_METADATA_ENTRIES) {
            throw new IllegalArgumentException(
                    "metadata must contain at most " + MAX_BUSINESS_METADATA_ENTRIES
                            + " non-reserved entries");
        }
        long reservedEntries = metadata.size() - businessEntries;
        if (reservedEntries > ServiceInstanceMetaCodec.RESERVED_KEY_COUNT) {
            throw new IllegalArgumentException(
                    "metadata must contain at most " + ServiceInstanceMetaCodec.RESERVED_KEY_COUNT
                            + " reserved " + ServiceInstanceMetaCodec.PREFIX + "* entries");
        }
        TreeMap<String, String> copy = new TreeMap<>();
        metadata.forEach((key, value) -> {
            String normalizedKey = require(key, "metadata key");
            String normalizedValue = value == null ? "" : value;
            if (normalizedKey.length() > 64) {
                throw new IllegalArgumentException("metadata key must not exceed 64 characters");
            }
            if (normalizedValue.length() > 512) {
                throw new IllegalArgumentException("metadata value must not exceed 512 characters");
            }
            String lowerKey = normalizedKey.toLowerCase(Locale.ROOT);
            if (lowerKey.startsWith("ddc.")
                    || lowerKey.startsWith("egon.internal.")
                    || lowerKey.startsWith("egon.rpc.")
                    && !validRpcFrameworkMetadata(lowerKey, normalizedValue)) {
                throw new IllegalArgumentException("metadata key uses a reserved prefix");
            }
            if (lowerKey.contains("password")
                    || lowerKey.contains("secret")
                    || lowerKey.contains("token")
                    || lowerKey.contains("privatekey")
                    || lowerKey.contains("private-key")
                    || lowerKey.contains("certificate")) {
                throw new IllegalArgumentException("metadata key may expose sensitive information");
            }
            copy.put(normalizedKey, normalizedValue);
        });
        return Collections.unmodifiableMap(copy);
    }

    /**
     * 判断保留的 RPC 框架元数据键值是否有效。
     * / Determines whether a reserved RPC framework metadata entry is valid.
     *
     * @param key 已规范化为小写的元数据键 / metadata key normalized to lower case
     * @param value 元数据值 / metadata value
     * @return 键值组合受支持时为 {@code true} / {@code true} when the key-value pair is supported
     */
    private static boolean validRpcFrameworkMetadata(String key, String value) {
        return switch (key) {
            case "egon.rpc.transport" -> "grpc".equals(value);
            case "egon.rpc.serialization" -> "protobuf".equals(value);
            case "egon.rpc.runtime-version" -> !value.isBlank();
            default -> false;
        };
    }

    /**
     * 校验必填字符串。
     * / Validates a required string.
     *
     * @param value 待校验值 / value to validate
     * @param fieldName 用于错误消息的字段名 / field name used in the error message
     * @return 原始非空值 / original non-blank value
     * @throws IllegalArgumentException 值为空或空白时抛出 / if the value is {@code null} or blank
     */
    private static String require(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }
}
