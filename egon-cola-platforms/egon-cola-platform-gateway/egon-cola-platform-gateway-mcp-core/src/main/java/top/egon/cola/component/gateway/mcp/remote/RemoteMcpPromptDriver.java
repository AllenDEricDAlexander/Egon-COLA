package top.egon.cola.component.gateway.mcp.remote;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimePrompt;
import top.egon.cola.component.gateway.core.mcp.remote.RemoteAuthProvider;
import top.egon.cola.component.gateway.mcp.prompt.McpPromptDriver;
import top.egon.cola.component.gateway.mcp.rule.CompiledMcpRules;
import top.egon.cola.component.gateway.mcp.security.McpSecurityGate;
import top.egon.cola.component.gateway.mcp.telemetry.McpTelemetry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Prompt Strategy for reviewed REMOTE_MCP descriptors.
 * 补充说明 / Supplementary summary: {@code RemoteMcpPromptDriver} 是驱动器，位于当前 Gateway 模块的相关包中，负责远程MCP提示词驱动器相关的职责与边界。
 * English supplement: {@code RemoteMcpPromptDriver} is a remote mcp prompt driver driver in the current Gateway module; it owns the remote mcp prompt driver-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class RemoteMcpPromptDriver implements McpPromptDriver {

    /**
     * 中文说明：保存 rules 对应的状态、依赖或配置值；字段类型为 {@code Supplier<CompiledMcpRules>}，由 {@code RemoteMcpPromptDriver} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rules; its type is {@code Supplier<CompiledMcpRules>}, and {@code RemoteMcpPromptDriver} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RemoteMcpPromptDriver} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RemoteMcpPromptDriver}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Supplier<CompiledMcpRules> rules;

    /**
     * 中文说明：保存 clients 对应的状态、依赖或配置值；字段类型为 {@code McpRemoteClientPool}，由 {@code RemoteMcpPromptDriver} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clients; its type is {@code McpRemoteClientPool}, and {@code RemoteMcpPromptDriver} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RemoteMcpPromptDriver} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RemoteMcpPromptDriver}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpRemoteClientPool clients;

    /**
     * 中文说明：保存 router 对应的状态、依赖或配置值；字段类型为 {@code McpNamespaceRouter}，由 {@code RemoteMcpPromptDriver} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by router; its type is {@code McpNamespaceRouter}, and {@code RemoteMcpPromptDriver} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RemoteMcpPromptDriver} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RemoteMcpPromptDriver}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpNamespaceRouter router;

    /**
     * 中文说明：保存 translator 对应的状态、依赖或配置值；字段类型为 {@code McpDialectTranslator}，由 {@code RemoteMcpPromptDriver} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by translator; its type is {@code McpDialectTranslator}, and {@code RemoteMcpPromptDriver} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RemoteMcpPromptDriver} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RemoteMcpPromptDriver}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpDialectTranslator translator;

    /**
     * 中文说明：创建 {@code RemoteMcpPromptDriver} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RemoteMcpPromptDriver} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param rules 参数 rules；parameter rules。
     * @param clients 参数 clients；parameter clients。
     * @param router 参数 router；parameter router。
     * @param translator 参数 translator；parameter translator。
     */
    public RemoteMcpPromptDriver(
            Supplier<CompiledMcpRules> rules,
            McpRemoteClientPool clients,
            McpNamespaceRouter router,
            McpDialectTranslator translator) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.clients = Objects.requireNonNull(clients, "clients");
        this.router = Objects.requireNonNull(router, "router");
        this.translator = Objects.requireNonNull(translator, "translator");
    }

    /**
     * 中文说明：执行 sourceTypes 操作；该方法是 {@code RemoteMcpPromptDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the source types operation; this method is the invocation entry point on {@code RemoteMcpPromptDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RemoteMcpPromptDriver.sourceTypes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 sourceTypes 的处理结果；returns the result of the operation.
     */
    @Override
    public Set<String> sourceTypes() {
        return Set.of("REMOTE_MCP");
    }

    /**
     * 中文说明：执行 render 操作；该方法是 {@code RemoteMcpPromptDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the render operation; this method is the invocation entry point on {@code RemoteMcpPromptDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RemoteMcpPromptDriver.render(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param prompt 参数 提示词；parameter prompt。
     * @param arguments 参数 arguments；parameter arguments。
     * @param attributes 参数 attributes；parameter attributes。
     * @return 返回 render 的处理结果；returns the result of the operation.
     */
    @Override
    public Publisher<Result> render(
            McpRuntimePrompt prompt,
            Map<String, String> arguments,
            Map<String, Object> attributes) {
        McpNamespaceRouter.Binding binding = router.binding(
                active(),
                prompt.remoteMountId(),
                "PROMPT",
                prompt.name()
        );
        McpDialectTranslator.OutboundCall call = translator.outbound(
                dialect(attributes),
                binding.provider().dialect(),
                "prompts/get",
                Map.of(
                        "name", binding.remoteName(),
                        "arguments", Map.copyOf(arguments)
                ),
                Map.of(),
                trace(attributes)
        );
        McpSecurityGate.IdentityContext identity =
                McpSecurityGate.IdentityContext.from(attributes);
        McpTelemetry.Scope telemetry = McpTelemetry.current(attributes);
        telemetry.remoteProvider(binding.provider().providerCode());
        var exchange = clients.exchange(
                binding.provider(),
                call,
                new RemoteAuthProvider.AuthContext(
                        identity.subjectId(),
                        identity.tenantId(),
                        identity.clientId()
                )
        );
        return Mono.from(McpTelemetry.observeChild(
                        telemetry,
                        McpTelemetry.ChildKind.REMOTE,
                        exchange
                ))
                .map(translator::result)
                .map(this::result);
    }

    /**
     * 中文说明：执行 result 操作；该方法是 {@code RemoteMcpPromptDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the result operation; this method is the invocation entry point on {@code RemoteMcpPromptDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RemoteMcpPromptDriver.result(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @return 返回 result 的处理结果；returns the result of the operation.
     */
    private Result result(Map<String, Object> source) {
        String description = source.get("description") instanceof String text
                ? text
                : "";
        if (!(source.get("messages") instanceof List<?> messages)) {
            throw McpPromptDriver.invalid(
                    "remote MCP prompt messages are invalid"
            );
        }
        ArrayList<Message> result = new ArrayList<>();
        messages.forEach(value -> {
            if (!(value instanceof Map<?, ?> message)
                    || !(message.get("role") instanceof String role)
                    || !(message.get("content") instanceof Map<?, ?> content)
                    || !(content.get("text") instanceof String text)) {
                throw McpPromptDriver.invalid(
                        "remote MCP prompt message is invalid"
                );
            }
            result.add(new Message(role, text));
        });
        return new Result(description, result);
    }

    /**
     * 中文说明：执行 active 操作；该方法是 {@code RemoteMcpPromptDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the active operation; this method is the invocation entry point on {@code RemoteMcpPromptDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RemoteMcpPromptDriver.active(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 active 的处理结果；returns the result of the operation.
     */
    private CompiledMcpRules active() {
        CompiledMcpRules value = rules.get();
        return value == null ? CompiledMcpRules.empty() : value;
    }

    /**
     * 中文说明：执行 dialect 操作；该方法是 {@code RemoteMcpPromptDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the dialect operation; this method is the invocation entry point on {@code RemoteMcpPromptDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RemoteMcpPromptDriver.dialect(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param attributes 参数 attributes；parameter attributes。
     * @return 返回 dialect 的处理结果；returns the result of the operation.
     */
    private McpProtocolDialect dialect(Map<String, Object> attributes) {
        Object value = attributes.get("mcp.protocol-dialect");
        return value instanceof McpProtocolDialect dialect
                ? dialect
                : McpProtocolDialect.STABLE_2025_11_25;
    }

    /**
     * 中文说明：执行 trace 操作；该方法是 {@code RemoteMcpPromptDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the trace operation; this method is the invocation entry point on {@code RemoteMcpPromptDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RemoteMcpPromptDriver.trace(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param attributes 参数 attributes；parameter attributes。
     * @return 返回 trace 的处理结果；returns the result of the operation.
     */
    private Map<String, String> trace(Map<String, Object> attributes) {
        java.util.LinkedHashMap<String, String> result =
                new java.util.LinkedHashMap<>();
        List.of("traceparent", "tracestate", "x-egon-request-id")
                .forEach(name -> {
                    Object value = attributes.get(name);
                    if (value instanceof String text && !text.isBlank()) {
                        result.put(name, text.trim());
                    }
                });
        return Map.copyOf(result);
    }
}
