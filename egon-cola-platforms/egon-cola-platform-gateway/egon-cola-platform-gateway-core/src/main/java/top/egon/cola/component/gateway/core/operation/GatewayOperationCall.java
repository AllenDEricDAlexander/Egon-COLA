package top.egon.cola.component.gateway.core.operation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Position-aware arguments for an in-process Gateway Operation invocation.
 */
public record GatewayOperationCall(
        String operationId,
        Map<String, Object> pathArguments,
        Map<String, Object> queryArguments,
        Object body
) {

    public GatewayOperationCall {
        operationId = required(operationId, "operationId");
        pathArguments = immutableArguments(pathArguments);
        queryArguments = immutableArguments(queryArguments);
    }

    private static Map<String, Object> immutableArguments(
            Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(
                required(key, "argument name"),
                value
        ));
        return Collections.unmodifiableMap(copy);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
