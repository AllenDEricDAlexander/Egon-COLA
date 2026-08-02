package top.egon.cola.component.gateway.contract.mcp.protocol;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record McpJsonRpcError(
        int code,
        String message,
        McpErrorCode dataCode,
        Map<String, Object> data
) {

    public McpJsonRpcError {
        message = required(message, "message");
        dataCode = Objects.requireNonNull(dataCode, "dataCode");
        LinkedHashMap<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("code", dataCode.name());
        if (data != null) {
            data.entrySet().stream()
                    .filter(entry -> !"code".equals(entry.getKey()))
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> normalized.put(
                            entry.getKey(),
                            entry.getValue()
                    ));
        }
        data = Collections.unmodifiableMap(normalized);
    }

    public static McpJsonRpcError of(
            McpErrorCode code,
            String message) {
        return new McpJsonRpcError(
                code.jsonRpcCode(),
                message,
                code,
                Map.of()
        );
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
