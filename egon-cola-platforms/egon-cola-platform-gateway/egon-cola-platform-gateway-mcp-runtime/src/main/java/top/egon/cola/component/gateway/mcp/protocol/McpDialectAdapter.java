package top.egon.cola.component.gateway.mcp.protocol;

import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;

public interface McpDialectAdapter {

    McpProtocolDialect dialect();

    McpJsonRpcRequest decode(HttpMcpRequest request);
}
