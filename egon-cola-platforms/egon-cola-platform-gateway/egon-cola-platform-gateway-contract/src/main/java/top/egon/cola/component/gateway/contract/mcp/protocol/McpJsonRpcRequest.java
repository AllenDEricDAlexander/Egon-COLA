package top.egon.cola.component.gateway.contract.mcp.protocol;

import java.util.Map;

public record McpJsonRpcRequest(
        String jsonrpc,
        Object id,
        String method,
        Map<String, Object> params,
        Map<String, Object> meta
) {

    public static final String VERSION = "2.0";

    public McpJsonRpcRequest {
        jsonrpc = required(jsonrpc, "jsonrpc");
        method = required(method, "method");
        params = params == null ? Map.of() : Map.copyOf(params);
        meta = meta == null ? Map.of() : Map.copyOf(meta);
    }

    public boolean notification() {
        return id == null;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
