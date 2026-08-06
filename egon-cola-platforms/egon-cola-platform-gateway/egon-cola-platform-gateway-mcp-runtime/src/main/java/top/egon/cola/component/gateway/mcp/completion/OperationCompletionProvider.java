package top.egon.cola.component.gateway.mcp.completion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.core.operation.GatewayOperationCall;
import top.egon.cola.component.gateway.core.operation.GatewayOperationInvocation;
import top.egon.cola.component.gateway.core.operation.GatewayOperationInvoker;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;
import top.egon.cola.component.gateway.mcp.prompt.McpPromptDriver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class OperationCompletionProvider
        implements McpCompletionProvider {

    private final GatewayOperationInvoker invoker;

    private final ObjectMapper objectMapper;

    public OperationCompletionProvider(
            GatewayOperationInvoker invoker,
            ObjectMapper objectMapper) {
        this.invoker = Objects.requireNonNull(invoker, "invoker");
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
    }

    @Override
    public String sourceType() {
        return "LOCAL_OPERATION";
    }

    @Override
    public Mono<Result> complete(Request request) {
        if (McpCompletionProvider.sensitiveArgumentName(
                request.argumentName()
        )) {
            return Mono.just(new Result(List.of(), 0, false));
        }
        if (request.operationId() == null) {
            throw McpPromptDriver.invalid(
                    "MCP completion operation is not configured"
            );
        }
        GatewayOperationInvocation invocation = new GatewayOperationInvocation(
                new GatewayOperationCall(
                        request.operationId(),
                        Map.of(),
                        Map.of(
                                "referenceType", request.referenceType(),
                                "referenceName", request.referenceName(),
                                "argumentName", request.argumentName(),
                                "value", request.valuePrefix()
                        ),
                        null
                ),
                attribute(request, "originalBearerToken"),
                attribute(request, "callerId"),
                attribute(request, "clientIp"),
                traceHeaders(request)
        );
        return Mono.from(invoker.invoke(invocation)).map(result -> {
            if (result.statusCode() >= 400
                    || result.body().length > 256 * 1024) {
                throw McpPromptDriver.invalid(
                        "MCP completion operation failed"
                );
            }
            List<String> values = values(result.body()).stream()
                    .filter(value -> value.length() <= 256)
                    .filter(value -> !McpCompletionProvider.sensitiveValue(
                            value
                    ))
                    .distinct()
                    .sorted()
                    .toList();
            return new Result(
                    values.stream().limit(100).toList(),
                    values.size(),
                    values.size() > 100
            );
        });
    }

    private List<String> values(byte[] body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode source = root.isArray() ? root : root.path("values");
            if (!source.isArray()) {
                throw McpPromptDriver.invalid(
                        "MCP completion response is invalid"
                );
            }
            ArrayList<String> result = new ArrayList<>();
            source.forEach(value -> {
                if (!value.isTextual()) {
                    throw McpPromptDriver.invalid(
                            "MCP completion values must be strings"
                    );
                }
                result.add(value.textValue());
            });
            return List.copyOf(result);
        } catch (McpProtocolException failure) {
            throw failure;
        } catch (Exception failure) {
            if (failure instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw McpPromptDriver.invalid(
                    "MCP completion response is invalid"
            );
        }
    }

    private Map<String, String> traceHeaders(Request request) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (String name : List.of(
                "traceparent",
                "tracestate",
                "x-egon-request-id")) {
            String value = attribute(request, name);
            if (value != null) {
                result.put(name, value);
            }
        }
        return Map.copyOf(result);
    }

    private String attribute(Request request, String name) {
        Object value = request.attributes().get(name);
        return value instanceof String text && !text.isBlank()
                ? text.trim()
                : null;
    }
}
