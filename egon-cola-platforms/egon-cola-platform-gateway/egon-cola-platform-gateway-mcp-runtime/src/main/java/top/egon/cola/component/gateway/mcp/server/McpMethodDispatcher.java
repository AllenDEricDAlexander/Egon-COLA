package top.egon.cola.component.gateway.mcp.server;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcError;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class McpMethodDispatcher {

    private final Map<String, McpMethodHandler> handlers;

    public McpMethodDispatcher(List<McpMethodHandler> handlers) {
        LinkedHashMap<String, McpMethodHandler> indexed = new LinkedHashMap<>();
        Objects.requireNonNull(handlers, "handlers").stream()
                .sorted(java.util.Comparator.comparing(McpMethodHandler::method))
                .forEach(handler -> {
                    String method = required(handler.method());
                    if (indexed.putIfAbsent(method, handler) != null) {
                        throw new IllegalArgumentException(
                                "duplicate MCP method handler: " + method
                        );
                    }
                });
        this.handlers = Collections.unmodifiableMap(indexed);
    }

    public Publisher<McpJsonRpcResponse> dispatch(
            McpJsonRpcRequest request,
            McpRequestContext context) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(context, "context");
        McpMethodHandler handler = handlers.get(request.method());
        if (handler == null) {
            return request.notification()
                    ? Flux.empty()
                    : Mono.just(McpJsonRpcResponse.methodNotFound(request.id()));
        }

        Flux<McpJsonRpcResponse> responses;
        try {
            Publisher<McpJsonRpcResponse> result = Objects.requireNonNull(
                    handler.handle(request, context),
                    "handler result"
            );
            responses = Flux.from(result)
                    .onErrorResume(
                            McpProtocolException.class,
                            error -> Mono.just(error.toResponse(request.id()))
                    )
                    .onErrorResume(
                            error -> Mono.just(internalError(request.id()))
                    );
        } catch (McpProtocolException error) {
            responses = Flux.just(error.toResponse(request.id()));
        } catch (RuntimeException error) {
            responses = Flux.just(internalError(request.id()));
        }
        return request.notification() ? responses.thenMany(Flux.empty()) : responses;
    }

    private McpJsonRpcResponse internalError(Object id) {
        return McpJsonRpcResponse.failure(
                id,
                McpJsonRpcError.of(
                        McpErrorCode.MCP_INTERNAL_ERROR,
                        "MCP request processing failed"
                )
        );
    }

    private String required(String method) {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("MCP handler method is required");
        }
        return method.trim();
    }
}
