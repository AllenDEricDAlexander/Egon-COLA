package top.egon.cola.component.gateway.mcp.protocol;

import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;

public final class StableMcpDialectAdapter extends AbstractMcpDialectAdapter {

    public StableMcpDialectAdapter(McpJsonRpcCodec codec) {
        super(codec);
    }

    @Override
    public McpProtocolDialect dialect() {
        return McpProtocolDialect.STABLE_2025_11_25;
    }

    @Override
    protected void validateDialect(
            HttpMcpRequest request,
            McpJsonRpcRequest decoded) {
        requireProtocolVersion(request, false);
        if (!"initialize".equals(decoded.method())) {
            return;
        }
        Object version = decoded.params().get("protocolVersion");
        if (!dialect().protocolVersion().equals(version)) {
            throw new McpProtocolException(
                    McpErrorCode.MCP_PROTOCOL_UNSUPPORTED,
                    "MCP initialize protocol version is not supported"
            );
        }
    }
}
