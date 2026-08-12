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
 * 补充说明 / Supplementary summary: {@code McpTaskOperationExecutor} 是类型，位于当前 Gateway 模块的相关包中，负责MCP任务操作Executor相关的职责与边界。
 * English supplement: {@code McpTaskOperationExecutor} is a type in the current Gateway module; it owns the mcp task operation executor-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpTaskOperationExecutor implements McpTaskExecutor {

    /**
     * Gateway Operation 调用边界；Gateway operation invocation boundary.
     * 补充说明 / Supplementary summary: 保存 操作Invoker 对应的状态、依赖或配置值；字段类型为 {@code GatewayOperationInvoker}，由 {@code McpTaskOperationExecutor} 在其生命周期内读取或更新。
     * English supplement: Holds the state, dependency, or configuration represented by operation invoker; its type is {@code GatewayOperationInvoker}, and {@code McpTaskOperationExecutor} reads or updates it during its lifecycle.
     * 用法 / Usage: 该字段通过 {@code McpTaskOperationExecutor} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskOperationExecutor}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayOperationInvoker operationInvoker;

    /**
     * JSON 编解码器；JSON codec.
     * 补充说明 / Supplementary summary: 保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code McpTaskOperationExecutor} 在其生命周期内读取或更新。
     * English supplement: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code McpTaskOperationExecutor} reads or updates it during its lifecycle.
     * 用法 / Usage: 该字段通过 {@code McpTaskOperationExecutor} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskOperationExecutor}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;

    /**
     * MCP Server 到 Resource URI 的当前规则解析器；active server-to-Resource resolver.
     * 补充说明 / Supplementary summary: 保存 资源Resolver 对应的状态、依赖或配置值；字段类型为 {@code Function<String, URI>}，由 {@code McpTaskOperationExecutor} 在其生命周期内读取或更新。
     * English supplement: Holds the state, dependency, or configuration represented by resource resolver; its type is {@code Function<String, URI>}, and {@code McpTaskOperationExecutor} reads or updates it during its lifecycle.
     * 用法 / Usage: 该字段通过 {@code McpTaskOperationExecutor} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskOperationExecutor}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Function<String, URI> resourceResolver;

    /**
     * IdP SERVICE Token 提供器；IdP SERVICE-token supplier.
     * 补充说明 / Supplementary summary: 保存 tokenSupplier 对应的状态、依赖或配置值；字段类型为 {@code McpTaskServiceTokenSupplier}，由 {@code McpTaskOperationExecutor} 在其生命周期内读取或更新。
     * English supplement: Holds the state, dependency, or configuration represented by token supplier; its type is {@code McpTaskServiceTokenSupplier}, and {@code McpTaskOperationExecutor} reads or updates it during its lifecycle.
     * 用法 / Usage: 该字段通过 {@code McpTaskOperationExecutor} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskOperationExecutor}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpTaskServiceTokenSupplier tokenSupplier;

    /**
     * 创建无状态任务执行器。
     * Creates a stateless task executor.
     * 补充说明 / Supplementary summary: 创建 {@code McpTaskOperationExecutor} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English supplement: Creates an instance of {@code McpTaskOperationExecutor} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
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
     * 补充说明 / Supplementary summary: 执行 execute 操作；该方法是 {@code McpTaskOperationExecutor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English supplement: Executes the execute operation; this method is the invocation entry point on {@code McpTaskOperationExecutor} and performs the corresponding runtime, management, or protocol work.
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskOperationExecutor.execute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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

    /**
     * 中文说明：执行 arguments 操作；该方法是 {@code McpTaskOperationExecutor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the arguments operation; this method is the invocation entry point on {@code McpTaskOperationExecutor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskOperationExecutor.arguments(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param configured 参数 configured；parameter configured。
     * @return 返回 arguments 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 body 操作；该方法是 {@code McpTaskOperationExecutor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the body operation; this method is the invocation entry point on {@code McpTaskOperationExecutor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskOperationExecutor.body(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param configured 参数 configured；parameter configured。
     * @param inputResponse 参数 input响应；parameter input response。
     * @return 返回 body 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 outcome 操作；该方法是 {@code McpTaskOperationExecutor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the outcome operation; this method is the invocation entry point on {@code McpTaskOperationExecutor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskOperationExecutor.outcome(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param result 参数 result；parameter result。
     * @return 返回 outcome 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 payload 操作；该方法是 {@code McpTaskOperationExecutor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the payload operation; this method is the invocation entry point on {@code McpTaskOperationExecutor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskOperationExecutor.payload(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param responseBody 参数 响应Body；parameter response body。
     * @return 返回 payload 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 firstHeader 操作；该方法是 {@code McpTaskOperationExecutor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the first header operation; this method is the invocation entry point on {@code McpTaskOperationExecutor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskOperationExecutor.firstHeader(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param headers 参数 headers；parameter headers。
     * @param name 参数 name；parameter name。
     * @return 返回 firstHeader 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 required 操作；该方法是 {@code McpTaskOperationExecutor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code McpTaskOperationExecutor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskOperationExecutor.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalStateException(field + " is required");
        }
        return value;
    }
}
