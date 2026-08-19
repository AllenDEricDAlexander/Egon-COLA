package top.egon.cola.component.gateway.mcp.resource.service;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.mcp.common.protocol.McpProtocolException;
import top.egon.cola.component.gateway.mcp.common.security.McpSecurityGate;
import top.egon.cola.component.gateway.mcp.server.service.McpMethodHandler;
import top.egon.cola.component.gateway.mcp.server.domain.McpRequestContext;
import top.egon.cola.component.gateway.mcp.common.telemetry.McpTelemetry;

import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 中文说明：{@code McpResourcesReadHandler} 是处理器，位于当前 Gateway 模块的相关包中，负责MCPResourcesRead处理器相关的职责与边界。
 * English summary: {@code McpResourcesReadHandler} is a mcp resources read handler handler in the current Gateway module; it owns the mcp resources read handler-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpResourcesReadHandler implements McpMethodHandler {

    /**
     * 中文说明：保存 目录 对应的状态、依赖或配置值；字段类型为 {@code McpResourceCatalog}，由 {@code McpResourcesReadHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by catalog; its type is {@code McpResourceCatalog}, and {@code McpResourcesReadHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpResourcesReadHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResourcesReadHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpResourceCatalog catalog;

    /**
     * 中文说明：保存 drivers 对应的状态、依赖或配置值；字段类型为 {@code Map<String, McpResourceDriver>}，由 {@code McpResourcesReadHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by drivers; its type is {@code Map<String, McpResourceDriver>}, and {@code McpResourcesReadHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpResourcesReadHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResourcesReadHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Map<String, McpResourceDriver> drivers;

    /**
     * 中文说明：保存 安全 对应的状态、依赖或配置值；字段类型为 {@code McpSecurityGate}，由 {@code McpResourcesReadHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by security; its type is {@code McpSecurityGate}, and {@code McpResourcesReadHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpResourcesReadHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResourcesReadHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpSecurityGate security;

    /**
     * 中文说明：创建 {@code McpResourcesReadHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpResourcesReadHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param catalog 参数 目录；parameter catalog。
     * @param drivers 参数 drivers；parameter drivers。
     * @param security 参数 安全；parameter security。
     */
    public McpResourcesReadHandler(
            McpResourceCatalog catalog,
            List<McpResourceDriver> drivers,
            McpSecurityGate security) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.drivers = index(drivers);
        this.security = Objects.requireNonNull(security, "security");
    }

    /**
     * 中文说明：执行 方法 操作；该方法是 {@code McpResourcesReadHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the method operation; this method is the invocation entry point on {@code McpResourcesReadHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpResourcesReadHandler.method(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 方法 的处理结果；returns the result of the operation.
     */
    @Override
    public String method() {
        return "resources/read";
    }

    /**
     * 中文说明：执行 handle 操作；该方法是 {@code McpResourcesReadHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the handle operation; this method is the invocation entry point on {@code McpResourcesReadHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpResourcesReadHandler.handle(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param context 参数 context；parameter context。
     * @return 返回 handle 的处理结果；returns the result of the operation.
     */
    @Override
    public Publisher<McpJsonRpcResponse> handle(
            McpJsonRpcRequest request,
            McpRequestContext context) {
        String uri = string(request.params().get("uri"));
        McpResourceCatalog.ResolvedResource resolved = catalog.resolve(
                context.server().serverCode(),
                uri
        );
        McpSecurityGate.IdentityContext identity;
        try {
            identity = McpSecurityGate.IdentityContext.from(
                    context.attributes()
            );
        } catch (IllegalArgumentException failure) {
            throw new McpProtocolException(
                    McpErrorCode.MCP_UNAUTHENTICATED,
                    "MCP identity context is incomplete"
            );
        }
        McpResourceDriver driver = drivers.get(resolved.driverType());
        if (driver == null) {
            throw McpResourceDriver.rejected(
                    "MCP resource driver is unavailable"
            );
        }
        Publisher<Void> authorization = resolved.resource() != null
                ? security.authorizeResourceRead(
                resolved.resource(),
                identity
        )
                : security.authorizeResourceRead(
                resolved.template(),
                identity
        );
        Map<String, Object> attributes = attributes(context);
        Publisher<McpResourceDriver.Content> content = driver.read(
                resolved.request(attributes)
        );
        if ("LOCAL_OPERATION".equals(driver.driverType())) {
            content = McpTelemetry.observeChild(
                    attributes,
                    McpTelemetry.ChildKind.OPERATION,
                    content
            );
        } else if ("APP_UI".equals(driver.driverType())) {
            content = McpTelemetry.observeChild(
                    attributes,
                    McpTelemetry.ChildKind.ARTIFACT,
                    content
            );
        }
        return Mono.from(authorization)
                .then(Mono.from(content))
                .map(resolvedContent -> McpJsonRpcResponse.success(
                        request.id(),
                        Map.of("contents", List.of(describe(resolvedContent)))
                ));
    }

    /**
     * 中文说明：执行 describe 操作；该方法是 {@code McpResourcesReadHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the describe operation; this method is the invocation entry point on {@code McpResourcesReadHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpResourcesReadHandler.describe(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @return 返回 describe 的处理结果；returns the result of the operation.
     */
    private Map<String, Object> describe(McpResourceDriver.Content content) {
        LinkedHashMap<String, Object> value = new LinkedHashMap<>();
        value.put("uri", content.uri());
        value.put("mimeType", content.mimeType());
        if (content.textual()) {
            value.put("text", content.text());
        } else {
            value.put("blob", Base64.getEncoder().encodeToString(
                    content.data()
            ));
        }
        if (!content.metadata().isEmpty()) {
            value.put("_meta", content.metadata());
        }
        return Map.copyOf(value);
    }

    /**
     * 中文说明：执行 索引 操作；该方法是 {@code McpResourcesReadHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the index operation; this method is the invocation entry point on {@code McpResourcesReadHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpResourcesReadHandler.index(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @return 返回 索引 的处理结果；returns the result of the operation.
     */
    private Map<String, McpResourceDriver> index(
            List<McpResourceDriver> source) {
        LinkedHashMap<String, McpResourceDriver> result = new LinkedHashMap<>();
        Objects.requireNonNull(source, "drivers").forEach(driver -> {
            String type = driver.driverType();
            if (type == null || type.isBlank()
                    || result.putIfAbsent(type.trim(), driver) != null) {
                throw new IllegalArgumentException(
                        "MCP resource driver types must be unique"
                );
            }
        });
        return Collections.unmodifiableMap(result);
    }

    /**
     * 中文说明：执行 string 操作；该方法是 {@code McpResourcesReadHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the string operation; this method is the invocation entry point on {@code McpResourcesReadHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpResourcesReadHandler.string(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 attributes 操作；该方法是 {@code McpResourcesReadHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the attributes operation; this method is the invocation entry point on {@code McpResourcesReadHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpResourcesReadHandler.attributes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
