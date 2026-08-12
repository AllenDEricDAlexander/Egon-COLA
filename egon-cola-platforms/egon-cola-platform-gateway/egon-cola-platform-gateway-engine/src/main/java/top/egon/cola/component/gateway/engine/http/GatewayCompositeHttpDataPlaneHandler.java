package top.egon.cola.component.gateway.engine.http;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.engine.mcp.McpEngineHttpHandler;
import top.egon.cola.component.gateway.engine.websocket.GatewayPreparedWebSocketSession;
import top.egon.cola.component.gateway.engine.websocket.GatewayWebSocketHandshakeResult;
import top.egon.cola.component.gateway.engine.websocket.GatewayWebSocketPeer;
import top.egon.cola.component.gateway.mcp.transport.McpHttpRequest;
import top.egon.cola.component.gateway.mcp.transport.McpHttpResponse;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Front controller that reserves fixed platform paths before rule routing.
 * 补充说明 / Supplementary summary: {@code GatewayCompositeHttpDataPlaneHandler} 是处理器，位于当前 Gateway 模块的相关包中，负责网关CompositeHttpDataPlane处理器相关的职责与边界。
 * English supplement: {@code GatewayCompositeHttpDataPlaneHandler} is a gateway composite http data plane handler handler in the current Gateway module; it owns the gateway composite http data plane handler-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayCompositeHttpDataPlaneHandler
        implements GatewayHttpDataPlaneHandler {

    /**
     * 中文说明：表示 DEFAULTMCPMAXBODYBYTES 这一固定值；它属于 {@code GatewayCompositeHttpDataPlaneHandler} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value default mcp max body bytes; it is a state, type, or protocol value of {@code GatewayCompositeHttpDataPlaneHandler} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCompositeHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCompositeHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final int DEFAULT_MCP_MAX_BODY_BYTES = 2 * 1024 * 1024;

    /**
     * 中文说明：保存 MCP 对应的状态、依赖或配置值；字段类型为 {@code McpEngineHttpHandler}，由 {@code GatewayCompositeHttpDataPlaneHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by mcp; its type is {@code McpEngineHttpHandler}, and {@code GatewayCompositeHttpDataPlaneHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCompositeHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCompositeHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpEngineHttpHandler mcp;

    /**
     * 中文说明：保存 routes 对应的状态、依赖或配置值；字段类型为 {@code GatewayHttpDataPlaneHandler}，由 {@code GatewayCompositeHttpDataPlaneHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by routes; its type is {@code GatewayHttpDataPlaneHandler}, and {@code GatewayCompositeHttpDataPlaneHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCompositeHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCompositeHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayHttpDataPlaneHandler routes;

    /**
     * 中文说明：保存 maximumMCPBodyBytes 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayCompositeHttpDataPlaneHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by maximum mcp body bytes; its type is {@code int}, and {@code GatewayCompositeHttpDataPlaneHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCompositeHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCompositeHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final int maximumMcpBodyBytes;

    /**
     * 中文说明：创建 {@code GatewayCompositeHttpDataPlaneHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayCompositeHttpDataPlaneHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param mcp 参数 MCP；parameter mcp。
     * @param routes 参数 routes；parameter routes。
     */
    public GatewayCompositeHttpDataPlaneHandler(
            McpEngineHttpHandler mcp,
            GatewayHttpDataPlaneHandler routes) {
        this(mcp, routes, DEFAULT_MCP_MAX_BODY_BYTES);
    }

    /**
     * 中文说明：创建 {@code GatewayCompositeHttpDataPlaneHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayCompositeHttpDataPlaneHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param mcp 参数 MCP；parameter mcp。
     * @param routes 参数 routes；parameter routes。
     * @param maximumMcpBodyBytes 参数 maximumMCPBodyBytes；parameter maximum mcp body bytes。
     */
    public GatewayCompositeHttpDataPlaneHandler(
            McpEngineHttpHandler mcp,
            GatewayHttpDataPlaneHandler routes,
            int maximumMcpBodyBytes) {
        this.mcp = Objects.requireNonNull(mcp, "mcp");
        this.routes = Objects.requireNonNull(routes, "routes");
        if (maximumMcpBodyBytes < 1) {
            throw new IllegalArgumentException(
                    "maximumMcpBodyBytes must be positive"
            );
        }
        this.maximumMcpBodyBytes = maximumMcpBodyBytes;
    }

    /**
     * 中文说明：执行 handle 操作；该方法是 {@code GatewayCompositeHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the handle operation; this method is the invocation entry point on {@code GatewayCompositeHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCompositeHttpDataPlaneHandler.handle(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param accessZone 参数 accessZone；parameter access zone。
     * @param request 参数 请求；parameter request。
     * @return 返回 handle 的处理结果；returns the result of the operation.
     */
    @Override
    public Mono<GatewayOutboundHttpResponse> handle(
            AccessZone accessZone,
            GatewayInboundHttpRequest request) {
        if (!mcp.supports(request.uri())) {
            return routes.handle(accessZone, request);
        }
        return aggregate(request)
                .flatMap(body -> mcp.handle(new McpHttpRequest(
                        request.method(),
                        request.uri(),
                        firstHeaders(request.headers()),
                        body,
                        Map.of(
                                "accessZone", accessZone.name(),
                                "remoteAddress", remoteAddress(request)
                        )
                )))
                .map(this::adapt)
                .onErrorResume(
                        DataBufferLimitException.class,
                        ignored -> Mono.just(new GatewayOutboundHttpResponse(
                                413,
                                Map.of("content-type", List.of(
                                        "application/json; charset=UTF-8"
                                )),
                                Flux.just(DefaultDataBufferFactory
                                        .sharedInstance.wrap(
                                                ("{\"error\":"
                                                        + "\"MCP_BODY_TOO_LARGE"
                                                        + "\"}").getBytes(
                                                        StandardCharsets.UTF_8
                                                )
                                        ))
                        ))
                );
    }

    /**
     * 中文说明：执行 prepareWebSocket 操作；该方法是 {@code GatewayCompositeHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the prepare web socket operation; this method is the invocation entry point on {@code GatewayCompositeHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCompositeHttpDataPlaneHandler.prepareWebSocket(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param accessZone 参数 accessZone；parameter access zone。
     * @param request 参数 请求；parameter request。
     * @return 返回 prepareWebSocket 的处理结果；returns the result of the operation.
     */
    @Override
    public Mono<GatewayWebSocketHandshakeResult> prepareWebSocket(
            AccessZone accessZone,
            GatewayInboundHttpRequest request) {
        return routes.prepareWebSocket(accessZone, request);
    }

    /**
     * 中文说明：执行 bridgeWebSocket 操作；该方法是 {@code GatewayCompositeHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the bridge web socket operation; this method is the invocation entry point on {@code GatewayCompositeHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCompositeHttpDataPlaneHandler.bridgeWebSocket(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param upstream 参数 upstream；parameter upstream。
     * @param downstream 参数 downstream；parameter downstream。
     * @return 返回 bridgeWebSocket 的处理结果；returns the result of the operation.
     */
    @Override
    public Mono<Void> bridgeWebSocket(
            GatewayPreparedWebSocketSession upstream,
            GatewayWebSocketPeer downstream) {
        return routes.bridgeWebSocket(upstream, downstream);
    }

    /**
     * 中文说明：执行 aggregate 操作；该方法是 {@code GatewayCompositeHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the aggregate operation; this method is the invocation entry point on {@code GatewayCompositeHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCompositeHttpDataPlaneHandler.aggregate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 aggregate 的处理结果；returns the result of the operation.
     */
    private Mono<String> aggregate(GatewayInboundHttpRequest request) {
        return DataBufferUtils.join(request.body(), maximumMcpBodyBytes)
                .map(buffer -> {
                    try {
                        byte[] bytes = new byte[buffer.readableByteCount()];
                        buffer.read(bytes);
                        return new String(bytes, StandardCharsets.UTF_8);
                    } finally {
                        DataBufferUtils.release(buffer);
                    }
                })
                .defaultIfEmpty("");
    }

    /**
     * 中文说明：执行 adapt 操作；该方法是 {@code GatewayCompositeHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the adapt operation; this method is the invocation entry point on {@code GatewayCompositeHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCompositeHttpDataPlaneHandler.adapt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param response 参数 响应；parameter response。
     * @return 返回 adapt 的处理结果；returns the result of the operation.
     */
    private GatewayOutboundHttpResponse adapt(McpHttpResponse response) {
        Flux<DataBuffer> body = Flux.from(response.body())
                .map(bytes -> DefaultDataBufferFactory.sharedInstance.wrap(
                        bytes
                ));
        GatewayOutboundHttpResponse adapted =
                new GatewayOutboundHttpResponse(
                        response.status(),
                        response.headers(),
                        body
                );
        return response.flushPerEvent()
                ? adapted.withFlushMode(GatewayHttpFlushMode.PER_BUFFER)
                : adapted;
    }

    /**
     * 中文说明：执行 firstHeaders 操作；该方法是 {@code GatewayCompositeHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the first headers operation; this method is the invocation entry point on {@code GatewayCompositeHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCompositeHttpDataPlaneHandler.firstHeaders(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param headers 参数 headers；parameter headers。
     * @return 返回 firstHeaders 的处理结果；returns the result of the operation.
     */
    private Map<String, String> firstHeaders(
            Map<String, List<String>> headers) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        headers.forEach((name, values) -> {
            if (!values.isEmpty()) {
                result.put(name, values.getFirst());
            }
        });
        return Map.copyOf(result);
    }

    /**
     * 中文说明：执行 远程Address 操作；该方法是 {@code GatewayCompositeHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the remote address operation; this method is the invocation entry point on {@code GatewayCompositeHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCompositeHttpDataPlaneHandler.remoteAddress(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 远程Address 的处理结果；returns the result of the operation.
     */
    private String remoteAddress(GatewayInboundHttpRequest request) {
        return request.remoteAddress() == null
                ? "unknown"
                : request.remoteAddress().getAddress().getHostAddress();
    }
}
