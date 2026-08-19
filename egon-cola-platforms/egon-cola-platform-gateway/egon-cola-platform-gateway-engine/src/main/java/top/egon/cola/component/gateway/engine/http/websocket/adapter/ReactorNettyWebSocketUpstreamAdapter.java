package top.egon.cola.component.gateway.engine.http.websocket.adapter;

import top.egon.cola.component.gateway.engine.common.transport.service.GatewayTransportTimeoutException;

import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayPreparedWebSocketSession;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketCloseStatus;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketFrame;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketFrameType;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketHandshakeResult;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketProxyContext;
import top.egon.cola.component.gateway.engine.http.websocket.service.GatewayWebSocketPeer;
import top.egon.cola.component.gateway.engine.http.websocket.service.WebSocketUpstreamAdapter;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakeException;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.ssl.SslContext;
import io.netty.util.AttributeKey;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.WebsocketClientSpec;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.engine.common.transport.service.GatewayTransportTimeouts;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Reactor Netty client adapter that completes the upstream handshake first.
 * 补充说明 / Supplementary summary: {@code ReactorNettyWebSocketUpstreamAdapter} 是适配器，位于当前 Gateway 模块的相关包中，负责ReactorNettyWebSocketUpstreamAdapter相关的职责与边界。
 * English supplement: {@code ReactorNettyWebSocketUpstreamAdapter} is a reactor netty web socket upstream adapter adapter in the current Gateway module; it owns the reactor netty web socket upstream adapter-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class ReactorNettyWebSocketUpstreamAdapter
        implements WebSocketUpstreamAdapter {

    /**
     * 中文说明：表示 HANDSHAKEHEADERS 这一固定值；它属于 {@code ReactorNettyWebSocketUpstreamAdapter} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value handshake headers; it is a state, type, or protocol value of {@code ReactorNettyWebSocketUpstreamAdapter} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code ReactorNettyWebSocketUpstreamAdapter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReactorNettyWebSocketUpstreamAdapter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Set<String> HANDSHAKE_HEADERS = Set.of(
            "host",
            "connection",
            "upgrade",
            "keep-alive",
            "proxy-connection",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailer",
            "content-length",
            "transfer-encoding",
            "sec-websocket-key",
            "sec-websocket-version",
            "sec-websocket-protocol",
            "sec-websocket-extensions"
    );

    /**
     * 中文说明：表示 SUBPROTOCOLOMITTED 这一固定值；它属于 {@code ReactorNettyWebSocketUpstreamAdapter} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value subprotocol omitted; it is a state, type, or protocol value of {@code ReactorNettyWebSocketUpstreamAdapter} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code ReactorNettyWebSocketUpstreamAdapter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReactorNettyWebSocketUpstreamAdapter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final AttributeKey<Boolean> SUBPROTOCOL_OMITTED =
            AttributeKey.valueOf("gateway.ws.subprotocol.omitted");

    /**
     * 中文说明：保存 客户端 对应的状态、依赖或配置值；字段类型为 {@code HttpClient}，由 {@code ReactorNettyWebSocketUpstreamAdapter} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by client; its type is {@code HttpClient}, and {@code ReactorNettyWebSocketUpstreamAdapter} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ReactorNettyWebSocketUpstreamAdapter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReactorNettyWebSocketUpstreamAdapter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final HttpClient client;

    /**
     * 中文说明：保存 secureSslContext 对应的状态、依赖或配置值；字段类型为 {@code SslContext}，由 {@code ReactorNettyWebSocketUpstreamAdapter} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by secure ssl context; its type is {@code SslContext}, and {@code ReactorNettyWebSocketUpstreamAdapter} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ReactorNettyWebSocketUpstreamAdapter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReactorNettyWebSocketUpstreamAdapter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final SslContext secureSslContext;

    /**
     * 中文说明：创建 {@code ReactorNettyWebSocketUpstreamAdapter} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code ReactorNettyWebSocketUpstreamAdapter} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param client 参数 客户端；parameter client。
     */
    public ReactorNettyWebSocketUpstreamAdapter(HttpClient client) {
        this(client, null);
    }

    /**
     * 中文说明：创建 {@code ReactorNettyWebSocketUpstreamAdapter} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code ReactorNettyWebSocketUpstreamAdapter} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param client 参数 客户端；parameter client。
     * @param secureSslContext 参数 secureSslContext；parameter secure ssl context。
     */
    public ReactorNettyWebSocketUpstreamAdapter(
            HttpClient client,
            SslContext secureSslContext) {
        this.client = Objects.requireNonNull(client, "client");
        this.secureSslContext = secureSslContext;
    }

    /**
     * 中文说明：执行 prepare 操作；该方法是 {@code ReactorNettyWebSocketUpstreamAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the prepare operation; this method is the invocation entry point on {@code ReactorNettyWebSocketUpstreamAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyWebSocketUpstreamAdapter.prepare(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param context 参数 context；parameter context。
     * @return 返回 prepare 的处理结果；returns the result of the operation.
     */
    @Override
    public Mono<GatewayWebSocketHandshakeResult> prepare(
            GatewayWebSocketProxyContext context) {
        Objects.requireNonNull(context, "context");
        return Mono.defer(() -> {
            HttpClient configured = client.option(
                            ChannelOption.CONNECT_TIMEOUT_MILLIS,
                            timeoutMillis(context.policy().connectTimeout())
                    )
                    .headers(headers -> copyHeaders(context, headers));
            if (context.provider().secure()
                    && secureSslContext != null) {
                configured = configured.secure(spec ->
                        spec.sslContext(secureSslContext)
                );
            }
            if (!context.subprotocolCandidates().isEmpty()) {
                String compatibilityProtocol = context
                        .subprotocolCandidates()
                        .getFirst();
                configured = configured.doOnConnected(connection ->
                        connection.addHandlerLast(
                                "gatewayWsOptionalSubprotocol",
                                new OptionalSubprotocolHandler(
                                        compatibilityProtocol
                                )
                        )
                );
            }
            WebsocketClientSpec websocketSpec = websocketSpec(context);
            Mono<? extends Connection> connection = configured
                    .websocket(websocketSpec)
                    .uri(upstreamUri(context))
                    .connect();
            return GatewayTransportTimeouts.responseHeaders(
                            connection,
                            context.policy().responseHeaderTimeout()
                    )
                    .map(value -> accepted(context, value))
                    .onErrorResume(failure -> Mono.just(rejected(failure)));
        });
    }

    /**
     * 中文说明：执行 accepted 操作；该方法是 {@code ReactorNettyWebSocketUpstreamAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the accepted operation; this method is the invocation entry point on {@code ReactorNettyWebSocketUpstreamAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyWebSocketUpstreamAdapter.accepted(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param context 参数 context；parameter context。
     * @param connection 参数 connection；parameter connection。
     * @return 返回 accepted 的处理结果；returns the result of the operation.
     */
    private GatewayWebSocketHandshakeResult accepted(
            GatewayWebSocketProxyContext context,
            Connection connection) {
        if (!(connection instanceof WebsocketInbound inbound)
                || !(connection instanceof WebsocketOutbound outbound)) {
            connection.dispose();
            return GatewayWebSocketHandshakeResult.rejected(
                    502,
                    "GATEWAY_WEBSOCKET_INVALID_UPSTREAM_SESSION",
                    "upstream did not expose a WebSocket session"
            );
        }
        ReactorNettyPeer peer = new ReactorNettyPeer(
                connection,
                inbound,
                outbound
        );
        boolean omitted = Boolean.TRUE.equals(
                connection.channel()
                        .attr(SUBPROTOCOL_OMITTED)
                        .getAndSet(null)
        );
        String selected = omitted ? null : inbound.selectedSubprotocol();
        if (!context.acceptsSubprotocol(selected)) {
            peer.dispose();
            return GatewayWebSocketHandshakeResult.rejected(
                    502,
                    "GATEWAY_WEBSOCKET_SUBPROTOCOL_MISMATCH",
                    "upstream selected an unoffered subprotocol"
            );
        }
        return GatewayWebSocketHandshakeResult.accepted(
                new GatewayPreparedWebSocketSession(
                        context,
                        peer,
                        selected
                )
        );
    }

    /**
     * 中文说明：执行 rejected 操作；该方法是 {@code ReactorNettyWebSocketUpstreamAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the rejected operation; this method is the invocation entry point on {@code ReactorNettyWebSocketUpstreamAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyWebSocketUpstreamAdapter.rejected(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param failure 参数 failure；parameter failure。
     * @return 返回 rejected 的处理结果；returns the result of the operation.
     */
    private GatewayWebSocketHandshakeResult rejected(Throwable failure) {
        int status = 502;
        if (failure instanceof GatewayTransportTimeoutException) {
            status = 504;
        } else if (failure instanceof WebSocketClientHandshakeException
                && ((WebSocketClientHandshakeException) failure)
                .response() != null) {
            int upstreamStatus = ((WebSocketClientHandshakeException) failure)
                    .response()
                    .status()
                    .code();
            if (upstreamStatus >= 400 && upstreamStatus <= 599) {
                status = upstreamStatus;
            }
        }
        return GatewayWebSocketHandshakeResult.rejected(
                status,
                "GATEWAY_WEBSOCKET_UPSTREAM_HANDSHAKE_FAILED",
                failure.getClass().getSimpleName()
        );
    }

    /**
     * 中文说明：执行 WebSocketSpec 操作；该方法是 {@code ReactorNettyWebSocketUpstreamAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the websocket spec operation; this method is the invocation entry point on {@code ReactorNettyWebSocketUpstreamAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyWebSocketUpstreamAdapter.websocketSpec(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param context 参数 context；parameter context。
     * @return 返回 WebSocketSpec 的处理结果；returns the result of the operation.
     */
    private WebsocketClientSpec websocketSpec(
            GatewayWebSocketProxyContext context) {
        WebsocketClientSpec.Builder builder = WebsocketClientSpec.builder()
                .handlePing(true)
                .compress(false)
                .maxFramePayloadLength(Math.toIntExact(
                        context.policy()
                                .websocketMaxFrameBytes()
                                .orElseThrow()
                ));
        if (!context.subprotocolCandidates().isEmpty()) {
            builder.protocols(String.join(",", context.subprotocolCandidates()));
        }
        return builder.build();
    }

    /**
     * 中文说明：执行 copyHeaders 操作；该方法是 {@code ReactorNettyWebSocketUpstreamAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the copy headers operation; this method is the invocation entry point on {@code ReactorNettyWebSocketUpstreamAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyWebSocketUpstreamAdapter.copyHeaders(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param context 参数 context；parameter context。
     * @param target 参数 target；parameter target。
     */
    private void copyHeaders(
            GatewayWebSocketProxyContext context,
            HttpHeaders target) {
        Set<String> connectionTokens = context.headers().entrySet().stream()
                .filter(entry -> "connection".equalsIgnoreCase(
                        entry.getKey()
                ))
                .flatMap(entry -> entry.getValue().stream())
                .flatMap(value -> java.util.Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(token -> token.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        context.headers().forEach((name, values) -> {
            String normalized = name.toLowerCase(Locale.ROOT);
            if (!HANDSHAKE_HEADERS.contains(normalized)
                    && !connectionTokens.contains(normalized)) {
                values.forEach(value -> target.add(name, value));
            }
        });
        target.remove(HttpHeaderNames.SEC_WEBSOCKET_EXTENSIONS);
    }

    /**
     * 中文说明：执行 upstreamUri 操作；该方法是 {@code ReactorNettyWebSocketUpstreamAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the upstream uri operation; this method is the invocation entry point on {@code ReactorNettyWebSocketUpstreamAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyWebSocketUpstreamAdapter.upstreamUri(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param context 参数 context；parameter context。
     * @return 返回 upstreamUri 的处理结果；returns the result of the operation.
     */
    private URI upstreamUri(GatewayWebSocketProxyContext context) {
        ProviderInstance provider = context.provider();
        String host = provider.host().indexOf(':') >= 0
                ? "[" + provider.host() + "]"
                : provider.host();
        String scheme = provider.secure() ? "wss" : "ws";
        return URI.create(
                scheme
                        + "://"
                        + host
                        + ":"
                        + provider.port()
                        + context.pathAndQuery()
        );
    }

    /**
     * 中文说明：执行 超时Millis 操作；该方法是 {@code ReactorNettyWebSocketUpstreamAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the timeout millis operation; this method is the invocation entry point on {@code ReactorNettyWebSocketUpstreamAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyWebSocketUpstreamAdapter.timeoutMillis(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param timeout 参数 超时；parameter timeout。
     * @return 返回 超时Millis 的处理结果；returns the result of the operation.
     */
    private int timeoutMillis(Duration timeout) {
        return Math.toIntExact(Math.min(
                Integer.MAX_VALUE,
                Math.max(1, timeout.toMillis())
        ));
    }

    /**
     * Netty requires a selected subprotocol whenever candidates were offered,
     * although RFC 6455 permits the server to select none. This handler only
     * satisfies that internal check and records the original wire fact.
     * 补充说明 / Supplementary summary: {@code OptionalSubprotocolHandler} 是处理器，位于当前 Gateway 模块的相关包中，负责OptionalSubprotocol处理器相关的职责与边界。
     * English supplement: {@code OptionalSubprotocolHandler} is a optional subprotocol handler handler in the current Gateway module; it owns the optional subprotocol handler-related responsibility and boundary.
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private static final class OptionalSubprotocolHandler
            extends ChannelInboundHandlerAdapter {

        /**
         * 中文说明：保存 compatibilityProtocol 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code ReactorNettyWebSocketUpstreamAdapter.OptionalSubprotocolHandler} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by compatibility protocol; its type is {@code String}, and {@code ReactorNettyWebSocketUpstreamAdapter.OptionalSubprotocolHandler} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ReactorNettyWebSocketUpstreamAdapter.OptionalSubprotocolHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReactorNettyWebSocketUpstreamAdapter.OptionalSubprotocolHandler}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final String compatibilityProtocol;

        /**
         * 中文说明：创建 {@code ReactorNettyWebSocketUpstreamAdapter.OptionalSubprotocolHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code ReactorNettyWebSocketUpstreamAdapter.OptionalSubprotocolHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param compatibilityProtocol 参数 compatibilityProtocol；parameter compatibility protocol。
         */
        private OptionalSubprotocolHandler(String compatibilityProtocol) {
            this.compatibilityProtocol = compatibilityProtocol;
        }

        /**
         * 中文说明：执行 通道Read 操作；该方法是 {@code ReactorNettyWebSocketUpstreamAdapter.OptionalSubprotocolHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the channel read operation; this method is the invocation entry point on {@code ReactorNettyWebSocketUpstreamAdapter.OptionalSubprotocolHandler} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyWebSocketUpstreamAdapter.OptionalSubprotocolHandler.channelRead(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param context 参数 context；parameter context。
         * @param message 参数 消息；parameter message。
         */
        @Override
        public void channelRead(
                ChannelHandlerContext context,
                Object message) throws Exception {
            if (message instanceof HttpResponse response
                    && response.status().equals(
                    HttpResponseStatus.SWITCHING_PROTOCOLS
            ) && !response.headers().contains(
                    HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL
            )) {
                response.headers().set(
                        HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL,
                        compatibilityProtocol
                );
                context.channel()
                        .attr(SUBPROTOCOL_OMITTED)
                        .set(Boolean.TRUE);
            }
            super.channelRead(context, message);
        }
    }

    /**
     * 中文说明：{@code ReactorNettyPeer} 是类型，位于当前 Gateway 模块的相关包中，负责ReactorNettyPeer相关的职责与边界。
     * English summary: {@code ReactorNettyPeer} is a type in the current Gateway module; it owns the reactor netty peer-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private static final class ReactorNettyPeer
            implements GatewayWebSocketPeer {

        /**
         * 中文说明：保存 connection 对应的状态、依赖或配置值；字段类型为 {@code Connection}，由 {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by connection; its type is {@code Connection}, and {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final Connection connection;

        /**
         * 中文说明：保存 inbound 对应的状态、依赖或配置值；字段类型为 {@code WebsocketInbound}，由 {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by inbound; its type is {@code WebsocketInbound}, and {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final WebsocketInbound inbound;

        /**
         * 中文说明：保存 outbound 对应的状态、依赖或配置值；字段类型为 {@code WebsocketOutbound}，由 {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by outbound; its type is {@code WebsocketOutbound}, and {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final WebsocketOutbound outbound;

        /**
         * 中文说明：保存 buffers 对应的状态、依赖或配置值；字段类型为 {@code NettyDataBufferFactory}，由 {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by buffers; its type is {@code NettyDataBufferFactory}, and {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final NettyDataBufferFactory buffers;

        /**
         * 中文说明：创建 {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param connection 参数 connection；parameter connection。
         * @param inbound 参数 inbound；parameter inbound。
         * @param outbound 参数 outbound；parameter outbound。
         */
        private ReactorNettyPeer(
                Connection connection,
                WebsocketInbound inbound,
                WebsocketOutbound outbound) {
            this.connection = connection;
            this.inbound = inbound;
            this.outbound = outbound;
            buffers = new NettyDataBufferFactory(
                    connection.channel().alloc()
            );
        }

        /**
         * 中文说明：执行 receive 操作；该方法是 {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the receive operation; this method is the invocation entry point on {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer.receive(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 receive 的处理结果；returns the result of the operation.
         */
        @Override
        public Flux<GatewayWebSocketFrame> receive() {
            return inbound.receiveFrames().map(this::fromNetty);
        }

        /**
         * 中文说明：执行 send 操作；该方法是 {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the send operation; this method is the invocation entry point on {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer.send(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param frames 参数 frames；parameter frames。
         * @return 返回 send 的处理结果；returns the result of the operation.
         */
        @Override
        public Mono<Void> send(Flux<GatewayWebSocketFrame> frames) {
            return outbound.sendObject(frames
                            .map(this::toNetty)
                            .doOnDiscard(
                                    GatewayWebSocketFrame.class,
                                    GatewayWebSocketFrame::release
                            ))
                    .then();
        }

        /**
         * 中文说明：执行 sendClose 操作；该方法是 {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the send close operation; this method is the invocation entry point on {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer.sendClose(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param status 参数 status；parameter status。
         * @return 返回 sendClose 的处理结果；returns the result of the operation.
         */
        @Override
        public Mono<Void> sendClose(GatewayWebSocketCloseStatus status) {
            Objects.requireNonNull(status, "status");
            if (!status.sendable()) {
                return Mono.fromRunnable(this::dispose);
            }
            return outbound.sendClose(status.code(), status.reason());
        }

        /**
         * 中文说明：执行 dispose 操作；该方法是 {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the dispose operation; this method is the invocation entry point on {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer.dispose(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         */
        @Override
        public void dispose() {
            connection.dispose();
        }

        /**
         * 中文说明：执行 disposed 操作；该方法是 {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the disposed operation; this method is the invocation entry point on {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer.disposed(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 disposed 的处理结果；returns the result of the operation.
         */
        @Override
        public boolean disposed() {
            return connection.isDisposed();
        }

        /**
         * 中文说明：执行 fromNetty 操作；该方法是 {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the from netty operation; this method is the invocation entry point on {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer.fromNetty(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param frame 参数 frame；parameter frame。
         * @return 返回 fromNetty 的处理结果；returns the result of the operation.
         */
        private GatewayWebSocketFrame fromNetty(WebSocketFrame frame) {
            if (frame instanceof CloseWebSocketFrame close) {
                GatewayWebSocketCloseStatus status = close.statusCode() < 0
                        ? null
                        : new GatewayWebSocketCloseStatus(
                                close.statusCode(),
                                close.reasonText()
                        );
                return new GatewayWebSocketFrame(
                        GatewayWebSocketFrameType.CLOSE,
                        true,
                        buffers.wrap(connection.channel().alloc().buffer(0)),
                        status
                );
            }
            ByteBuf content = frame.content().retain();
            return GatewayWebSocketFrame.data(
                    frameType(frame),
                    frame.isFinalFragment(),
                    buffers.wrap(content)
            );
        }

        /**
         * 中文说明：执行 toNetty 操作；该方法是 {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the to netty operation; this method is the invocation entry point on {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer.toNetty(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param frame 参数 frame；parameter frame。
         * @return 返回 toNetty 的处理结果；returns the result of the operation.
         */
        private WebSocketFrame toNetty(GatewayWebSocketFrame frame) {
            if (frame.type() == GatewayWebSocketFrameType.CLOSE) {
                GatewayWebSocketCloseStatus status = frame.closeStatus();
                frame.release();
                return status == null
                        ? new CloseWebSocketFrame()
                        : new CloseWebSocketFrame(
                                status.code(),
                                status.reason()
                        );
            }
            ByteBuf payload = transfer(frame);
            return switch (frame.type()) {
                case TEXT -> new TextWebSocketFrame(
                        frame.finalFragment(),
                        0,
                        payload
                );
                case BINARY -> new BinaryWebSocketFrame(
                        frame.finalFragment(),
                        0,
                        payload
                );
                case CONTINUATION -> new ContinuationWebSocketFrame(
                        frame.finalFragment(),
                        0,
                        payload
                );
                case PING -> new PingWebSocketFrame(payload);
                case PONG -> new PongWebSocketFrame(payload);
                case CLOSE -> throw new IllegalStateException(
                        "close frame handled separately"
                );
            };
        }

        /**
         * 中文说明：执行 transfer 操作；该方法是 {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the transfer operation; this method is the invocation entry point on {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer.transfer(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param frame 参数 frame；parameter frame。
         * @return 返回 transfer 的处理结果；returns the result of the operation.
         */
        private ByteBuf transfer(GatewayWebSocketFrame frame) {
            DataBuffer payload = frame.payload();
            if (payload instanceof NettyDataBuffer netty) {
                ByteBuf nativeBuffer = netty.getNativeBuffer().retain();
                frame.release();
                return nativeBuffer;
            }
            ByteBuf copy = connection.channel().alloc().buffer(
                    payload.readableByteCount()
            );
            try (DataBuffer.ByteBufferIterator byteBuffers =
                         payload.readableByteBuffers()) {
                byteBuffers.forEachRemaining(copy::writeBytes);
            } finally {
                frame.release();
            }
            return copy;
        }

        /**
         * 中文说明：执行 frameType 操作；该方法是 {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the frame type operation; this method is the invocation entry point on {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyWebSocketUpstreamAdapter.ReactorNettyPeer.frameType(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param frame 参数 frame；parameter frame。
         * @return 返回 frameType 的处理结果；returns the result of the operation.
         */
        private GatewayWebSocketFrameType frameType(WebSocketFrame frame) {
            if (frame instanceof TextWebSocketFrame) {
                return GatewayWebSocketFrameType.TEXT;
            }
            if (frame instanceof BinaryWebSocketFrame) {
                return GatewayWebSocketFrameType.BINARY;
            }
            if (frame instanceof ContinuationWebSocketFrame) {
                return GatewayWebSocketFrameType.CONTINUATION;
            }
            if (frame instanceof PingWebSocketFrame) {
                return GatewayWebSocketFrameType.PING;
            }
            if (frame instanceof PongWebSocketFrame) {
                return GatewayWebSocketFrameType.PONG;
            }
            throw new IllegalArgumentException(
                    "unsupported WebSocket frame: "
                            + frame.getClass().getSimpleName()
            );
        }
    }
}
