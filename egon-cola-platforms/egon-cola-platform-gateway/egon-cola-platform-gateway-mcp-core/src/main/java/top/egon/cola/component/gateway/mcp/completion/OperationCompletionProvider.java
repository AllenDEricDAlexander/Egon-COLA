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

/**
 * 中文说明：{@code OperationCompletionProvider} 是提供方组件，位于当前 Gateway 模块的相关包中，负责操作补全提供方相关的职责与边界。
 * English summary: {@code OperationCompletionProvider} is a operation completion provider provider in the current Gateway module; it owns the operation completion provider-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class OperationCompletionProvider
        implements McpCompletionProvider {

    /**
     * 中文说明：保存 invoker 对应的状态、依赖或配置值；字段类型为 {@code GatewayOperationInvoker}，由 {@code OperationCompletionProvider} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by invoker; its type is {@code GatewayOperationInvoker}, and {@code OperationCompletionProvider} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code OperationCompletionProvider} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code OperationCompletionProvider}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayOperationInvoker invoker;

    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code OperationCompletionProvider} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code OperationCompletionProvider} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code OperationCompletionProvider} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code OperationCompletionProvider}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：创建 {@code OperationCompletionProvider} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code OperationCompletionProvider} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param invoker 参数 invoker；parameter invoker。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     */
    public OperationCompletionProvider(
            GatewayOperationInvoker invoker,
            ObjectMapper objectMapper) {
        this.invoker = Objects.requireNonNull(invoker, "invoker");
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
    }

    /**
     * 中文说明：执行 sourceType 操作；该方法是 {@code OperationCompletionProvider} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the source type operation; this method is the invocation entry point on {@code OperationCompletionProvider} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code OperationCompletionProvider.sourceType(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 sourceType 的处理结果；returns the result of the operation.
     */
    @Override
    public String sourceType() {
        return "LOCAL_OPERATION";
    }

    /**
     * 中文说明：执行 complete 操作；该方法是 {@code OperationCompletionProvider} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the complete operation; this method is the invocation entry point on {@code OperationCompletionProvider} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code OperationCompletionProvider.complete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 complete 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 values 操作；该方法是 {@code OperationCompletionProvider} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the values operation; this method is the invocation entry point on {@code OperationCompletionProvider} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code OperationCompletionProvider.values(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param body 参数 body；parameter body。
     * @return 返回 values 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 traceHeaders 操作；该方法是 {@code OperationCompletionProvider} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the trace headers operation; this method is the invocation entry point on {@code OperationCompletionProvider} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code OperationCompletionProvider.traceHeaders(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 traceHeaders 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 attribute 操作；该方法是 {@code OperationCompletionProvider} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the attribute operation; this method is the invocation entry point on {@code OperationCompletionProvider} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code OperationCompletionProvider.attribute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param name 参数 name；parameter name。
     * @return 返回 attribute 的处理结果；returns the result of the operation.
     */
    private String attribute(Request request, String name) {
        Object value = request.attributes().get(name);
        return value instanceof String text && !text.isBlank()
                ? text.trim()
                : null;
    }
}
