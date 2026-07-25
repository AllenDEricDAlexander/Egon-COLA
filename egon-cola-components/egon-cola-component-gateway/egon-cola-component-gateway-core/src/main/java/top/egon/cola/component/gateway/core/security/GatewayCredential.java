package top.egon.cola.component.gateway.core.security;

import java.util.LinkedHashMap;
import java.util.Map;

public final class GatewayCredential {

    private final String type;

    private final String tokenReference;

    private final Map<String, String> attributes;

    public GatewayCredential(
            String type,
            String tokenReference,
            Map<String, String> attributes) {
        this.type = required(type, "type", 64);
        this.tokenReference = required(
                tokenReference,
                "tokenReference",
                8192
        );
        this.attributes = attributes(attributes);
    }

    public String type() {
        return type;
    }

    public String tokenReference() {
        return tokenReference;
    }

    public Map<String, String> attributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return "GatewayCredential[type=" + type + ", sensitive=REDACTED]";
    }

    private Map<String, String> attributes(Map<String, String> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        if (source.size() > 16) {
            throw new IllegalArgumentException(
                    "credential attribute count exceeds 16"
            );
        }
        Map<String, String> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(
                required(key, "attribute key", 64),
                required(value, "attribute value", 256)
        ));
        return Map.copyOf(result);
    }

    private static String required(
            String value,
            String field,
            int maximum) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String normalized = value.trim();
        if (normalized.length() > maximum) {
            throw new IllegalArgumentException(
                    field + " exceeds " + maximum
            );
        }
        return normalized;
    }
}
