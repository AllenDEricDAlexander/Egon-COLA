package top.egon.cola.component.gateway.admin.rule;

import java.time.Duration;
import java.util.UUID;

public record GatewayDdcPublicationCommand(
        String appCode,
        String env,
        String namespace,
        String configKey,
        String value,
        Long expectedVersion,
        String changeId,
        String operator,
        Duration timeout
) {

    public GatewayDdcPublicationCommand {
        appCode = required(appCode, "appCode");
        env = required(env, "env");
        namespace = required(namespace, "namespace");
        configKey = required(configKey, "configKey");
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        if (expectedVersion == null || expectedVersion < 0) {
            throw new IllegalArgumentException(
                    "expectedVersion must not be null or negative"
            );
        }
        changeId = required(changeId, "changeId");
        requireUuidV7(changeId);
        operator = required(operator, "operator");
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }
        return value.trim();
    }

    private static void requireUuidV7(String value) {
        try {
            UUID uuid = UUID.fromString(canonicalUuid(value));
            if (uuid.version() != 7) {
                throw new IllegalArgumentException(
                        "changeId must be a UUIDv7"
                );
            }
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage() != null
                    && exception.getMessage().contains("UUIDv7")) {
                throw exception;
            }
            throw new IllegalArgumentException(
                    "changeId must be a UUIDv7",
                    exception
            );
        }
    }

    private static String canonicalUuid(String value) {
        if (value.length() != 32) {
            return value;
        }
        return value.substring(0, 8)
                + "-" + value.substring(8, 12)
                + "-" + value.substring(12, 16)
                + "-" + value.substring(16, 20)
                + "-" + value.substring(20);
    }
}
