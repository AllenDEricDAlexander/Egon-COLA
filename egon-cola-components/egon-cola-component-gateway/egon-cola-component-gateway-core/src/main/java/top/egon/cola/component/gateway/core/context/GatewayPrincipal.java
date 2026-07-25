package top.egon.cola.component.gateway.core.context;

import java.util.LinkedHashMap;
import java.util.Map;

public record GatewayPrincipal(
        String principalId,
        String principalType,
        String tenantId,
        String displayName,
        boolean authenticated,
        Map<String, String> attributes
) {

    public GatewayPrincipal {
        principalId = required(principalId, "principalId");
        principalType = required(principalType, "principalType");
        tenantId = optional(tenantId);
        displayName = optional(displayName);
        bounded(principalId, 256, "principalId");
        bounded(principalType, 64, "principalType");
        bounded(tenantId, 256, "tenantId");
        bounded(displayName, 256, "displayName");
        attributes = immutableMap(attributes, "principal attribute");
    }

    public GatewayPrincipal(
            String principalId,
            Map<String, String> attributes) {
        this(
                principalId,
                "USER",
                null,
                null,
                true,
                attributes
        );
    }

    public static GatewayPrincipal anonymous() {
        return new GatewayPrincipal(
                "ANONYMOUS",
                "ANONYMOUS",
                null,
                null,
                false,
                Map.of()
        );
    }

    private static Map<String, String> immutableMap(
            Map<String, String> source,
            String fieldName) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        if (source.size() > 32) {
            throw new IllegalArgumentException(
                    fieldName + " count exceeds 32"
            );
        }
        Map<String, String> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key == null || value == null) {
                throw new IllegalArgumentException(
                        fieldName + " keys and values must not be null"
                );
            }
            bounded(key, 64, fieldName + " key");
            bounded(value, 512, fieldName + " value");
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

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void bounded(
            String value,
            int maximum,
            String fieldName) {
        if (value != null && value.length() > maximum) {
            throw new IllegalArgumentException(
                    fieldName + " exceeds " + maximum
            );
        }
    }
}
