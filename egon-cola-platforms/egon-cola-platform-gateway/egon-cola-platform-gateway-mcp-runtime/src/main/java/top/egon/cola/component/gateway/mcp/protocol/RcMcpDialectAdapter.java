package top.egon.cola.component.gateway.mcp.protocol;

import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;

import java.util.Objects;

public final class RcMcpDialectAdapter extends AbstractMcpDialectAdapter {

    public RcMcpDialectAdapter(McpJsonRpcCodec codec) {
        super(codec);
    }

    @Override
    public McpProtocolDialect dialect() {
        return McpProtocolDialect.RC_2026_07_28;
    }

    @Override
    protected void validateDialect(
            HttpMcpRequest request,
            McpJsonRpcRequest decoded) {
        requireProtocolVersion(request, true);
        if (decoded.meta().isEmpty()) {
            throw new McpProtocolException(
                    McpErrorCode.MCP_CLIENT_CAPABILITY_REQUIRED,
                    "MCP RC request metadata is required"
            );
        }
        requireAgreement(
                request.header("Mcp-Method"),
                decoded.method(),
                "method"
        );
        Object name = decoded.params().get("name");
        if (name != null) {
            requireAgreement(
                    request.header("Mcp-Name"),
                    String.valueOf(name),
                    "name"
            );
        } else if (request.header("Mcp-Name") != null) {
            throw mismatch("name");
        }
    }

    private void requireAgreement(
            String header,
            String body,
            String field) {
        if (!Objects.equals(header, body)) {
            throw mismatch(field);
        }
    }

    private McpProtocolException mismatch(String field) {
        return new McpProtocolException(
                McpErrorCode.MCP_HEADER_MISMATCH,
                "MCP " + field + " header does not match the JSON-RPC body"
        );
    }
}
