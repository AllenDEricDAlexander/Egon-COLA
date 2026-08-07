package top.egon.cola.component.gateway.contract.mcp.protocol;

/**
 * MCP JSON-RPC 错误码及其网关内部语义。
 *
 * <p>枚举值同时提供标准 JSON-RPC 数字码和网关可审计的业务码，响应构造时应优先使用本类型。
 */
public enum McpErrorCode {
    MCP_PARSE_ERROR(-32700),
    MCP_INVALID_REQUEST(-32600),
    MCP_METHOD_NOT_FOUND(-32601),
    MCP_INVALID_PARAMS(-32602),
    MCP_INTERNAL_ERROR(-32603),
    MCP_HEADER_MISMATCH(-32020),
    MCP_CLIENT_CAPABILITY_REQUIRED(-32021),
    MCP_PROTOCOL_UNSUPPORTED(-32022),
    MCP_UNAUTHENTICATED(-32023),
    MCP_FORBIDDEN(-32024),
    MCP_APPROVAL_REQUIRED(-32025),
    MCP_APPROVAL_MISMATCH(-32026),
    MCP_APPROVAL_CONSUMED(-32027),
    MCP_TASK_NOT_FOUND(-32028),
    MCP_RESOURCE_REJECTED(-32029),
    MCP_REMOTE_UNAVAILABLE(-32030);

    private final int jsonRpcCode;

    McpErrorCode(int jsonRpcCode) {
        this.jsonRpcCode = jsonRpcCode;
    }

    public int jsonRpcCode() {
        return jsonRpcCode;
    }
}
