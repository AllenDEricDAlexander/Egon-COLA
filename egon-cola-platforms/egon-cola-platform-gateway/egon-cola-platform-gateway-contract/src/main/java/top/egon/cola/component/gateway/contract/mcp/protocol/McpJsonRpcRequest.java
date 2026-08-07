package top.egon.cola.component.gateway.contract.mcp.protocol;

import java.util.Map;

/**
 * MCP JSON-RPC 2.0 请求。
 *
 * <p>{@code params} 保存工具、资源或提示词的参数，{@code meta} 保存协议扩展元数据；没有
 * {@code id} 的请求被视为通知，不应产生响应。
 */
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
