package top.egon.cola.component.gateway.mcp.server.handler;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.mcp.server.McpMethodHandler;
import top.egon.cola.component.gateway.mcp.server.McpRequestContext;

public final class McpDiscoverHandler implements McpMethodHandler {

    @Override
    public String method() {
        return "server/discover";
    }

    @Override
    public Publisher<McpJsonRpcResponse> handle(
            McpJsonRpcRequest request,
            McpRequestContext context) {
        return Mono.just(McpJsonRpcResponse.success(
                request.id(),
                McpServerDescription.result(
                        new McpServerDescription.McpRequestContextView(
                                context.server(),
                                context.dialect().protocolVersion()
                        )
                )
        ));
    }
}
