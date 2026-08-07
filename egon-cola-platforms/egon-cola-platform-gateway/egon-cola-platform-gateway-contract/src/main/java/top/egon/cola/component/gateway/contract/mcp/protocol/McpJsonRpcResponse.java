package top.egon.cola.component.gateway.contract.mcp.protocol;

/**
 * MCP JSON-RPC 2.0 响应。
 *
 * <p>响应只能在 {@code result} 和 {@code error} 之间二选一；通知请求由协议层直接忽略响应。
 */
public record McpJsonRpcResponse(
        String jsonrpc,
        Object id,
        Object result,
        McpJsonRpcError error
) {

    public McpJsonRpcResponse {
        if (!McpJsonRpcRequest.VERSION.equals(jsonrpc)) {
            throw new IllegalArgumentException("jsonrpc must be 2.0");
        }
        if (result != null && error != null) {
            throw new IllegalArgumentException(
                    "result and error are mutually exclusive"
            );
        }
    }

    public static McpJsonRpcResponse success(Object id, Object result) {
        return new McpJsonRpcResponse(
                McpJsonRpcRequest.VERSION,
                id,
                result,
                null
        );
    }

    public static McpJsonRpcResponse failure(
            Object id,
            McpJsonRpcError error) {
        return new McpJsonRpcResponse(
                McpJsonRpcRequest.VERSION,
                id,
                null,
                error
        );
    }

    public static McpJsonRpcResponse methodNotFound(Object id) {
        return failure(
                id,
                McpJsonRpcError.of(
                        McpErrorCode.MCP_METHOD_NOT_FOUND,
                        "MCP method was not found"
                )
        );
    }
}
