package top.egon.cola.component.gateway.engine.http;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.WebsocketServerSpec;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.engine.http.buffer.GatewayDataBufferOwnership;
import top.egon.cola.component.gateway.engine.security.GatewayTransportSecurity;
import top.egon.cola.component.gateway.engine.websocket.GatewayPreparedWebSocketSession;
import top.egon.cola.component.gateway.engine.websocket.GatewayWebSocketHandshakeResult;
import top.egon.cola.component.gateway.engine.websocket.ReactorNettyWebSocketPeer;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 中文说明：{@code GatewayHttpListener} 是监听器，位于当前 Gateway 模块的相关包中，负责网关Http监听器相关的职责与边界。
 * English summary: {@code GatewayHttpListener} is a gateway http listener listener in the current Gateway module; it owns the gateway http listener-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayHttpListener implements AutoCloseable {

    /**
     * 中文说明：表示 缓冲区工厂 这一固定值；它属于 {@code GatewayHttpListener} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value buffer factory; it is a state, type, or protocol value of {@code GatewayHttpListener} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayHttpListener} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpListener}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final NettyDataBufferFactory BUFFER_FACTORY =
            new NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT);

    /**
     * 中文说明：保存 accessZone 对应的状态、依赖或配置值；字段类型为 {@code AccessZone}，由 {@code GatewayHttpListener} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by access zone; its type is {@code AccessZone}, and {@code GatewayHttpListener} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayHttpListener} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpListener}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AccessZone accessZone;

    /**
     * 中文说明：保存 properties 对应的状态、依赖或配置值；字段类型为 {@code GatewayHttpEngineProperties.Listener}，由 {@code GatewayHttpListener} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by properties; its type is {@code GatewayHttpEngineProperties.Listener}, and {@code GatewayHttpListener} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayHttpListener} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpListener}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayHttpEngineProperties.Listener properties;

    /**
     * 中文说明：保存 处理器 对应的状态、依赖或配置值；字段类型为 {@code GatewayHttpDataPlaneHandler}，由 {@code GatewayHttpListener} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by handler; its type is {@code GatewayHttpDataPlaneHandler}, and {@code GatewayHttpListener} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayHttpListener} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpListener}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayHttpDataPlaneHandler handler;

    /**
     * 中文说明：保存 服务器 对应的状态、依赖或配置值；字段类型为 {@code DisposableServer}，由 {@code GatewayHttpListener} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by server; its type is {@code DisposableServer}, and {@code GatewayHttpListener} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayHttpListener} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpListener}; do not couple callers to its representation when the owning type exposes an API.
     */
    private DisposableServer server;

    /**
     * 中文说明：创建 {@code GatewayHttpListener} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayHttpListener} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param accessZone 参数 accessZone；parameter access zone。
     * @param properties 参数 properties；parameter properties。
     * @param handler 参数 处理器；parameter handler。
     */
    public GatewayHttpListener(
            AccessZone accessZone,
            GatewayHttpEngineProperties.Listener properties,
            GatewayHttpDataPlaneHandler handler) {
        this.accessZone = Objects.requireNonNull(accessZone, "accessZone");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.handler = Objects.requireNonNull(handler, "handler");
    }

    /**
     * 中文说明：执行 start 操作；该方法是 {@code GatewayHttpListener} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the start operation; this method is the invocation entry point on {@code GatewayHttpListener} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpListener.start(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    public synchronized void start() {
        if (!properties.enabled() || server != null) {
            return;
        }
        HttpServer httpServer = HttpServer.create()
                .host(properties.host())
                .port(properties.port());
        GatewayTransportSecurity security =
                properties.transportSecurity();
        if (security.enabled()) {
            try {
                SslContextBuilder context = SslContextBuilder.forServer(
                        security.certificateChainFile().toFile(),
                        security.privateKeyFile().toFile()
                );
                if (security.clientCertificateRequired()) {
                    context.trustManager(
                            security.trustCertificateCollectionFile().toFile()
                    ).clientAuth(ClientAuth.REQUIRE);
                }
                SslContext sslContext = context.build();
                httpServer = httpServer.secure(spec ->
                        spec.sslContext(sslContext)
                );
            } catch (SSLException failure) {
                throw new IllegalStateException(
                        "failed to configure " + accessZone + " HTTP TLS",
                        failure
                );
            }
        }
        server = httpServer
                .handle((request, response) -> {
                    Map<String, List<String>> headers = new LinkedHashMap<>();
                    request.requestHeaders().forEach(entry ->
                            headers.computeIfAbsent(
                                    entry.getKey(),
                                    ignored -> new ArrayList<>()
                            ).add(entry.getValue())
                    );
                    GatewayInboundHttpRequest inbound =
                            new GatewayInboundHttpRequest(
                                    request.method().name(),
                                    request.requestHeaders()
                                            .get("host", ""),
                                    request.uri(),
                                    headers,
                                    remoteAddress(request.remoteAddress()),
                                    request.receive()
                                            .map(buffer ->
                                                    GatewayDataBufferOwnership
                                                            .retainAndWrap(
                                                                    BUFFER_FACTORY,
                                                                    buffer
                                                            ))
                            );
                    if (webSocketUpgrade(inbound)) {
                        return handleWebSocket(inbound, response);
                    }
                    return handler.handle(accessZone, inbound)
                            .doOnDiscard(
                                    GatewayOutboundHttpResponse.class,
                                    GatewayOutboundHttpResponse::abandon
                            )
                            .flatMap(outbound -> writeHttp(response, outbound));
                })
                .bindNow();
    }

    /**
     * 中文说明：执行 port 操作；该方法是 {@code GatewayHttpListener} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the port operation; this method is the invocation entry point on {@code GatewayHttpListener} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpListener.port(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 port 的处理结果；returns the result of the operation.
     */
    public synchronized int port() {
        return server == null ? -1 : server.port();
    }

    /**
     * 中文说明：执行 running 操作；该方法是 {@code GatewayHttpListener} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the running operation; this method is the invocation entry point on {@code GatewayHttpListener} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpListener.running(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 running 的处理结果；returns the result of the operation.
     */
    public synchronized boolean running() {
        return server != null && !server.isDisposed();
    }

    /**
     * 中文说明：执行 accessZone 操作；该方法是 {@code GatewayHttpListener} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the access zone operation; this method is the invocation entry point on {@code GatewayHttpListener} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpListener.accessZone(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 accessZone 的处理结果；returns the result of the operation.
     */
    public AccessZone accessZone() {
        return accessZone;
    }

    /**
     * 中文说明：执行 close 操作；该方法是 {@code GatewayHttpListener} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the close operation; this method is the invocation entry point on {@code GatewayHttpListener} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpListener.close(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Override
    public synchronized void close() {
        if (server != null) {
            server.disposeNow();
            server = null;
        }
    }

    /**
     * 中文说明：执行 远程Address 操作；该方法是 {@code GatewayHttpListener} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the remote address operation; this method is the invocation entry point on {@code GatewayHttpListener} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpListener.remoteAddress(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param address 参数 address；parameter address。
     * @return 返回 远程Address 的处理结果；returns the result of the operation.
     */
    private InetSocketAddress remoteAddress(java.net.SocketAddress address) {
        return address instanceof InetSocketAddress inet
                ? inet
                : null;
    }

    /**
     * 中文说明：执行 writeHttp 操作；该方法是 {@code GatewayHttpListener} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the write http operation; this method is the invocation entry point on {@code GatewayHttpListener} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpListener.writeHttp(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param response 参数 响应；parameter response。
     * @param outbound 参数 outbound；parameter outbound。
     * @return 返回 writeHttp 的处理结果；returns the result of the operation.
     */
    private Mono<Void> writeHttp(
            HttpServerResponse response,
            GatewayOutboundHttpResponse outbound) {
        return Mono.defer(() -> {
            response.status(outbound.status());
            outbound.headers().forEach((name, values) ->
                    values.forEach(value -> response.header(name, value))
            );
            if (outbound.flushMode() == GatewayHttpFlushMode.PER_BUFFER) {
                Publisher<? extends Publisher<? extends io.netty.buffer.ByteBuf>>
                        groups = outbound.body().map(buffer -> Mono.just(
                        GatewayDataBufferOwnership.transferToNetty(
                                buffer,
                                response.alloc()
                        )
                ));
                return response.sendGroups(groups).then();
            }
            return response.send(outbound.body().map(buffer ->
                    GatewayDataBufferOwnership.transferToNetty(
                            buffer,
                            response.alloc()
                    )
            )).then();
        }).doFinally(ignored -> outbound.abandon());
    }

    /**
     * 中文说明：执行 handleWebSocket 操作；该方法是 {@code GatewayHttpListener} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the handle web socket operation; this method is the invocation entry point on {@code GatewayHttpListener} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpListener.handleWebSocket(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param inbound 参数 inbound；parameter inbound。
     * @param response 参数 响应；parameter response。
     * @return 返回 handleWebSocket 的处理结果；returns the result of the operation.
     */
    private Mono<Void> handleWebSocket(
            GatewayInboundHttpRequest inbound,
            HttpServerResponse response) {
        return handler.prepareWebSocket(accessZone, inbound)
                .flatMap(result -> {
                    if (result instanceof GatewayWebSocketHandshakeResult
                            .Rejected rejected) {
                        return writeHttp(
                                response,
                                webSocketRejection(rejected)
                        );
                    }
                    GatewayPreparedWebSocketSession session =
                            ((GatewayWebSocketHandshakeResult.Accepted) result)
                                    .session();
                    WebsocketServerSpec.Builder specification =
                            WebsocketServerSpec.builder()
                                    .handlePing(true)
                                    .compress(false)
                                    .maxFramePayloadLength(Math.toIntExact(
                                            session.context()
                                                    .policy()
                                                    .websocketMaxFrameBytes()
                                                    .orElseThrow()
                                    ));
                    if (session.selectedSubprotocol() != null) {
                        specification.protocols(
                                session.selectedSubprotocol()
                        );
                    }
                    return response.sendWebsocket((input, output) -> {
                        AtomicReference<Connection> connection =
                                new AtomicReference<>();
                        input.withConnection(connection::set);
                        return handler.bridgeWebSocket(
                                session,
                                new ReactorNettyWebSocketPeer(
                                        Objects.requireNonNull(
                                                connection.get(),
                                                "websocket connection"
                                        ),
                                        input,
                                        output
                                )
                        );
                    }, specification.build()).doFinally(ignored ->
                            session.dispose()
                    );
                });
    }

    /**
     * 中文说明：执行 webSocketUpgrade 操作；该方法是 {@code GatewayHttpListener} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the web socket upgrade operation; this method is the invocation entry point on {@code GatewayHttpListener} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpListener.webSocketUpgrade(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 webSocketUpgrade 的处理结果；returns the result of the operation.
     */
    static boolean webSocketUpgrade(GatewayInboundHttpRequest request) {
        if (!"GET".equalsIgnoreCase(request.method())) {
            return false;
        }
        boolean upgrade = headerValues(request.headers(), "upgrade")
                .stream()
                .anyMatch(value -> "websocket".equalsIgnoreCase(
                        value.trim()
                ));
        boolean connection = headerValues(request.headers(), "connection")
                .stream()
                .flatMap(value -> java.util.Arrays.stream(value.split(",")))
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch("upgrade"::equals);
        return upgrade && connection;
    }

    /**
     * 中文说明：执行 webSocketRejection 操作；该方法是 {@code GatewayHttpListener} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the web socket rejection operation; this method is the invocation entry point on {@code GatewayHttpListener} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpListener.webSocketRejection(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param rejected 参数 rejected；parameter rejected。
     * @return 返回 webSocketRejection 的处理结果；returns the result of the operation.
     */
    private GatewayOutboundHttpResponse webSocketRejection(
            GatewayWebSocketHandshakeResult.Rejected rejected) {
        String body = "{\"success\":false,\"code\":\""
                + rejected.errorCode()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                + "\"}";
        return new GatewayOutboundHttpResponse(
                rejected.httpStatus(),
                Map.of("content-type", List.of(
                        "application/json; charset=UTF-8"
                )),
                reactor.core.publisher.Flux.just(BUFFER_FACTORY.wrap(
                        body.getBytes(
                                java.nio.charset.StandardCharsets.UTF_8
                        )
                ))
        );
    }

    /**
     * 中文说明：执行 headerValues 操作；该方法是 {@code GatewayHttpListener} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the header values operation; this method is the invocation entry point on {@code GatewayHttpListener} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpListener.headerValues(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param headers 参数 headers；parameter headers。
     * @param name 参数 name；parameter name。
     * @return 返回 headerValues 的处理结果；returns the result of the operation.
     */
    private static List<String> headerValues(
            Map<String, List<String>> headers,
            String name) {
        return headers.entrySet().stream()
                .filter(entry -> name.equalsIgnoreCase(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream())
                .toList();
    }
}
