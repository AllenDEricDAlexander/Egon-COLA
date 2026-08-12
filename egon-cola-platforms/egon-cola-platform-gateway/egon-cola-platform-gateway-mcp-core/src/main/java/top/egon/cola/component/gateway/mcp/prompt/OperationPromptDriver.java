package top.egon.cola.component.gateway.mcp.prompt;

import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimePrompt;
import top.egon.cola.component.gateway.core.operation.GatewayOperationCall;
import top.egon.cola.component.gateway.core.operation.GatewayOperationInvocation;
import top.egon.cola.component.gateway.core.operation.GatewayOperationInvoker;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 中文说明：{@code OperationPromptDriver} 是驱动器，位于当前 Gateway 模块的相关包中，负责操作提示词驱动器相关的职责与边界。
 * English summary: {@code OperationPromptDriver} is a operation prompt driver driver in the current Gateway module; it owns the operation prompt driver-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class OperationPromptDriver implements McpPromptDriver {

    /**
     * 中文说明：保存 invoker 对应的状态、依赖或配置值；字段类型为 {@code GatewayOperationInvoker}，由 {@code OperationPromptDriver} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by invoker; its type is {@code GatewayOperationInvoker}, and {@code OperationPromptDriver} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code OperationPromptDriver} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code OperationPromptDriver}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayOperationInvoker invoker;

    /**
     * 中文说明：创建 {@code OperationPromptDriver} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code OperationPromptDriver} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param invoker 参数 invoker；parameter invoker。
     */
    public OperationPromptDriver(GatewayOperationInvoker invoker) {
        this.invoker = Objects.requireNonNull(invoker, "invoker");
    }

    /**
     * 中文说明：执行 sourceTypes 操作；该方法是 {@code OperationPromptDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the source types operation; this method is the invocation entry point on {@code OperationPromptDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code OperationPromptDriver.sourceTypes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 sourceTypes 的处理结果；returns the result of the operation.
     */
    @Override
    public Set<String> sourceTypes() {
        return Set.of("LOCAL_OPERATION");
    }

    /**
     * 中文说明：执行 render 操作；该方法是 {@code OperationPromptDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the render operation; this method is the invocation entry point on {@code OperationPromptDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code OperationPromptDriver.render(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param prompt 参数 提示词；parameter prompt。
     * @param arguments 参数 arguments；parameter arguments。
     * @param attributes 参数 attributes；parameter attributes。
     * @return 返回 render 的处理结果；returns the result of the operation.
     */
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
                new GatewayOperationCall(
                        prompt.operationId(),
                        Map.of(),
                        Map.copyOf(arguments),
                        null
                ),
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

    /**
     * 中文说明：执行 traceHeaders 操作；该方法是 {@code OperationPromptDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the trace headers operation; this method is the invocation entry point on {@code OperationPromptDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code OperationPromptDriver.traceHeaders(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param attributes 参数 attributes；parameter attributes。
     * @return 返回 traceHeaders 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 attribute 操作；该方法是 {@code OperationPromptDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the attribute operation; this method is the invocation entry point on {@code OperationPromptDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code OperationPromptDriver.attribute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param attributes 参数 attributes；parameter attributes。
     * @param name 参数 name；parameter name。
     * @return 返回 attribute 的处理结果；returns the result of the operation.
     */
    private String attribute(Map<String, Object> attributes, String name) {
        Object value = attributes.get(name);
        return value instanceof String text && !text.isBlank()
                ? text.trim()
                : null;
    }
}
