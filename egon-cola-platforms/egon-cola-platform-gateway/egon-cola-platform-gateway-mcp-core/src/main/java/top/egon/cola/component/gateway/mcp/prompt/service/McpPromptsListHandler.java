package top.egon.cola.component.gateway.mcp.prompt.service;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 中文说明：{@code McpPromptsListHandler} 是处理器，位于当前 Gateway 模块的相关包中，负责MCPPromptsList处理器相关的职责与边界。
 * English summary: {@code McpPromptsListHandler} is a mcp prompts list handler handler in the current Gateway module; it owns the mcp prompts list handler-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpPromptsListHandler implements McpMethodHandler {

    /**
     * 中文说明：保存 rules 对应的状态、依赖或配置值；字段类型为 {@code Supplier<CompiledMcpRules>}，由 {@code McpPromptsListHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rules; its type is {@code Supplier<CompiledMcpRules>}, and {@code McpPromptsListHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpPromptsListHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpPromptsListHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Supplier<CompiledMcpRules> rules;

    /**
     * 中文说明：保存 安全 对应的状态、依赖或配置值；字段类型为 {@code McpSecurityGate}，由 {@code McpPromptsListHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by security; its type is {@code McpSecurityGate}, and {@code McpPromptsListHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpPromptsListHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpPromptsListHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpSecurityGate security;

    /**
     * 中文说明：创建 {@code McpPromptsListHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpPromptsListHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param rules 参数 rules；parameter rules。
     * @param security 参数 安全；parameter security。
     */
    public McpPromptsListHandler(
            Supplier<CompiledMcpRules> rules,
            McpSecurityGate security) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.security = Objects.requireNonNull(security, "security");
    }

    /**
     * 中文说明：执行 方法 操作；该方法是 {@code McpPromptsListHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the method operation; this method is the invocation entry point on {@code McpPromptsListHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpPromptsListHandler.method(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 方法 的处理结果；returns the result of the operation.
     */
    @Override
    public String method() {
        return "prompts/list";
    }

    /**
     * 中文说明：执行 handle 操作；该方法是 {@code McpPromptsListHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the handle operation; this method is the invocation entry point on {@code McpPromptsListHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpPromptsListHandler.handle(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param context 参数 context；parameter context。
     * @return 返回 handle 的处理结果；returns the result of the operation.
     */
    @Override
    public Publisher<McpJsonRpcResponse> handle(
            McpJsonRpcRequest request,
            McpRequestContext context) {
        McpSecurityGate.IdentityContext identity = identity(context);
        return Flux.fromIterable(prompts(context.server().serverCode()))
                .concatMap(prompt -> Mono.from(
                                security.authorizePrompt(prompt, identity)
                        ).thenReturn(describe(prompt))
                        .onErrorResume(
                                McpProtocolException.class,
                                ignored -> Mono.empty()
                        ))
                .collectList()
                .map(prompts -> McpJsonRpcResponse.success(
                        request.id(),
                        Map.of("prompts", prompts)
                ));
    }

    /**
     * 中文说明：执行 prompts 操作；该方法是 {@code McpPromptsListHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the prompts operation; this method is the invocation entry point on {@code McpPromptsListHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpPromptsListHandler.prompts(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serverCode 参数 服务器Code；parameter server code。
     * @return 返回 prompts 的处理结果；returns the result of the operation.
     */
    private List<McpRuntimePrompt> prompts(String serverCode) {
        CompiledMcpRules active = rules.get();
        if (active == null) {
            return List.of();
        }
        return active.promptsByQualifiedName().values().stream()
                .filter(McpRuntimePrompt::enabled)
                .filter(prompt -> prompt.serverCode().equals(serverCode))
                .filter(prompt -> active.remoteAvailable(
                        prompt.remoteMountId(),
                        "PROMPT"
                ))
                .sorted(java.util.Comparator.comparing(
                        McpRuntimePrompt::name
                ))
                .toList();
    }

    /**
     * 中文说明：执行 describe 操作；该方法是 {@code McpPromptsListHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the describe operation; this method is the invocation entry point on {@code McpPromptsListHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpPromptsListHandler.describe(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param prompt 参数 提示词；parameter prompt。
     * @return 返回 describe 的处理结果；returns the result of the operation.
     */
    private Map<String, Object> describe(McpRuntimePrompt prompt) {
        LinkedHashMap<String, Object> value = new LinkedHashMap<>();
        value.put("name", prompt.name());
        if (prompt.description() != null) {
            value.put("description", prompt.description());
        }
        value.put("arguments", prompt.arguments().stream()
                .map(name -> Map.<String, Object>of(
                        "name", name,
                        "required", true
                ))
                .toList());
        return Map.copyOf(value);
    }

    /**
     * 中文说明：执行 身份 操作；该方法是 {@code McpPromptsListHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the identity operation; this method is the invocation entry point on {@code McpPromptsListHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpPromptsListHandler.identity(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
}
