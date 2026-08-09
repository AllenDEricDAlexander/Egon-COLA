package top.egon.cola.component.gateway.mcp.protocol;

import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;

public final class LegacySseMcpAdapter extends AbstractMcpDialectAdapter {

    public LegacySseMcpAdapter(McpJsonRpcCodec codec) {
        super(codec);
    }

    @Override
    public McpProtocolDialect dialect() {
        return McpProtocolDialect.LEGACY_2024_SSE;
    }

    @Override
    protected void validateDialect(
            HttpMcpRequest request,
            McpJsonRpcRequest decoded) {
        requireProtocolVersion(request, false);
    }
}
