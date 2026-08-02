package top.egon.cola.component.gateway.mcp.server;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcError;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;
import top.egon.cola.component.gateway.mcp.telemetry.McpTelemetry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class McpMethodDispatcher {

    private final Map<String, McpMethodHandler> handlers;

    private final McpTelemetry telemetry;

    public McpMethodDispatcher(List<McpMethodHandler> handlers) {
        this(handlers, McpTelemetry.noop());
    }

    public McpMethodDispatcher(
            List<McpMethodHandler> handlers,
            McpTelemetry telemetry) {
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
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
    }

    public Publisher<McpJsonRpcResponse> dispatch(
            McpJsonRpcRequest request,
            McpRequestContext context) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(context, "context");
        McpTelemetry.Scope observation = McpTelemetry.startSafely(
                telemetry,
                telemetryRequest(request, context)
        );
        McpMethodHandler handler = handlers.get(request.method());
        if (handler == null) {
            observation.failure(McpErrorCode.MCP_METHOD_NOT_FOUND.name());
            return request.notification()
                    ? Flux.empty()
                    : Mono.just(McpJsonRpcResponse.methodNotFound(request.id()));
        }

        Flux<McpJsonRpcResponse> responses;
        try {
            LinkedHashMap<String, Object> attributes = new LinkedHashMap<>(
                    context.attributes()
            );
            attributes.put(McpTelemetry.SCOPE_ATTRIBUTE, observation);
            McpRequestContext observedContext = new McpRequestContext(
                    context.server(),
                    context.dialect(),
                    context.sessionId(),
                    Map.copyOf(attributes)
            );
            Publisher<McpJsonRpcResponse> result = Objects.requireNonNull(
                    handler.handle(request, observedContext),
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
        Flux<McpJsonRpcResponse> observed = responses
                .doOnNext(response -> {
                    if (response.error() == null) {
                        observation.success();
                    } else {
                        observation.failure(
                                response.error().dataCode().name()
                        );
                    }
                })
                .doOnError(ignored -> observation.failure(
                        McpErrorCode.MCP_INTERNAL_ERROR.name()
                ))
                .doOnComplete(observation::success)
                .doOnCancel(() -> observation.failure("CANCELLED"));
        return request.notification()
                ? observed.thenMany(Flux.empty())
                : observed;
    }

    private McpTelemetry.Request telemetryRequest(
            McpJsonRpcRequest request,
            McpRequestContext context) {
        Object remote = context.attributes().get("mcp.remote-provider");
        return new McpTelemetry.Request(
                request.method(),
                primitive(request.method()),
                context.server().serverCode(),
                remote instanceof String value && !value.isBlank()
                        ? value
                        : null,
                context.attributes()
        );
    }

    private String primitive(String method) {
        if (method.startsWith("tools/")) {
            return "TOOL";
        }
        if (method.startsWith("resources/")) {
            return method.contains("subscribe")
                    ? "SUBSCRIPTION"
                    : "RESOURCE";
        }
        if (method.startsWith("subscriptions/")) {
            return "SUBSCRIPTION";
        }
        if (method.startsWith("prompts/")) {
            return "PROMPT";
        }
        if (method.startsWith("completion/")) {
            return "COMPLETION";
        }
        if (method.startsWith("tasks/")) {
            return "TASK";
        }
        if ("initialize".equals(method)
                || "notifications/initialized".equals(method)
                || "server/discover".equals(method)
                || "ping".equals(method)) {
            return "LIFECYCLE";
        }
        return "UNKNOWN";
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
