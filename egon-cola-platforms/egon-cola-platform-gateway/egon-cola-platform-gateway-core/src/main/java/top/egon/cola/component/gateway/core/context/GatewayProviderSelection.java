package top.egon.cola.component.gateway.core.context;

import java.util.LinkedHashMap;
import java.util.Map;

public record GatewayProviderSelection(
        String serviceName,
        String instanceId,
        String leaseId,
        String host,
        int port,
        Map<String, String> metadata
) {

    public GatewayProviderSelection {
        serviceName = required(serviceName, "serviceName");
        instanceId = required(instanceId, "instanceId");
        leaseId = required(leaseId, "leaseId");
        host = required(host, "host");
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("port is invalid");
        }
        metadata = immutableMetadata(metadata);
    }

    private static Map<String, String> immutableMetadata(
            Map<String, String> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, String> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key == null || value == null) {
                throw new IllegalArgumentException(
                        "metadata keys and values must not be null"
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
