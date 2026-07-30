package top.egon.cola.component.gateway.engine.websocket;

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
import top.egon.cola.component.gateway.engine.transport.GatewayTransportTimeoutException;
import top.egon.cola.component.gateway.engine.transport.GatewayTransportTimeouts;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Reactor Netty client adapter that completes the upstream handshake first.
 */
public final class ReactorNettyWebSocketUpstreamAdapter
        implements WebSocketUpstreamAdapter {

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

    private static final AttributeKey<Boolean> SUBPROTOCOL_OMITTED =
            AttributeKey.valueOf("gateway.ws.subprotocol.omitted");

    private final HttpClient client;

    private final SslContext secureSslContext;

    public ReactorNettyWebSocketUpstreamAdapter(HttpClient client) {
        this(client, null);
    }

    public ReactorNettyWebSocketUpstreamAdapter(
            HttpClient client,
            SslContext secureSslContext) {
        this.client = Objects.requireNonNull(client, "client");
        this.secureSslContext = secureSslContext;
    }

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
     */
    private static final class OptionalSubprotocolHandler
            extends ChannelInboundHandlerAdapter {

        private final String compatibilityProtocol;

        private OptionalSubprotocolHandler(String compatibilityProtocol) {
            this.compatibilityProtocol = compatibilityProtocol;
        }

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

    private static final class ReactorNettyPeer
            implements GatewayWebSocketPeer {

        private final Connection connection;

        private final WebsocketInbound inbound;

        private final WebsocketOutbound outbound;

        private final NettyDataBufferFactory buffers;

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

        @Override
        public Flux<GatewayWebSocketFrame> receive() {
            return inbound.receiveFrames().map(this::fromNetty);
        }

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

        @Override
        public Mono<Void> sendClose(GatewayWebSocketCloseStatus status) {
            Objects.requireNonNull(status, "status");
            if (!status.sendable()) {
                return Mono.fromRunnable(this::dispose);
            }
            return outbound.sendClose(status.code(), status.reason());
        }

        @Override
        public void dispose() {
            connection.dispose();
        }

        @Override
        public boolean disposed() {
            return connection.isDisposed();
        }

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
