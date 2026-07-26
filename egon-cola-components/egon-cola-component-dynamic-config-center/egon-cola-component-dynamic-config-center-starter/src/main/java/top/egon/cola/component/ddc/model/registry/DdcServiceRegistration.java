package top.egon.cola.component.ddc.model.registry;

import top.egon.cola.component.ddc.management.model.ServiceInstanceMetaCodec;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

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

    /** Entries an instance may use for its own metadata, excluding the reserved namespace. */
    public static final int MAX_BUSINESS_METADATA_ENTRIES = 32;

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

    private static boolean validRpcFrameworkMetadata(String key, String value) {
        return switch (key) {
            case "egon.rpc.transport" -> "grpc".equals(value);
            case "egon.rpc.serialization" -> "protobuf".equals(value);
            case "egon.rpc.runtime-version" -> !value.isBlank();
            default -> false;
        };
    }

    private static String require(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }
}
