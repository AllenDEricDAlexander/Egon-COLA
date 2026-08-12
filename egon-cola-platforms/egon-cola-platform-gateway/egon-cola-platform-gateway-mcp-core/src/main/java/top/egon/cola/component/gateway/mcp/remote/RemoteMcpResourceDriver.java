package top.egon.cola.component.gateway.mcp.remote;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeResource;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeResourceTemplate;
import top.egon.cola.component.gateway.core.mcp.remote.RemoteAuthProvider;
import top.egon.cola.component.gateway.mcp.resource.McpResourceDriver;
import top.egon.cola.component.gateway.mcp.rule.CompiledMcpRules;
import top.egon.cola.component.gateway.mcp.security.McpSecurityGate;
import top.egon.cola.component.gateway.mcp.telemetry.McpTelemetry;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Resource Strategy for reviewed REMOTE_MCP descriptors and Apps.
 * 补充说明 / Supplementary summary: {@code RemoteMcpResourceDriver} 是驱动器，位于当前 Gateway 模块的相关包中，负责远程MCP资源驱动器相关的职责与边界。
 * English supplement: {@code RemoteMcpResourceDriver} is a remote mcp resource driver driver in the current Gateway module; it owns the remote mcp resource driver-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class RemoteMcpResourceDriver implements McpResourceDriver {

    /**
     * 中文说明：表示 驱动器TYPE 这一固定值；它属于 {@code RemoteMcpResourceDriver} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value driver type; it is a state, type, or protocol value of {@code RemoteMcpResourceDriver} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code RemoteMcpResourceDriver} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RemoteMcpResourceDriver}; do not couple callers to its representation when the owning type exposes an API.
     */
    public static final String DRIVER_TYPE = "REMOTE_MCP";

    /**
     * 中文说明：保存 rules 对应的状态、依赖或配置值；字段类型为 {@code Supplier<CompiledMcpRules>}，由 {@code RemoteMcpResourceDriver} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rules; its type is {@code Supplier<CompiledMcpRules>}, and {@code RemoteMcpResourceDriver} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RemoteMcpResourceDriver} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RemoteMcpResourceDriver}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Supplier<CompiledMcpRules> rules;

    /**
     * 中文说明：保存 clients 对应的状态、依赖或配置值；字段类型为 {@code McpRemoteClientPool}，由 {@code RemoteMcpResourceDriver} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clients; its type is {@code McpRemoteClientPool}, and {@code RemoteMcpResourceDriver} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RemoteMcpResourceDriver} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RemoteMcpResourceDriver}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpRemoteClientPool clients;

    /**
     * 中文说明：保存 router 对应的状态、依赖或配置值；字段类型为 {@code McpNamespaceRouter}，由 {@code RemoteMcpResourceDriver} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by router; its type is {@code McpNamespaceRouter}, and {@code RemoteMcpResourceDriver} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RemoteMcpResourceDriver} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RemoteMcpResourceDriver}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpNamespaceRouter router;

    /**
     * 中文说明：保存 translator 对应的状态、依赖或配置值；字段类型为 {@code McpDialectTranslator}，由 {@code RemoteMcpResourceDriver} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by translator; its type is {@code McpDialectTranslator}, and {@code RemoteMcpResourceDriver} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RemoteMcpResourceDriver} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RemoteMcpResourceDriver}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpDialectTranslator translator;

    /**
     * 中文说明：创建 {@code RemoteMcpResourceDriver} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RemoteMcpResourceDriver} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param rules 参数 rules；parameter rules。
     * @param clients 参数 clients；parameter clients。
     * @param router 参数 router；parameter router。
     * @param translator 参数 translator；parameter translator。
     */
    public RemoteMcpResourceDriver(
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
     * 中文说明：执行 驱动器Type 操作；该方法是 {@code RemoteMcpResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the driver type operation; this method is the invocation entry point on {@code RemoteMcpResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RemoteMcpResourceDriver.driverType(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 驱动器Type 的处理结果；returns the result of the operation.
     */
    @Override
    public String driverType() {
        return DRIVER_TYPE;
    }

    /**
     * 中文说明：执行 read 操作；该方法是 {@code RemoteMcpResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the read operation; this method is the invocation entry point on {@code RemoteMcpResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RemoteMcpResourceDriver.read(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 read 的处理结果；returns the result of the operation.
     */
    @Override
    public Publisher<Content> read(ReadRequest request) {
        Descriptor descriptor = descriptor(request.serverCode(), request.name());
        McpNamespaceRouter.Binding binding = router.binding(
                active(),
                descriptor.remoteMountId(),
                descriptor.primitiveType(),
                descriptor.name()
        );
        String remoteUri = descriptor.configuration().getOrDefault(
                "remoteUri",
                request.uri()
        );
        McpDialectTranslator.OutboundCall call = translator.outbound(
                dialect(request.attributes()),
                binding.provider().dialect(),
                "resources/read",
                Map.of("uri", remoteUri),
                Map.of(),
                trace(request.attributes())
        );
        McpSecurityGate.IdentityContext identity = identity(
                request.attributes()
        );
        McpTelemetry.Scope telemetry = McpTelemetry.current(
                request.attributes()
        );
        telemetry.remoteProvider(binding.provider().providerCode());
        var exchange = clients.exchange(
                binding.provider(),
                call,
                auth(identity)
        );
        return Mono.from(McpTelemetry.observeChild(
                        telemetry,
                        McpTelemetry.ChildKind.REMOTE,
                        exchange
                ))
                .map(translator::result)
                .map(result -> content(request, result));
    }

    /**
     * 中文说明：执行 content 操作；该方法是 {@code RemoteMcpResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the content operation; this method is the invocation entry point on {@code RemoteMcpResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RemoteMcpResourceDriver.content(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param result 参数 result；parameter result。
     * @return 返回 content 的处理结果；returns the result of the operation.
     */
    private Content content(
            ReadRequest request,
            Map<String, Object> result) {
        Object raw = result.get("contents");
        if (!(raw instanceof List<?> contents) || contents.isEmpty()
                || !(contents.getFirst() instanceof Map<?, ?> source)) {
            throw McpResourceDriver.rejected(
                    "remote MCP resource content is invalid"
            );
        }
        String mimeType = source.get("mimeType") instanceof String mime
                && !mime.isBlank()
                ? mime.trim()
                : request.mimeType();
        byte[] bytes;
        boolean textual;
        if (source.get("text") instanceof String text) {
            bytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            textual = true;
        } else if (source.get("blob") instanceof String blob) {
            try {
                bytes = Base64.getDecoder().decode(blob);
            } catch (IllegalArgumentException failure) {
                throw McpResourceDriver.rejected(
                        "remote MCP resource blob is invalid"
                );
            }
            textual = false;
        } else {
            throw McpResourceDriver.rejected(
                    "remote MCP resource content is missing"
            );
        }
        if (bytes.length > request.maximumBytes()) {
            throw McpResourceDriver.rejected(
                    "remote MCP resource exceeds its maximum size"
            );
        }
        Map<String, Object> metadata = source.get("_meta") instanceof Map<?, ?>
                ? stringObjectMap((Map<?, ?>) source.get("_meta"))
                : Map.of();
        return new Content(
                request.uri(),
                mimeType,
                bytes,
                textual,
                metadata
        );
    }

    /**
     * 中文说明：执行 descriptor 操作；该方法是 {@code RemoteMcpResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the descriptor operation; this method is the invocation entry point on {@code RemoteMcpResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RemoteMcpResourceDriver.descriptor(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param name 参数 name；parameter name。
     * @return 返回 descriptor 的处理结果；returns the result of the operation.
     */
    private Descriptor descriptor(String serverCode, String name) {
        CompiledMcpRules current = active();
        String key = CompiledMcpRules.qualified(serverCode, name);
        McpRuntimeResource resource = current.resourcesByQualifiedName().get(
                key
        );
        if (resource != null && resource.remoteMountId() != null) {
            return new Descriptor(
                    resource.name(),
                    resource.remoteMountId(),
                    "RESOURCE",
                    resource.configuration()
            );
        }
        McpRuntimeResourceTemplate template =
                current.templatesByQualifiedName().get(key);
        if (template != null && template.remoteMountId() != null) {
            return new Descriptor(
                    template.name(),
                    template.remoteMountId(),
                    "RESOURCE_TEMPLATE",
                    template.configuration()
            );
        }
        throw McpResourceDriver.rejected(
                "remote MCP resource descriptor was not found"
        );
    }

    /**
     * 中文说明：执行 stringObjectMap 操作；该方法是 {@code RemoteMcpResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the string object map operation; this method is the invocation entry point on {@code RemoteMcpResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RemoteMcpResourceDriver.stringObjectMap(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @return 返回 stringObjectMap 的处理结果；returns the result of the operation.
     */
    private Map<String, Object> stringObjectMap(Map<?, ?> source) {
        java.util.LinkedHashMap<String, Object> result =
                new java.util.LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key instanceof String name) {
                result.put(name, value);
            }
        });
        return Map.copyOf(result);
    }

    /**
     * 中文说明：执行 active 操作；该方法是 {@code RemoteMcpResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the active operation; this method is the invocation entry point on {@code RemoteMcpResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RemoteMcpResourceDriver.active(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 active 的处理结果；returns the result of the operation.
     */
    private CompiledMcpRules active() {
        CompiledMcpRules value = rules.get();
        return value == null ? CompiledMcpRules.empty() : value;
    }

    /**
     * 中文说明：执行 身份 操作；该方法是 {@code RemoteMcpResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the identity operation; this method is the invocation entry point on {@code RemoteMcpResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RemoteMcpResourceDriver.identity(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param attributes 参数 attributes；parameter attributes。
     * @return 返回 身份 的处理结果；returns the result of the operation.
     */
    private McpSecurityGate.IdentityContext identity(
            Map<String, Object> attributes) {
        return McpSecurityGate.IdentityContext.from(attributes);
    }

    /**
     * 中文说明：执行 认证 操作；该方法是 {@code RemoteMcpResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the auth operation; this method is the invocation entry point on {@code RemoteMcpResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RemoteMcpResourceDriver.auth(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param identity 参数 身份；parameter identity。
     * @return 返回 认证 的处理结果；returns the result of the operation.
     */
    private RemoteAuthProvider.AuthContext auth(
            McpSecurityGate.IdentityContext identity) {
        return new RemoteAuthProvider.AuthContext(
                identity.subjectId(),
                identity.tenantId(),
                identity.clientId()
        );
    }

    /**
     * 中文说明：执行 dialect 操作；该方法是 {@code RemoteMcpResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the dialect operation; this method is the invocation entry point on {@code RemoteMcpResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RemoteMcpResourceDriver.dialect(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 trace 操作；该方法是 {@code RemoteMcpResourceDriver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the trace operation; this method is the invocation entry point on {@code RemoteMcpResourceDriver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RemoteMcpResourceDriver.trace(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：{@code Descriptor} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Descriptor相关的职责与边界。
     * English summary: {@code Descriptor} is an immutable data carrier in the current Gateway module; it owns the descriptor-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param name 参数 name；parameter name。
     * @param remoteMountId 参数 远程MountId；parameter remote mount id。
     * @param primitiveType 参数 primitiveType；parameter primitive type。
     * @param configuration 参数 配置；parameter configuration。
     */
    private record Descriptor(
            /**
             * 中文说明：保存 name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RemoteMcpResourceDriver.Descriptor} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by name; its type is {@code String}, and {@code RemoteMcpResourceDriver.Descriptor} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code RemoteMcpResourceDriver.Descriptor} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RemoteMcpResourceDriver.Descriptor}; do not couple callers to its representation when the owning type exposes an API.
             */
            String name,
            /**
             * 中文说明：保存 远程MountId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RemoteMcpResourceDriver.Descriptor} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by remote mount id; its type is {@code String}, and {@code RemoteMcpResourceDriver.Descriptor} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code RemoteMcpResourceDriver.Descriptor} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RemoteMcpResourceDriver.Descriptor}; do not couple callers to its representation when the owning type exposes an API.
             */
            String remoteMountId,
            /**
             * 中文说明：保存 primitiveType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RemoteMcpResourceDriver.Descriptor} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by primitive type; its type is {@code String}, and {@code RemoteMcpResourceDriver.Descriptor} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code RemoteMcpResourceDriver.Descriptor} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RemoteMcpResourceDriver.Descriptor}; do not couple callers to its representation when the owning type exposes an API.
             */
            String primitiveType,
            /**
             * 中文说明：保存 配置 对应的状态、依赖或配置值；字段类型为 {@code Map<String, String>}，由 {@code RemoteMcpResourceDriver.Descriptor} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by configuration; its type is {@code Map<String, String>}, and {@code RemoteMcpResourceDriver.Descriptor} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code RemoteMcpResourceDriver.Descriptor} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RemoteMcpResourceDriver.Descriptor}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, String> configuration
    ) {
    }
}
