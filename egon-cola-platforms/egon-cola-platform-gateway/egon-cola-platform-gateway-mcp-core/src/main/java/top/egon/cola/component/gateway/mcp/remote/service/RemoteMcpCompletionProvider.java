package top.egon.cola.component.gateway.mcp.remote.service;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimePrompt;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeResource;
import top.egon.cola.component.gateway.core.mcp.remote.RemoteAuthProvider;
import top.egon.cola.component.gateway.mcp.completion.service.McpCompletionProvider;
import top.egon.cola.component.gateway.mcp.prompt.service.McpPromptDriver;
import top.egon.cola.component.gateway.mcp.rule.domain.CompiledMcpRules;
import top.egon.cola.component.gateway.mcp.common.security.McpSecurityGate;
import top.egon.cola.component.gateway.mcp.common.telemetry.McpTelemetry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Completion Strategy for remote Prompt and Resource references.
 * 补充说明 / Supplementary summary: {@code RemoteMcpCompletionProvider} 是提供方组件，位于当前 Gateway 模块的相关包中，负责远程MCP补全提供方相关的职责与边界。
 * English supplement: {@code RemoteMcpCompletionProvider} is a remote mcp completion provider provider in the current Gateway module; it owns the remote mcp completion provider-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class RemoteMcpCompletionProvider
        implements McpCompletionProvider {

    /**
     * 中文说明：保存 rules 对应的状态、依赖或配置值；字段类型为 {@code Supplier<CompiledMcpRules>}，由 {@code RemoteMcpCompletionProvider} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rules; its type is {@code Supplier<CompiledMcpRules>}, and {@code RemoteMcpCompletionProvider} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RemoteMcpCompletionProvider} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RemoteMcpCompletionProvider}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Supplier<CompiledMcpRules> rules;

    /**
     * 中文说明：保存 clients 对应的状态、依赖或配置值；字段类型为 {@code McpRemoteClientPool}，由 {@code RemoteMcpCompletionProvider} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clients; its type is {@code McpRemoteClientPool}, and {@code RemoteMcpCompletionProvider} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RemoteMcpCompletionProvider} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RemoteMcpCompletionProvider}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpRemoteClientPool clients;

    /**
     * 中文说明：保存 router 对应的状态、依赖或配置值；字段类型为 {@code McpNamespaceRouter}，由 {@code RemoteMcpCompletionProvider} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by router; its type is {@code McpNamespaceRouter}, and {@code RemoteMcpCompletionProvider} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RemoteMcpCompletionProvider} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RemoteMcpCompletionProvider}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpNamespaceRouter router;

    /**
     * 中文说明：保存 translator 对应的状态、依赖或配置值；字段类型为 {@code McpDialectTranslator}，由 {@code RemoteMcpCompletionProvider} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by translator; its type is {@code McpDialectTranslator}, and {@code RemoteMcpCompletionProvider} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RemoteMcpCompletionProvider} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RemoteMcpCompletionProvider}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpDialectTranslator translator;

    /**
     * 中文说明：创建 {@code RemoteMcpCompletionProvider} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RemoteMcpCompletionProvider} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param rules 参数 rules；parameter rules。
     * @param clients 参数 clients；parameter clients。
     * @param router 参数 router；parameter router。
     * @param translator 参数 translator；parameter translator。
     */
    public RemoteMcpCompletionProvider(
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
     * 中文说明：执行 sourceType 操作；该方法是 {@code RemoteMcpCompletionProvider} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the source type operation; this method is the invocation entry point on {@code RemoteMcpCompletionProvider} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RemoteMcpCompletionProvider.sourceType(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 sourceType 的处理结果；returns the result of the operation.
     */
    @Override
    public String sourceType() {
        return "REMOTE_MCP";
    }

    /**
     * 中文说明：执行 complete 操作；该方法是 {@code RemoteMcpCompletionProvider} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the complete operation; this method is the invocation entry point on {@code RemoteMcpCompletionProvider} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RemoteMcpCompletionProvider.complete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 complete 的处理结果；returns the result of the operation.
     */
    @Override
    public Publisher<Result> complete(Request request) {
        if (McpCompletionProvider.sensitiveArgumentName(
                request.argumentName()
        ) || McpCompletionProvider.sensitiveValue(request.valuePrefix())) {
            return Mono.error(McpPromptDriver.invalid(
                    "MCP completion cannot enumerate sensitive values"
            ));
        }
        Reference reference = reference(request);
        McpNamespaceRouter.Binding binding = router.binding(
                active(),
                reference.mountId(),
                reference.primitiveType(),
                reference.exposedName()
        );
        Map<String, Object> ref = "ref/prompt".equals(request.referenceType())
                ? Map.of(
                        "type", "ref/prompt",
                        "name", binding.remoteName()
                )
                : Map.of(
                        "type", "ref/resource",
                        "uri", reference.remoteReference()
                );
        McpDialectTranslator.OutboundCall call = translator.outbound(
                dialect(request.attributes()),
                binding.provider().dialect(),
                "completion/complete",
                Map.of(
                        "ref", ref,
                        "argument", Map.of(
                                "name", request.argumentName(),
                                "value", request.valuePrefix()
                        )
                ),
                Map.of(),
                trace(request.attributes())
        );
        McpSecurityGate.IdentityContext identity =
                McpSecurityGate.IdentityContext.from(request.attributes());
        McpTelemetry.Scope telemetry = McpTelemetry.current(
                request.attributes()
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
                .map(translator::result)
                .map(this::result);
    }

    /**
     * 中文说明：执行 result 操作；该方法是 {@code RemoteMcpCompletionProvider} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the result operation; this method is the invocation entry point on {@code RemoteMcpCompletionProvider} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RemoteMcpCompletionProvider.result(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @return 返回 result 的处理结果；returns the result of the operation.
     */
    private Result result(Map<String, Object> source) {
        if (!(source.get("completion") instanceof Map<?, ?> completion)
                || !(completion.get("values") instanceof List<?> values)) {
            throw McpPromptDriver.invalid(
                    "remote MCP completion result is invalid"
            );
        }
        ArrayList<String> result = new ArrayList<>();
        values.forEach(value -> {
            if (!(value instanceof String text)
                    || McpCompletionProvider.sensitiveValue(text)
                    || result.size() >= 100) {
                return;
            }
            result.add(text);
        });
        int total = completion.get("total") instanceof Number number
                ? Math.max(result.size(), number.intValue())
                : result.size();
        boolean hasMore = Boolean.TRUE.equals(completion.get("hasMore"));
        return new Result(result, total, hasMore);
    }

    /**
     * 中文说明：执行 reference 操作；该方法是 {@code RemoteMcpCompletionProvider} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the reference operation; this method is the invocation entry point on {@code RemoteMcpCompletionProvider} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RemoteMcpCompletionProvider.reference(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 reference 的处理结果；returns the result of the operation.
     */
    private Reference reference(Request request) {
        CompiledMcpRules current = active();
        if ("ref/prompt".equals(request.referenceType())) {
            McpRuntimePrompt prompt = current.promptsByQualifiedName().get(
                    CompiledMcpRules.qualified(
                            request.serverCode(),
                            request.referenceName()
                    )
            );
            if (prompt != null && prompt.remoteMountId() != null) {
                return new Reference(
                        prompt.remoteMountId(),
                        "PROMPT",
                        prompt.name(),
                        prompt.name()
                );
            }
        }
        if ("ref/resource".equals(request.referenceType())) {
            for (McpRuntimeResource resource
                    : current.resourcesByQualifiedName().values()) {
                if (resource.serverCode().equals(request.serverCode())
                        && resource.uri().equals(request.referenceName())
                        && resource.remoteMountId() != null) {
                    return new Reference(
                            resource.remoteMountId(),
                            "RESOURCE",
                            resource.name(),
                            resource.configuration().getOrDefault(
                                    "remoteUri",
                                    resource.uri()
                            )
                    );
                }
            }
        }
        throw McpPromptDriver.invalid(
                "remote MCP completion reference was not found"
        );
    }

    /**
     * 中文说明：执行 active 操作；该方法是 {@code RemoteMcpCompletionProvider} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the active operation; this method is the invocation entry point on {@code RemoteMcpCompletionProvider} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RemoteMcpCompletionProvider.active(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 active 的处理结果；returns the result of the operation.
     */
    private CompiledMcpRules active() {
        CompiledMcpRules value = rules.get();
        return value == null ? CompiledMcpRules.empty() : value;
    }

    /**
     * 中文说明：执行 dialect 操作；该方法是 {@code RemoteMcpCompletionProvider} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the dialect operation; this method is the invocation entry point on {@code RemoteMcpCompletionProvider} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RemoteMcpCompletionProvider.dialect(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 trace 操作；该方法是 {@code RemoteMcpCompletionProvider} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the trace operation; this method is the invocation entry point on {@code RemoteMcpCompletionProvider} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RemoteMcpCompletionProvider.trace(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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

    /**
     * 中文说明：{@code Reference} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Reference相关的职责与边界。
     * English summary: {@code Reference} is an immutable data carrier in the current Gateway module; it owns the reference-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param mountId 参数 mountId；parameter mount id。
     * @param primitiveType 参数 primitiveType；parameter primitive type。
     * @param exposedName 参数 exposedName；parameter exposed name。
     * @param remoteReference 参数 远程Reference；parameter remote reference。
     */
    private record Reference(
            /**
             * 中文说明：保存 mountId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RemoteMcpCompletionProvider.Reference} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by mount id; its type is {@code String}, and {@code RemoteMcpCompletionProvider.Reference} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code RemoteMcpCompletionProvider.Reference} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RemoteMcpCompletionProvider.Reference}; do not couple callers to its representation when the owning type exposes an API.
             */
            String mountId,
            /**
             * 中文说明：保存 primitiveType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RemoteMcpCompletionProvider.Reference} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by primitive type; its type is {@code String}, and {@code RemoteMcpCompletionProvider.Reference} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code RemoteMcpCompletionProvider.Reference} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RemoteMcpCompletionProvider.Reference}; do not couple callers to its representation when the owning type exposes an API.
             */
            String primitiveType,
            /**
             * 中文说明：保存 exposedName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RemoteMcpCompletionProvider.Reference} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by exposed name; its type is {@code String}, and {@code RemoteMcpCompletionProvider.Reference} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code RemoteMcpCompletionProvider.Reference} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RemoteMcpCompletionProvider.Reference}; do not couple callers to its representation when the owning type exposes an API.
             */
            String exposedName,
            /**
             * 中文说明：保存 远程Reference 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RemoteMcpCompletionProvider.Reference} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by remote reference; its type is {@code String}, and {@code RemoteMcpCompletionProvider.Reference} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code RemoteMcpCompletionProvider.Reference} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RemoteMcpCompletionProvider.Reference}; do not couple callers to its representation when the owning type exposes an API.
             */
            String remoteReference
    ) {
    }
}
