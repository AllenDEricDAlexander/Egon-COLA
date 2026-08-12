package top.egon.cola.component.gateway.mcp.completion;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimePrompt;
import top.egon.cola.component.gateway.mcp.prompt.McpPromptDriver;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;
import top.egon.cola.component.gateway.mcp.resource.McpResourceCatalog;
import top.egon.cola.component.gateway.mcp.rule.CompiledMcpRules;
import top.egon.cola.component.gateway.mcp.security.McpSecurityGate;
import top.egon.cola.component.gateway.mcp.server.McpMethodHandler;
import top.egon.cola.component.gateway.mcp.server.McpRequestContext;
import top.egon.cola.component.gateway.mcp.telemetry.McpTelemetry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 中文说明：{@code McpCompletionHandler} 是处理器，位于当前 Gateway 模块的相关包中，负责MCP补全处理器相关的职责与边界。
 * English summary: {@code McpCompletionHandler} is a mcp completion handler handler in the current Gateway module; it owns the mcp completion handler-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpCompletionHandler implements McpMethodHandler {

    /**
     * 中文说明：保存 rules 对应的状态、依赖或配置值；字段类型为 {@code Supplier<CompiledMcpRules>}，由 {@code McpCompletionHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rules; its type is {@code Supplier<CompiledMcpRules>}, and {@code McpCompletionHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpCompletionHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCompletionHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Supplier<CompiledMcpRules> rules;

    /**
     * 中文说明：保存 resources 对应的状态、依赖或配置值；字段类型为 {@code McpResourceCatalog}，由 {@code McpCompletionHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by resources; its type is {@code McpResourceCatalog}, and {@code McpCompletionHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpCompletionHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCompletionHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpResourceCatalog resources;

    /**
     * 中文说明：保存 providers 对应的状态、依赖或配置值；字段类型为 {@code Map<String, McpCompletionProvider>}，由 {@code McpCompletionHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by providers; its type is {@code Map<String, McpCompletionProvider>}, and {@code McpCompletionHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpCompletionHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCompletionHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Map<String, McpCompletionProvider> providers;

    /**
     * 中文说明：保存 安全 对应的状态、依赖或配置值；字段类型为 {@code McpSecurityGate}，由 {@code McpCompletionHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by security; its type is {@code McpSecurityGate}, and {@code McpCompletionHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpCompletionHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCompletionHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpSecurityGate security;

    /**
     * 中文说明：创建 {@code McpCompletionHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpCompletionHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param rules 参数 rules；parameter rules。
     * @param resources 参数 resources；parameter resources。
     * @param providers 参数 providers；parameter providers。
     * @param security 参数 安全；parameter security。
     */
    public McpCompletionHandler(
            Supplier<CompiledMcpRules> rules,
            McpResourceCatalog resources,
            List<McpCompletionProvider> providers,
            McpSecurityGate security) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.resources = Objects.requireNonNull(resources, "resources");
        this.providers = index(providers);
        this.security = Objects.requireNonNull(security, "security");
    }

    /**
     * 中文说明：执行 方法 操作；该方法是 {@code McpCompletionHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the method operation; this method is the invocation entry point on {@code McpCompletionHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCompletionHandler.method(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 方法 的处理结果；returns the result of the operation.
     */
    @Override
    public String method() {
        return "completion/complete";
    }

    /**
     * 中文说明：执行 handle 操作；该方法是 {@code McpCompletionHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the handle operation; this method is the invocation entry point on {@code McpCompletionHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCompletionHandler.handle(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param context 参数 context；parameter context。
     * @return 返回 handle 的处理结果；returns the result of the operation.
     */
    @Override
    public Publisher<McpJsonRpcResponse> handle(
            McpJsonRpcRequest request,
            McpRequestContext context) {
        Map<?, ?> reference = object(request.params().get("ref"), "ref");
        Map<?, ?> argument = object(
                request.params().get("argument"),
                "argument"
        );
        String referenceType = string(reference.get("type"), "ref.type");
        String argumentName = string(
                argument.get("name"),
                "argument.name"
        );
        String prefix = optional(argument.get("value"));
        McpSecurityGate.IdentityContext identity = identity(context);
        Resolved resolved = switch (referenceType) {
            case "ref/prompt" -> prompt(
                    context.server().serverCode(),
                    string(reference.get("name"), "ref.name"),
                    argumentName,
                    identity
            );
            case "ref/resource" -> resource(
                    context.server().serverCode(),
                    string(reference.get("uri"), "ref.uri"),
                    identity
            );
            default -> throw McpPromptDriver.invalid(
                    "MCP completion reference type is invalid"
            );
        };
        McpCompletionProvider provider = providers.get(resolved.sourceType());
        if (provider == null) {
            throw McpPromptDriver.invalid(
                    "MCP completion provider is unavailable"
            );
        }
        Map<String, Object> attributes = attributes(context);
        Publisher<McpCompletionProvider.Result> completion =
                provider.complete(new McpCompletionProvider.Request(
                        context.server().serverCode(),
                        referenceType,
                        resolved.referenceName(),
                        argumentName,
                        prefix,
                        resolved.operationId(),
                        attributes
                ));
        if ("LOCAL_OPERATION".equals(resolved.sourceType())) {
            completion = McpTelemetry.observeChild(
                    attributes,
                    McpTelemetry.ChildKind.OPERATION,
                    completion
            );
        }
        return Mono.from(resolved.authorization())
                .then(Mono.from(completion))
                .map(result -> McpJsonRpcResponse.success(
                        request.id(),
                        Map.of("completion", Map.of(
                                "values", result.values(),
                                "total", result.total(),
                                "hasMore", result.hasMore()
                        ))
                ));
    }

    /**
     * 中文说明：执行 提示词 操作；该方法是 {@code McpCompletionHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the prompt operation; this method is the invocation entry point on {@code McpCompletionHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCompletionHandler.prompt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param name 参数 name；parameter name。
     * @param argumentName 参数 argumentName；parameter argument name。
     * @param identity 参数 身份；parameter identity。
     * @return 返回 提示词 的处理结果；returns the result of the operation.
     */
    private Resolved prompt(
            String serverCode,
            String name,
            String argumentName,
            McpSecurityGate.IdentityContext identity) {
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
        if (!prompt.arguments().contains(argumentName)) {
            throw McpPromptDriver.invalid(
                    "MCP prompt argument is not declared"
            );
        }
        String sourceType = prompt.remoteMountId() != null
                ? "REMOTE_MCP"
                : "LOCAL_OPERATION".equals(prompt.sourceType())
                        ? "LOCAL_OPERATION"
                        : "LOCAL_DICTIONARY";
        return new Resolved(
                name,
                sourceType,
                prompt.operationId(),
                security.authorizePrompt(prompt, identity)
        );
    }

    /**
     * 中文说明：执行 资源 操作；该方法是 {@code McpCompletionHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the resource operation; this method is the invocation entry point on {@code McpCompletionHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCompletionHandler.resource(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param uri 参数 uri；parameter uri。
     * @param identity 参数 身份；parameter identity。
     * @return 返回 资源 的处理结果；returns the result of the operation.
     */
    private Resolved resource(
            String serverCode,
            String uri,
            McpSecurityGate.IdentityContext identity) {
        McpResourceCatalog.ResolvedResource resolved = resources.resolve(
                serverCode,
                uri
        );
        Publisher<Void> authorization = resolved.resource() != null
                ? security.authorizeResourceRead(
                        resolved.resource(),
                        identity
                )
                : security.authorizeResourceRead(
                        resolved.template(),
                        identity
                );
        String sourceType = resolved.remoteMountId() != null
                ? "REMOTE_MCP"
                : "LOCAL_OPERATION".equals(resolved.driverType())
                        ? "LOCAL_OPERATION"
                        : "LOCAL_DICTIONARY";
        return new Resolved(
                resolved.uri(),
                sourceType,
                resolved.operationId(),
                authorization
        );
    }

    /**
     * 中文说明：执行 索引 操作；该方法是 {@code McpCompletionHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the index operation; this method is the invocation entry point on {@code McpCompletionHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCompletionHandler.index(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @return 返回 索引 的处理结果；returns the result of the operation.
     */
    private Map<String, McpCompletionProvider> index(
            List<McpCompletionProvider> source) {
        LinkedHashMap<String, McpCompletionProvider> result =
                new LinkedHashMap<>();
        Objects.requireNonNull(source, "providers").forEach(provider -> {
            if (result.putIfAbsent(
                    provider.sourceType(),
                    provider
            ) != null) {
                throw new IllegalArgumentException(
                        "MCP completion source types must be unique"
                );
            }
        });
        return Collections.unmodifiableMap(result);
    }

    /**
     * 中文说明：执行 object 操作；该方法是 {@code McpCompletionHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the object operation; this method is the invocation entry point on {@code McpCompletionHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCompletionHandler.object(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 object 的处理结果；returns the result of the operation.
     */
    private Map<?, ?> object(Object value, String field) {
        if (!(value instanceof Map<?, ?> map)) {
            throw McpPromptDriver.invalid(
                    "MCP completion " + field + " must be an object"
            );
        }
        return map;
    }

    /**
     * 中文说明：执行 string 操作；该方法是 {@code McpCompletionHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the string operation; this method is the invocation entry point on {@code McpCompletionHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCompletionHandler.string(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 string 的处理结果；returns the result of the operation.
     */
    private String string(Object value, String field) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw McpPromptDriver.invalid(
                    "MCP completion " + field + " is required"
            );
        }
        return text.trim();
    }

    /**
     * 中文说明：执行 optional 操作；该方法是 {@code McpCompletionHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the optional operation; this method is the invocation entry point on {@code McpCompletionHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCompletionHandler.optional(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 optional 的处理结果；returns the result of the operation.
     */
    private String optional(Object value) {
        if (value == null) {
            return "";
        }
        if (!(value instanceof String text)) {
            throw McpPromptDriver.invalid(
                    "MCP completion argument value must be a string"
            );
        }
        return text;
    }

    /**
     * 中文说明：执行 身份 操作；该方法是 {@code McpCompletionHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the identity operation; this method is the invocation entry point on {@code McpCompletionHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCompletionHandler.identity(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 attributes 操作；该方法是 {@code McpCompletionHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the attributes operation; this method is the invocation entry point on {@code McpCompletionHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCompletionHandler.attributes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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

    /**
     * 中文说明：{@code Resolved} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Resolved相关的职责与边界。
     * English summary: {@code Resolved} is an immutable data carrier in the current Gateway module; it owns the resolved-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param referenceName 参数 referenceName；parameter reference name。
     * @param sourceType 参数 sourceType；parameter source type。
     * @param operationId 参数 操作Id；parameter operation id。
     * @param authorization 参数 授权；parameter authorization。
     */
    private record Resolved(
            /**
             * 中文说明：保存 referenceName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpCompletionHandler.Resolved} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by reference name; its type is {@code String}, and {@code McpCompletionHandler.Resolved} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCompletionHandler.Resolved} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCompletionHandler.Resolved}; do not couple callers to its representation when the owning type exposes an API.
             */
            String referenceName,
            /**
             * 中文说明：保存 sourceType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpCompletionHandler.Resolved} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by source type; its type is {@code String}, and {@code McpCompletionHandler.Resolved} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCompletionHandler.Resolved} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCompletionHandler.Resolved}; do not couple callers to its representation when the owning type exposes an API.
             */
            String sourceType,
            /**
             * 中文说明：保存 操作Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpCompletionHandler.Resolved} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by operation id; its type is {@code String}, and {@code McpCompletionHandler.Resolved} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCompletionHandler.Resolved} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCompletionHandler.Resolved}; do not couple callers to its representation when the owning type exposes an API.
             */
            String operationId,
            /**
             * 中文说明：保存 授权 对应的状态、依赖或配置值；字段类型为 {@code Publisher<Void>}，由 {@code McpCompletionHandler.Resolved} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by authorization; its type is {@code Publisher<Void>}, and {@code McpCompletionHandler.Resolved} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCompletionHandler.Resolved} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCompletionHandler.Resolved}; do not couple callers to its representation when the owning type exposes an API.
             */
            Publisher<Void> authorization
    ) {
    }
}
