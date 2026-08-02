package top.egon.cola.component.gateway.mcp.resource;

import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.core.operation.GatewayOperationInvocation;
import top.egon.cola.component.gateway.core.operation.GatewayOperationInvoker;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static top.egon.cola.component.gateway.mcp.resource.McpResourceDriver.bounded;
import static top.egon.cola.component.gateway.mcp.resource.McpResourceDriver.rejected;

public final class OperationResourceDriver implements McpResourceDriver {

    public static final String DRIVER_TYPE = "OPERATION";

    private final GatewayOperationInvoker invoker;

    public OperationResourceDriver(GatewayOperationInvoker invoker) {
        this.invoker = Objects.requireNonNull(invoker, "invoker");
    }

    @Override
    public String driverType() {
        return DRIVER_TYPE;
    }

    @Override
    public Mono<Content> read(ReadRequest request) {
        if (request.operationId() == null) {
            throw rejected("MCP resource operation is not configured");
        }
        LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("uri", request.uri());
        arguments.putAll(request.uriVariables());
        GatewayOperationInvocation invocation = new GatewayOperationInvocation(
                request.operationId(),
                Map.copyOf(arguments),
                attribute(request, "originalBearerToken"),
                attribute(request, "callerId"),
                attribute(request, "clientIp"),
                traceHeaders(request)
        );
        return Mono.from(invoker.invoke(invocation)).map(result -> {
            if (result.statusCode() >= 400) {
                throw rejected("MCP resource operation failed");
            }
            return bounded(
                    request,
                    result.body(),
                    textual(request.mimeType())
            );
        });
    }

    private Map<String, String> traceHeaders(ReadRequest request) {
        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        copy(request, headers, "traceparent");
        copy(request, headers, "tracestate");
        copy(request, headers, "x-egon-request-id");
        return Map.copyOf(headers);
    }

    private void copy(
            ReadRequest request,
            Map<String, String> target,
            String name) {
        String value = attribute(request, name);
        if (value != null) {
            target.put(name, value);
        }
    }

    private String attribute(ReadRequest request, String name) {
        Object value = request.attributes().get(name);
        return value instanceof String text && !text.isBlank()
                ? text.trim()
                : null;
    }

    private boolean textual(String mimeType) {
        return mimeType.startsWith("text/")
                || "application/json".equals(mimeType)
                || mimeType.endsWith("+json");
    }
}
