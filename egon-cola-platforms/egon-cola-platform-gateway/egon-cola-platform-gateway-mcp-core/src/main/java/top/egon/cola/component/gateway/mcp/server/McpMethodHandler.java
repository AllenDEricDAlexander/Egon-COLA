package top.egon.cola.component.gateway.mcp.server;

import org.reactivestreams.Publisher;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;

public interface McpMethodHandler {

    String method();

    Publisher<McpJsonRpcResponse> handle(
            McpJsonRpcRequest request,
            McpRequestContext context);
}
