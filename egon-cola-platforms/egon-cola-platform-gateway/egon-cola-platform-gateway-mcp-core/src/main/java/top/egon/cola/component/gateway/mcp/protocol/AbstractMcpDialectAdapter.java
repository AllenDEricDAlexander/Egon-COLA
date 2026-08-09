package top.egon.cola.component.gateway.mcp.protocol;

import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;

import java.util.Objects;

abstract class AbstractMcpDialectAdapter implements McpDialectAdapter {

    private final McpJsonRpcCodec codec;

    AbstractMcpDialectAdapter(McpJsonRpcCodec codec) {
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    @Override
    public final McpJsonRpcRequest decode(HttpMcpRequest request) {
        validateHttp(request);
        McpJsonRpcRequest decoded = codec.decode(request.body());
        validateDialect(request, decoded);
        return decoded;
    }

    protected abstract void validateDialect(
            HttpMcpRequest request,
            McpJsonRpcRequest decoded);

    protected final void requireProtocolVersion(
            HttpMcpRequest request,
            boolean required) {
        String version = request.header("Mcp-Protocol-Version");
        if (version == null && !required) {
            return;
        }
        if (!dialect().protocolVersion().equals(version)) {
            throw new McpProtocolException(
                    McpErrorCode.MCP_PROTOCOL_UNSUPPORTED,
                    "MCP protocol version is not supported"
            );
        }
    }

    private void validateHttp(HttpMcpRequest request) {
        if (!"POST".equals(request.method())) {
            throw new McpProtocolException(
                    McpErrorCode.MCP_INVALID_REQUEST,
                    "MCP JSON-RPC requests require POST"
            );
        }
        if (!request.contentType().startsWith("application/json")) {
            throw new McpProtocolException(
                    McpErrorCode.MCP_INVALID_REQUEST,
                    "MCP JSON-RPC requests require application/json"
            );
        }
    }
}
