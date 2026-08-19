package top.egon.cola.component.gateway.mcp.prompt.service;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimePrompt;
import top.egon.cola.component.gateway.mcp.common.protocol.McpProtocolException;
import top.egon.cola.component.gateway.mcp.rule.domain.CompiledMcpRules;
import top.egon.cola.component.gateway.mcp.common.security.McpSecurityGate;
import top.egon.cola.component.gateway.mcp.server.service.McpMethodHandler;
import top.egon.cola.component.gateway.mcp.server.domain.McpRequestContext;
import top.egon.cola.component.gateway.mcp.common.telemetry.McpTelemetry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 中文说明：{@code McpPromptsGetHandler} 是处理器，位于当前 Gateway 模块的相关包中，负责MCPPromptsGet处理器相关的职责与边界。
 * English summary: {@code McpPromptsGetHandler} is a mcp prompts get handler handler in the current Gateway module; it owns the mcp prompts get handler-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpPromptsGetHandler implements McpMethodHandler {

    /**
     * 中文说明：保存 rules 对应的状态、依赖或配置值；字段类型为 {@code Supplier<CompiledMcpRules>}，由 {@code McpPromptsGetHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rules; its type is {@code Supplier<CompiledMcpRules>}, and {@code McpPromptsGetHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpPromptsGetHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpPromptsGetHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Supplier<CompiledMcpRules> rules;

    /**
     * 中文说明：保存 drivers 对应的状态、依赖或配置值；字段类型为 {@code Map<String, McpPromptDriver>}，由 {@code McpPromptsGetHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by drivers; its type is {@code Map<String, McpPromptDriver>}, and {@code McpPromptsGetHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpPromptsGetHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpPromptsGetHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Map<String, McpPromptDriver> drivers;

    /**
     * 中文说明：保存 安全 对应的状态、依赖或配置值；字段类型为 {@code McpSecurityGate}，由 {@code McpPromptsGetHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by security; its type is {@code McpSecurityGate}, and {@code McpPromptsGetHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpPromptsGetHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpPromptsGetHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpSecurityGate security;

    /**
     * 中文说明：创建 {@code McpPromptsGetHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpPromptsGetHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param rules 参数 rules；parameter rules。
     * @param drivers 参数 drivers；parameter drivers。
     * @param security 参数 安全；parameter security。
     */
    public McpPromptsGetHandler(
            Supplier<CompiledMcpRules> rules,
            List<McpPromptDriver> drivers,
            McpSecurityGate security) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.drivers = index(drivers);
        this.security = Objects.requireNonNull(security, "security");
    }

    /**
     * 中文说明：执行 方法 操作；该方法是 {@code McpPromptsGetHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the method operation; this method is the invocation entry point on {@code McpPromptsGetHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpPromptsGetHandler.method(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 方法 的处理结果；returns the result of the operation.
     */
    @Override
    public String method() {
        return "prompts/get";
    }

    /**
     * 中文说明：执行 handle 操作；该方法是 {@code McpPromptsGetHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the handle operation; this method is the invocation entry point on {@code McpPromptsGetHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpPromptsGetHandler.handle(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param context 参数 context；parameter context。
     * @return 返回 handle 的处理结果；returns the result of the operation.
     */
    @Override
    public Publisher<McpJsonRpcResponse> handle(
            McpJsonRpcRequest request,
            McpRequestContext context) {
        McpRuntimePrompt prompt = prompt(
                context.server().serverCode(),
                string(request.params().get("name"), "name")
        );
        McpPromptDriver driver = drivers.get(prompt.sourceType());
        if (driver == null) {
            throw McpPromptDriver.invalid(
                    "MCP prompt driver is unavailable"
            );
        }
        Map<String, String> arguments = arguments(
                request.params().get("arguments")
        );
        McpSecurityGate.IdentityContext identity = identity(context);
        Map<String, Object> attributes = attributes(context);
        Publisher<McpPromptDriver.Result> rendered = driver.render(
                prompt,
                arguments,
                attributes
        );
        if ("LOCAL_OPERATION".equals(prompt.sourceType())) {
            rendered = McpTelemetry.observeChild(
                    attributes,
                    McpTelemetry.ChildKind.OPERATION,
                    rendered
            );
        }
        return Mono.from(security.authorizePrompt(prompt, identity))
                .then(Mono.from(rendered))
                .map(result -> McpJsonRpcResponse.success(
                        request.id(),
                        describe(result)
                ));
    }

    /**
     * 中文说明：执行 提示词 操作；该方法是 {@code McpPromptsGetHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the prompt operation; this method is the invocation entry point on {@code McpPromptsGetHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpPromptsGetHandler.prompt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param name 参数 name；parameter name。
     * @return 返回 提示词 的处理结果；returns the result of the operation.
     */
    private McpRuntimePrompt prompt(String serverCode, String name) {
        CompiledMcpRules active = rules.get();
        McpRuntimePrompt prompt = active == null
                ? null
                : active.promptsByQualifiedName().get(
                        CompiledMcpRules.qualified(serverCode, name)
                );
        if (prompt == null || !prompt.enabled()
                || !active.remoteAvailable(
                prompt.remoteMountId(),
                "PROMPT"
        )) {
            throw McpPromptDriver.invalid("MCP prompt was not found");
        }
        return prompt;
    }

    /**
     * 中文说明：执行 describe 操作；该方法是 {@code McpPromptsGetHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the describe operation; this method is the invocation entry point on {@code McpPromptsGetHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpPromptsGetHandler.describe(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param result 参数 result；parameter result。
     * @return 返回 describe 的处理结果；returns the result of the operation.
     */
    private Map<String, Object> describe(McpPromptDriver.Result result) {
        return Map.of(
                "description", result.description(),
                "messages", result.messages().stream()
                        .map(message -> Map.<String, Object>of(
                                "role", message.role(),
                                "content", Map.of(
                                        "type", "text",
                                        "text", message.text()
                                )
                        ))
                        .toList()
        );
    }

    /**
     * 中文说明：执行 索引 操作；该方法是 {@code McpPromptsGetHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the index operation; this method is the invocation entry point on {@code McpPromptsGetHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpPromptsGetHandler.index(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @return 返回 索引 的处理结果；returns the result of the operation.
     */
    private Map<String, McpPromptDriver> index(
            List<McpPromptDriver> source) {
        LinkedHashMap<String, McpPromptDriver> result = new LinkedHashMap<>();
        Objects.requireNonNull(source, "drivers").forEach(driver ->
                driver.sourceTypes().forEach(type -> {
                    if (result.putIfAbsent(type, driver) != null) {
                        throw new IllegalArgumentException(
                                "MCP prompt source types must be unique"
                        );
                    }
                }));
        return Collections.unmodifiableMap(result);
    }

    /**
     * 中文说明：执行 arguments 操作；该方法是 {@code McpPromptsGetHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the arguments operation; this method is the invocation entry point on {@code McpPromptsGetHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpPromptsGetHandler.arguments(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 arguments 的处理结果；returns the result of the operation.
     */
    private Map<String, String> arguments(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> source)) {
            throw McpPromptDriver.invalid(
                    "MCP prompt arguments must be an object"
            );
        }
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        source.forEach((key, item) -> {
            if (!(key instanceof String name)
                    || !(item instanceof String text)) {
                throw McpPromptDriver.invalid(
                        "MCP prompt arguments must contain strings"
                );
            }
            result.put(name, text);
        });
        return Map.copyOf(result);
    }

    /**
     * 中文说明：执行 string 操作；该方法是 {@code McpPromptsGetHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the string operation; this method is the invocation entry point on {@code McpPromptsGetHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpPromptsGetHandler.string(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 string 的处理结果；returns the result of the operation.
     */
    private String string(Object value, String field) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw McpPromptDriver.invalid(
                    "MCP prompt " + field + " is required"
            );
        }
        return text.trim();
    }

    /**
     * 中文说明：执行 身份 操作；该方法是 {@code McpPromptsGetHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the identity operation; this method is the invocation entry point on {@code McpPromptsGetHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpPromptsGetHandler.identity(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param context 参数 context；parameter context。
     * @return 返回 身份 的处理结果；returns the result of the operation.
     */
    private McpSecurityGate.IdentityContext identity(
            McpRequestContext context) {
        try {
            return McpSecurityGate.IdentityContext.from(context.attributes());
        } catch (IllegalArgumentException failure) {
            throw new McpProtocolException(
                    McpErrorCode.MCP_UNAUTHENTICATED,
                    "MCP identity context is incomplete"
            );
        }
    }

    /**
     * 中文说明：执行 attributes 操作；该方法是 {@code McpPromptsGetHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the attributes operation; this method is the invocation entry point on {@code McpPromptsGetHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpPromptsGetHandler.attributes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param context 参数 context；parameter context。
     * @return 返回 attributes 的处理结果；returns the result of the operation.
     */
    private Map<String, Object> attributes(McpRequestContext context) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>(
                context.attributes()
        );
        result.put("mcp.protocol-dialect", context.dialect());
        return Map.copyOf(result);
    }
}
