package top.egon.cola.component.gateway.mcp.subscription.service;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.mcp.common.protocol.McpProtocolException;
import top.egon.cola.component.gateway.mcp.resource.service.McpResourceCatalog;
import top.egon.cola.component.gateway.mcp.resource.service.McpResourceDriver;
import top.egon.cola.component.gateway.mcp.common.security.McpSecurityGate;
import top.egon.cola.component.gateway.mcp.server.service.McpMethodHandler;
import top.egon.cola.component.gateway.mcp.server.domain.McpRequestContext;

import java.util.Map;
import java.util.Objects;

/**
 * 中文说明：{@code McpSubscriptionsListenHandler} 是处理器，位于当前 Gateway 模块的相关包中，负责MCPSubscriptionsListen处理器相关的职责与边界。
 * English summary: {@code McpSubscriptionsListenHandler} is a mcp subscriptions listen handler handler in the current Gateway module; it owns the mcp subscriptions listen handler-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpSubscriptionsListenHandler implements McpMethodHandler {

    /**
     * 中文说明：保存 目录 对应的状态、依赖或配置值；字段类型为 {@code McpResourceCatalog}，由 {@code McpSubscriptionsListenHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by catalog; its type is {@code McpResourceCatalog}, and {@code McpSubscriptionsListenHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpSubscriptionsListenHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSubscriptionsListenHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpResourceCatalog catalog;

    /**
     * 中文说明：保存 subscriptions 对应的状态、依赖或配置值；字段类型为 {@code McpSubscriptionService}，由 {@code McpSubscriptionsListenHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by subscriptions; its type is {@code McpSubscriptionService}, and {@code McpSubscriptionsListenHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpSubscriptionsListenHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSubscriptionsListenHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpSubscriptionService subscriptions;

    /**
     * 中文说明：保存 安全 对应的状态、依赖或配置值；字段类型为 {@code McpSecurityGate}，由 {@code McpSubscriptionsListenHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by security; its type is {@code McpSecurityGate}, and {@code McpSubscriptionsListenHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpSubscriptionsListenHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSubscriptionsListenHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpSecurityGate security;

    /**
     * 中文说明：创建 {@code McpSubscriptionsListenHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpSubscriptionsListenHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param catalog 参数 目录；parameter catalog。
     * @param subscriptions 参数 subscriptions；parameter subscriptions。
     * @param security 参数 安全；parameter security。
     */
    public McpSubscriptionsListenHandler(
            McpResourceCatalog catalog,
            McpSubscriptionService subscriptions,
            McpSecurityGate security) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.subscriptions = Objects.requireNonNull(
                subscriptions,
                "subscriptions"
        );
        this.security = Objects.requireNonNull(security, "security");
    }

    /**
     * 中文说明：执行 方法 操作；该方法是 {@code McpSubscriptionsListenHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the method operation; this method is the invocation entry point on {@code McpSubscriptionsListenHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSubscriptionsListenHandler.method(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 方法 的处理结果；returns the result of the operation.
     */
    @Override
    public String method() {
        return "subscriptions/listen";
    }

    /**
     * 中文说明：执行 handle 操作；该方法是 {@code McpSubscriptionsListenHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the handle operation; this method is the invocation entry point on {@code McpSubscriptionsListenHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSubscriptionsListenHandler.handle(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param context 参数 context；parameter context。
     * @return 返回 handle 的处理结果；returns the result of the operation.
     */
    @Override
    public Publisher<McpJsonRpcResponse> handle(
            McpJsonRpcRequest request,
            McpRequestContext context) {
        String uri = string(request.params().get("uri"));
        String cursor = optional(request.params().get("cursor"));
        McpResourceCatalog.ResolvedResource resolved = catalog.resolve(
                context.server().serverCode(),
                uri
        );
        McpSecurityGate.IdentityContext identity = identity(context);
        Publisher<Void> authorization = resolved.resource() != null
                ? security.authorizeResourceRead(
                resolved.resource(),
                identity
        )
                : security.authorizeResourceRead(
                resolved.template(),
                identity
        );
        return reactor.core.publisher.Mono.from(authorization)
                .thenMany(Flux.from(subscriptions.listen(
                        resolved.uri(),
                        cursor
                )))
                .take(100)
                .map(event -> Map.of(
                        "eventId", event.eventId(),
                        "uri", event.uri(),
                        "kind", event.kind(),
                        "occurredAt", event.occurredAt().toString()
                ))
                .collectList()
                .map(events -> McpJsonRpcResponse.success(
                        request.id(),
                        Map.of("events", events)
                ));
    }

    /**
     * 中文说明：执行 身份 操作；该方法是 {@code McpSubscriptionsListenHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the identity operation; this method is the invocation entry point on {@code McpSubscriptionsListenHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSubscriptionsListenHandler.identity(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 string 操作；该方法是 {@code McpSubscriptionsListenHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the string operation; this method is the invocation entry point on {@code McpSubscriptionsListenHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSubscriptionsListenHandler.string(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 string 的处理结果；returns the result of the operation.
     */
    private String string(Object value) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw McpResourceDriver.rejected("MCP resource URI is required");
        }
        return text.trim();
    }

    /**
     * 中文说明：执行 optional 操作；该方法是 {@code McpSubscriptionsListenHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the optional operation; this method is the invocation entry point on {@code McpSubscriptionsListenHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSubscriptionsListenHandler.optional(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 optional 的处理结果；returns the result of the operation.
     */
    private String optional(Object value) {
        return value instanceof String text && !text.isBlank()
                ? text.trim()
                : null;
    }
}
