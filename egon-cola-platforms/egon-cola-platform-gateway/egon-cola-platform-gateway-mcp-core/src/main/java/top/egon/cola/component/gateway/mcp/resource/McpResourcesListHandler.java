package top.egon.cola.component.gateway.mcp.resource;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeResource;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;
import top.egon.cola.component.gateway.mcp.security.McpSecurityGate;
import top.egon.cola.component.gateway.mcp.server.McpMethodHandler;
import top.egon.cola.component.gateway.mcp.server.McpRequestContext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 中文说明：{@code McpResourcesListHandler} 是处理器，位于当前 Gateway 模块的相关包中，负责MCPResourcesList处理器相关的职责与边界。
 * English summary: {@code McpResourcesListHandler} is a mcp resources list handler handler in the current Gateway module; it owns the mcp resources list handler-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpResourcesListHandler implements McpMethodHandler {

    /**
     * 中文说明：保存 目录 对应的状态、依赖或配置值；字段类型为 {@code McpResourceCatalog}，由 {@code McpResourcesListHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by catalog; its type is {@code McpResourceCatalog}, and {@code McpResourcesListHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpResourcesListHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResourcesListHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpResourceCatalog catalog;

    /**
     * 中文说明：保存 安全 对应的状态、依赖或配置值；字段类型为 {@code McpSecurityGate}，由 {@code McpResourcesListHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by security; its type is {@code McpSecurityGate}, and {@code McpResourcesListHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpResourcesListHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResourcesListHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpSecurityGate security;

    /**
     * 中文说明：创建 {@code McpResourcesListHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpResourcesListHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param catalog 参数 目录；parameter catalog。
     * @param security 参数 安全；parameter security。
     */
    public McpResourcesListHandler(
            McpResourceCatalog catalog,
            McpSecurityGate security) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.security = Objects.requireNonNull(security, "security");
    }

    /**
     * 中文说明：执行 方法 操作；该方法是 {@code McpResourcesListHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the method operation; this method is the invocation entry point on {@code McpResourcesListHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpResourcesListHandler.method(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 方法 的处理结果；returns the result of the operation.
     */
    @Override
    public String method() {
        return "resources/list";
    }

    /**
     * 中文说明：执行 handle 操作；该方法是 {@code McpResourcesListHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the handle operation; this method is the invocation entry point on {@code McpResourcesListHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpResourcesListHandler.handle(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param context 参数 context；parameter context。
     * @return 返回 handle 的处理结果；returns the result of the operation.
     */
    @Override
    public Publisher<McpJsonRpcResponse> handle(
            McpJsonRpcRequest request,
            McpRequestContext context) {
        McpSecurityGate.IdentityContext identity = identity(context);
        return Flux.fromIterable(catalog.resources(
                        context.server().serverCode()
                ))
                .concatMap(resource -> Mono.from(
                                security.authorizeResourceRead(
                                        resource,
                                        identity
                                )
                        ).thenReturn(describe(resource))
                        .onErrorResume(
                                McpProtocolException.class,
                                ignored -> Mono.empty()
                        ))
                .collectList()
                .map(resources -> McpJsonRpcResponse.success(
                        request.id(),
                        Map.of("resources", resources)
                ));
    }

    /**
     * 中文说明：执行 describe 操作；该方法是 {@code McpResourcesListHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the describe operation; this method is the invocation entry point on {@code McpResourcesListHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpResourcesListHandler.describe(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param resource 参数 资源；parameter resource。
     * @return 返回 describe 的处理结果；returns the result of the operation.
     */
    private Map<String, Object> describe(McpRuntimeResource resource) {
        LinkedHashMap<String, Object> value = new LinkedHashMap<>();
        value.put("name", resource.name());
        value.put("uri", resource.uri());
        value.put("mimeType", resource.mimeType());
        if (resource.description() != null) {
            value.put("description", resource.description());
        }
        return Map.copyOf(value);
    }

    /**
     * 中文说明：执行 身份 操作；该方法是 {@code McpResourcesListHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the identity operation; this method is the invocation entry point on {@code McpResourcesListHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpResourcesListHandler.identity(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
