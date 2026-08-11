package top.egon.cola.component.gateway.engine.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.core.operation.GatewayInvocationResult;
import top.egon.cola.component.gateway.core.operation.GatewayOperationCall;
import top.egon.cola.component.gateway.core.operation.GatewayOperationInvocation;
import top.egon.cola.component.gateway.core.operation.GatewayOperationInvoker;
import top.egon.cola.component.gateway.mcp.task.McpTask;
import top.egon.cola.component.gateway.mcp.task.McpTaskExecutor;

import java.net.URI;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 使用 Gateway Engine 的 SERVICE 身份执行已通过 USER 权限校验的持久化 MCP 任务。
 * Executes durable MCP tasks that already passed USER authorization by using the Gateway
 * Engine's SERVICE identity.
 *
 * <p>USER Token 永不进入任务存储；执行时根据任务 tenant 与 MCP Server Resource 动态换取
 * SERVICE Token。</p>
 *
 * <p>The USER token never enters task storage. Execution exchanges the task tenant and MCP
 * Server Resource for a SERVICE token at runtime.</p>
 */
public final class McpTaskOperationExecutor implements McpTaskExecutor {

    /** Gateway Operation 调用边界；Gateway operation invocation boundary. */
    private final GatewayOperationInvoker operationInvoker;

    /** JSON 编解码器；JSON codec. */
    private final ObjectMapper objectMapper;

    /** MCP Server 到 Resource URI 的当前规则解析器；active server-to-Resource resolver. */
    private final Function<String, URI> resourceResolver;

    /** IdP SERVICE Token 提供器；IdP SERVICE-token supplier. */
    private final McpTaskServiceTokenSupplier tokenSupplier;

    /**
     * 创建无状态任务执行器。
     * Creates a stateless task executor.
     */
    public McpTaskOperationExecutor(
            GatewayOperationInvoker operationInvoker,
            ObjectMapper objectMapper,
            Function<String, URI> resourceResolver,
            McpTaskServiceTokenSupplier tokenSupplier
    ) {
        this.operationInvoker = Objects.requireNonNull(
                operationInvoker,
                "operationInvoker"
        );
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.resourceResolver = Objects.requireNonNull(
                resourceResolver,
                "resourceResolver"
        );
        this.tokenSupplier = Objects.requireNonNull(
                tokenSupplier,
                "tokenSupplier"
        );
    }

    /**
     * 解析任务输入、换取 SERVICE Token 并直接调用目标 Operation。
     * Resolves task input, exchanges a SERVICE token, and invokes the target operation directly.
     */
    @Override
    public Publisher<Outcome> execute(McpTask task) {
        Objects.requireNonNull(task, "task");
        Object operation = task.inputPayload().get("operationId");
        if (!(operation instanceof String operationId)
                || operationId.isBlank()) {
            return Mono.just(Outcome.failed(Map.of(
                    "code", "MCP_TASK_OPERATION_MISSING"
            )));
        }
        URI resourceUri = Objects.requireNonNull(
                resourceResolver.apply(task.serverCode()),
                "resourceUri"
        );
        String serviceToken = required(
                tokenSupplier.issue(task.tenantId(), resourceUri),
                "serviceToken"
        );
        GatewayOperationInvocation invocation = new GatewayOperationInvocation(
                new GatewayOperationCall(
                        operationId,
                        arguments(task.inputPayload().get("pathArguments")),
                        arguments(task.inputPayload().get("queryArguments")),
                        body(
                                task.inputPayload().get("body"),
                                task.inputPayload().get("inputResponse")
                        )
                ),
                "Bearer " + serviceToken,
                task.subjectId(),
                null,
                Map.of()
        );
        return Mono.from(operationInvoker.invoke(invocation))
                .map(this::outcome);
    }

    private Map<String, Object> arguments(Object configured) {
        if (!(configured instanceof Map<?, ?> source)) {
            return Map.of();
        }
        LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key instanceof String name) {
                arguments.put(name, value);
            }
        });
        return Map.copyOf(arguments);
    }

    private Object body(Object configured, Object inputResponse) {
        if (!(inputResponse instanceof Map<?, ?> input)) {
            return configured;
        }
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        if (configured instanceof Map<?, ?> source) {
            source.forEach((key, value) -> {
                if (key instanceof String name) {
                    result.put(name, value);
                }
            });
        }
        result.put("input", input);
        return Map.copyOf(result);
    }

    private Outcome outcome(GatewayInvocationResult result) {
        Map<String, Object> payload = payload(result.body());
        String inputKey = firstHeader(
                result.headers(),
                "x-egon-mcp-input-key"
        );
        if (result.statusCode() == 202 && inputKey != null) {
            return Outcome.inputRequired(inputKey, payload);
        }
        if (result.statusCode() >= 400) {
            return Outcome.failed(Map.of(
                    "code", "MCP_TASK_UPSTREAM_FAILED",
                    "status", result.statusCode()
            ));
        }
        return Outcome.completed(payload);
    }

    private Map<String, Object> payload(byte[] responseBody) {
        if (responseBody.length == 0) {
            return Map.of();
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root != null && root.isObject()) {
                return objectMapper.convertValue(
                        root,
                        new TypeReference<Map<String, Object>>() {
                        }
                );
            }
        } catch (Exception ignored) {
            // Non-JSON task results are returned as bounded Base64 content.
        }
        if (responseBody.length > 1024 * 1024) {
            return Map.of("code", "MCP_TASK_RESULT_TOO_LARGE");
        }
        return Map.of(
                "contentBase64",
                Base64.getEncoder().encodeToString(responseBody)
        );
    }

    private String firstHeader(
            Map<String, List<String>> headers,
            String name
    ) {
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .flatMap(entry -> entry.getValue().stream())
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .map(String::trim)
                .orElse(null);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalStateException(field + " is required");
        }
        return value;
    }
}
