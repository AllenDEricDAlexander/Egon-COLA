package top.egon.cola.component.gateway.mcp.prompt;

import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimePrompt;
import top.egon.cola.component.gateway.core.operation.GatewayOperationInvocation;
import top.egon.cola.component.gateway.core.operation.GatewayOperationInvoker;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class OperationPromptDriver implements McpPromptDriver {

    private final GatewayOperationInvoker invoker;

    public OperationPromptDriver(GatewayOperationInvoker invoker) {
        this.invoker = Objects.requireNonNull(invoker, "invoker");
    }

    @Override
    public Set<String> sourceTypes() {
        return Set.of("LOCAL_OPERATION");
    }

    @Override
    public Mono<Result> render(
            McpRuntimePrompt prompt,
            Map<String, String> arguments,
            Map<String, Object> attributes) {
        if (prompt.operationId() == null) {
            throw McpPromptDriver.invalid(
                    "MCP prompt operation is not configured"
            );
        }
        if (!Set.copyOf(prompt.arguments()).containsAll(arguments.keySet())) {
            throw McpPromptDriver.invalid(
                    "MCP prompt contains an undeclared argument"
            );
        }
        GatewayOperationInvocation invocation = new GatewayOperationInvocation(
                prompt.operationId(),
                Map.copyOf(arguments),
                attribute(attributes, "originalBearerToken"),
                attribute(attributes, "callerId"),
                attribute(attributes, "clientIp"),
                traceHeaders(attributes)
        );
        return Mono.from(invoker.invoke(invocation)).map(result -> {
            if (result.statusCode() >= 400
                    || result.body().length > 512 * 1024) {
                throw McpPromptDriver.invalid(
                        "MCP prompt operation failed"
                );
            }
            return new Result(
                    prompt.description(),
                    List.of(new Message(
                            "user",
                            new String(
                                    result.body(),
                                    StandardCharsets.UTF_8
                            )
                    ))
            );
        });
    }

    private Map<String, String> traceHeaders(Map<String, Object> attributes) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (String name : List.of(
                "traceparent",
                "tracestate",
                "x-egon-request-id")) {
            String value = attribute(attributes, name);
            if (value != null) {
                result.put(name, value);
            }
        }
        return Map.copyOf(result);
    }

    private String attribute(Map<String, Object> attributes, String name) {
        Object value = attributes.get(name);
        return value instanceof String text && !text.isBlank()
                ? text.trim()
                : null;
    }
}
