package top.egon.cola.component.gateway.mcp.remote.service;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;
import top.egon.cola.component.gateway.core.mcp.remote.RemoteAuthProvider;
import top.egon.cola.component.gateway.mcp.rule.domain.CompiledMcpRules;
import top.egon.cola.component.gateway.mcp.common.security.McpSecurityGate;
import top.egon.cola.component.gateway.mcp.common.telemetry.McpTelemetry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Strategy for invoking reviewed REMOTE_MCP Tool descriptors.
 * 补充说明 / Supplementary summary: {@code RemoteMcpToolDriver} 是驱动器，位于当前 Gateway 模块的相关包中，负责远程MCP工具驱动器相关的职责与边界。
 * English supplement: {@code RemoteMcpToolDriver} is a remote mcp tool driver driver in the current Gateway module; it owns the remote mcp tool driver-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class RemoteMcpToolDriver {

    /**
     * 中文说明：保存 rules 对应的状态、依赖或配置值；字段类型为 {@code Supplier<CompiledMcpRules>}，由 {@code RemoteMcpToolDriver} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rules; its type is {@code Supplier<CompiledMcpRules>}, and {@code RemoteMcpToolDriver} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RemoteMcpToolDriver} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RemoteMcpToolDriver}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Supplier<CompiledMcpRules> rules;

    /**
     * 中文说明：保存 clients 对应的状态、依赖或配置值；字段类型为 {@code McpRemoteClientPool}，由 {@code RemoteMcpToolDriver} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clients; its type is {@code McpRemoteClientPool}, and {@code RemoteMcpToolDriver} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RemoteMcpToolDriver} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RemoteMcpToolDriver}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpRemoteClientPool clients;

    /**
     * 中文说明：保存 router 对应的状态、依赖或配置值；字段类型为 {@code McpNamespaceRouter}，由 {@code RemoteMcpToolDriver} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by router; its type is {@code McpNamespaceRouter}, and {@code RemoteMcpToolDriver} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RemoteMcpToolDriver} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RemoteMcpToolDriver}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpNamespaceRouter router;

    /**
     * 中文说明：保存 translator 对应的状态、依赖或配置值；字段类型为 {@code McpDialectTranslator}，由 {@code RemoteMcpToolDriver} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by translator; its type is {@code McpDialectTranslator}, and {@code RemoteMcpToolDriver} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RemoteMcpToolDriver} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RemoteMcpToolDriver}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpDialectTranslator translator;

    /**
     * 中文说明：创建 {@code RemoteMcpToolDriver} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RemoteMcpToolDriver} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param rules 参数 rules；parameter rules。
     * @param clients 参数 clients；parameter clients。
     * @param router 参数 router；parameter router。
     * @param translator 参数 translator；parameter translator。
     */
    public RemoteMcpToolDriver(
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
     * 中文说明：执行 invoke 操作；该方法是 {@code RemoteMcpToolDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the invoke operation; this method is the invocation entry point on {@code RemoteMcpToolDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RemoteMcpToolDriver.invoke(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param tool 参数 工具；parameter tool。
     * @param arguments 参数 arguments；parameter arguments。
     * @param identity 参数 身份；parameter identity。
     * @param inboundDialect 参数 inboundDialect；parameter inbound dialect。
     * @param meta 参数 meta；parameter meta。
     * @param traceHeaders 参数 traceHeaders；parameter trace headers。
     * @return 返回 invoke 的处理结果；returns the result of the operation.
     */
    public Publisher<Map<String, Object>> invoke(
            McpRuntimeTool tool,
            Map<String, Object> arguments,
            McpSecurityGate.IdentityContext identity,
            McpProtocolDialect inboundDialect,
            Map<String, Object> meta,
            Map<String, String> traceHeaders) {
        return invoke(
                tool,
                arguments,
                identity,
                inboundDialect,
                meta,
                traceHeaders,
                McpTelemetry.Scope.noop()
        );
    }

    /**
     * 中文说明：执行 invoke 操作；该方法是 {@code RemoteMcpToolDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the invoke operation; this method is the invocation entry point on {@code RemoteMcpToolDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RemoteMcpToolDriver.invoke(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param tool 参数 工具；parameter tool。
     * @param arguments 参数 arguments；parameter arguments。
     * @param identity 参数 身份；parameter identity。
     * @param inboundDialect 参数 inboundDialect；parameter inbound dialect。
     * @param meta 参数 meta；parameter meta。
     * @param traceHeaders 参数 traceHeaders；parameter trace headers。
     * @param telemetry 参数 遥测；parameter telemetry。
     * @return 返回 invoke 的处理结果；returns the result of the operation.
     */
    public Publisher<Map<String, Object>> invoke(
            McpRuntimeTool tool,
            Map<String, Object> arguments,
            McpSecurityGate.IdentityContext identity,
            McpProtocolDialect inboundDialect,
            Map<String, Object> meta,
            Map<String, String> traceHeaders,
            McpTelemetry.Scope telemetry) {
        if (tool.remoteMountId() == null) {
            return Mono.error(new IllegalArgumentException(
                    "remote MCP Tool mount is required"
            ));
        }
        McpNamespaceRouter.Binding binding = router.binding(
                active(),
                tool.remoteMountId(),
                "TOOL",
                tool.name()
        );
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("name", binding.remoteName());
        params.put("arguments", Map.copyOf(arguments));
        McpDialectTranslator.OutboundCall call = translator.outbound(
                inboundDialect,
                binding.provider().dialect(),
                "tools/call",
                params,
                meta,
                traceHeaders
        );
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
                .map(translator::result);
    }

    /**
     * 中文说明：执行 active 操作；该方法是 {@code RemoteMcpToolDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the active operation; this method is the invocation entry point on {@code RemoteMcpToolDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RemoteMcpToolDriver.active(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 active 的处理结果；returns the result of the operation.
     */
    private CompiledMcpRules active() {
        CompiledMcpRules current = rules.get();
        return current == null ? CompiledMcpRules.empty() : current;
    }
}
