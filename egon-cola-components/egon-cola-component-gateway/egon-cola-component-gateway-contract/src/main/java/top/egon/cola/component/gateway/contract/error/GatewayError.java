package top.egon.cola.component.gateway.contract.error;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Safe public error contract shared by HTTP and RPC transports.
 */
public record GatewayError(
        String code,
        GatewayErrorCategory category,
        String message,
        String traceId,
        boolean retryable,
        Map<String, String> details
) {

    public GatewayError {
        code = required(code, "code");
        category = Objects.requireNonNull(category, "category");
        message = required(message, "message");
        traceId = required(traceId, "traceId");
        details = immutableDetails(details);
    }

    public static GatewayError internal(String traceId) {
        return new GatewayError(
                "GATEWAY_INTERNAL_ERROR",
                GatewayErrorCategory.INTERNAL_ERROR,
                "Gateway request failed",
                traceId,
                false,
                Map.of()
        );
    }

    private static Map<String, String> immutableDetails(
            Map<String, String> details) {
        if (details == null || details.isEmpty()) {
            return Map.of();
        }
        Map<String, String> copy = new LinkedHashMap<>();
        details.forEach((key, value) -> {
            if (key == null || value == null) {
                throw new IllegalArgumentException(
                        "error detail keys and values must not be null"
                );
            }
            copy.put(key, value);
        });
        return Map.copyOf(copy);
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
