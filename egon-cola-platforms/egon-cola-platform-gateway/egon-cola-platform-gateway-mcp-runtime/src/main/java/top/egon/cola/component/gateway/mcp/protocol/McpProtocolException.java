package top.egon.cola.component.gateway.mcp.protocol;

import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcError;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;

import java.util.Map;
import java.util.Objects;

public final class McpProtocolException extends RuntimeException {

    private final McpErrorCode code;
    private final Map<String, Object> data;

    public McpProtocolException(McpErrorCode code, String message) {
        this(code, message, Map.of());
    }

    public McpProtocolException(
            McpErrorCode code,
            String message,
            Map<String, Object> data) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
        this.data = data == null ? Map.of() : Map.copyOf(data);
    }

    public McpErrorCode code() {
        return code;
    }

    public Map<String, Object> data() {
        return data;
    }

    public McpJsonRpcResponse toResponse(Object id) {
        return McpJsonRpcResponse.failure(
                id,
                new McpJsonRpcError(
                        code.jsonRpcCode(),
                        getMessage(),
                        code,
                        data
                )
        );
    }
}
