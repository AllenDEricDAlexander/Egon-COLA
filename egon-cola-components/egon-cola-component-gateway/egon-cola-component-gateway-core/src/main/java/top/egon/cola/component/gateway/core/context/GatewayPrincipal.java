package top.egon.cola.component.gateway.core.context;

import java.util.LinkedHashMap;
import java.util.Map;

public record GatewayPrincipal(
        String principalId,
        Map<String, String> attributes
) {

    public GatewayPrincipal {
        principalId = required(principalId, "principalId");
        attributes = immutableMap(attributes, "principal attribute");
    }

    private static Map<String, String> immutableMap(
            Map<String, String> source,
            String fieldName) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, String> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key == null || value == null) {
                throw new IllegalArgumentException(
                        fieldName + " keys and values must not be null"
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
