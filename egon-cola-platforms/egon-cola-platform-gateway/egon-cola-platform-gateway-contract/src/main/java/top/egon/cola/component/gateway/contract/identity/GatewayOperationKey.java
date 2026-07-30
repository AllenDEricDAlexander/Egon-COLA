package top.egon.cola.component.gateway.contract.identity;

import java.util.Locale;
import java.util.Objects;

/**
 * Stable protocol identity used for definition idempotency and route references.
 */
public final class GatewayOperationKey {

    private final String value;

    private GatewayOperationKey(String value) {
        this.value = value;
    }

    public static GatewayOperationKey http(String applicationCode,
                                           String method,
                                           String path) {
        String normalizedApplication = required(applicationCode, "applicationCode");
        String normalizedMethod = required(method, "method")
                .toUpperCase(Locale.ROOT);
        String normalizedPath = normalizePath(path);
        return new GatewayOperationKey(String.join(
                ":",
                normalizedApplication,
                "http",
                normalizedMethod,
                normalizedPath
        ));
    }

    public static GatewayOperationKey rpc(String applicationCode,
                                          String serviceName,
                                          String group,
                                          String version,
                                          String fullMethodName) {
        return new GatewayOperationKey(String.join(
                ":",
                required(applicationCode, "applicationCode"),
                "rpc",
                required(serviceName, "serviceName"),
                required(group, "group"),
                required(version, "version"),
                required(fullMethodName, "fullMethodName")
        ));
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object candidate) {
        if (this == candidate) {
            return true;
        }
        if (!(candidate instanceof GatewayOperationKey that)) {
            return false;
        }
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }

    private static String normalizePath(String path) {
        String normalized = required(path, "path");
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized.replaceAll("/{2,}", "/");
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
