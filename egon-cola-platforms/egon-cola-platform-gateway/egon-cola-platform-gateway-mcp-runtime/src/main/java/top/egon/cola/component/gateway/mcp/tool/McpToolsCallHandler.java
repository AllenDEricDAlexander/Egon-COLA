package top.egon.cola.component.gateway.mcp.tool;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;
import top.egon.cola.component.gateway.core.operation.GatewayOperationInvocation;
import top.egon.cola.component.gateway.core.operation.GatewayOperationInvoker;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;
import top.egon.cola.component.gateway.mcp.server.McpMethodHandler;
import top.egon.cola.component.gateway.mcp.server.McpRequestContext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class McpToolsCallHandler implements McpMethodHandler {

    private final McpToolCatalog catalog;

    private final McpArgumentBinder argumentBinder;

    private final McpResultBinder resultBinder;

    private final GatewayOperationInvoker invoker;

    public McpToolsCallHandler(
            McpToolCatalog catalog,
            McpArgumentBinder argumentBinder,
            McpResultBinder resultBinder,
            GatewayOperationInvoker invoker) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.argumentBinder = Objects.requireNonNull(
                argumentBinder,
                "argumentBinder"
        );
        this.resultBinder = Objects.requireNonNull(
                resultBinder,
                "resultBinder"
        );
        this.invoker = Objects.requireNonNull(invoker, "invoker");
    }

    @Override
    public String method() {
        return "tools/call";
    }

    @Override
    public Publisher<McpJsonRpcResponse> handle(
            McpJsonRpcRequest request,
            McpRequestContext context) {
        String name = string(request.params().get("name"), "name");
        McpRuntimeTool tool = catalog.localTool(
                context.server().serverCode(),
                name
        ).orElseThrow(() -> invalid("MCP tool was not found"));
        Map<String, Object> arguments = arguments(
                request.params().get("arguments")
        );
        GatewayOperationInvocation invocation =
                new GatewayOperationInvocation(
                        tool.operationId(),
                        argumentBinder.bind(tool, arguments),
                        attribute(context, "originalBearerToken"),
                        attribute(context, "callerId"),
                        attribute(context, "clientIp"),
                        traceHeaders(context)
                );
        return Mono.from(invoker.invoke(invocation))
                .map(result -> McpJsonRpcResponse.success(
                        request.id(),
                        resultBinder.bind(tool, result)
                ));
    }

    private Map<String, Object> arguments(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> source)) {
            throw invalid("MCP tool arguments must be an object");
        }
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
        source.forEach((key, item) -> {
            if (!(key instanceof String name)) {
                throw invalid("MCP tool argument names must be strings");
            }
            copy.put(name, item);
        });
        return java.util.Collections.unmodifiableMap(copy);
    }

    private Map<String, String> traceHeaders(McpRequestContext context) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        copyAttribute(context, values, "traceparent");
        copyAttribute(context, values, "tracestate");
        copyAttribute(context, values, "x-egon-request-id");
        return Map.copyOf(values);
    }

    private void copyAttribute(
            McpRequestContext context,
            Map<String, String> target,
            String name) {
        String value = attribute(context, name);
        if (value != null) {
            target.put(name, value);
        }
    }

    private String attribute(McpRequestContext context, String name) {
        Object value = context.attributes().get(name);
        return value instanceof String text && !text.isBlank()
                ? text.trim()
                : null;
    }

    private String string(Object value, String field) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw invalid("MCP " + field + " is required");
        }
        return text.trim();
    }

    private McpProtocolException invalid(String message) {
        return new McpProtocolException(
                McpErrorCode.MCP_INVALID_PARAMS,
                message
        );
    }
}
