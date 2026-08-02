package top.egon.cola.component.gateway.mcp.server.handler;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.mcp.server.McpMethodHandler;
import top.egon.cola.component.gateway.mcp.server.McpRequestContext;

import java.util.Map;

public final class McpPingHandler implements McpMethodHandler {

    @Override
    public String method() {
        return "ping";
    }

    @Override
    public Publisher<McpJsonRpcResponse> handle(
            McpJsonRpcRequest request,
            McpRequestContext context) {
        return Mono.just(McpJsonRpcResponse.success(request.id(), Map.of()));
    }
}
