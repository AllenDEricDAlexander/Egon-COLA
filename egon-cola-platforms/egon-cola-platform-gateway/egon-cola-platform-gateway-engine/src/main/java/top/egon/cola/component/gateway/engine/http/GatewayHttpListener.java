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

public final class GatewayHttpListener implements AutoCloseable {

    private static final NettyDataBufferFactory BUFFER_FACTORY =
            new NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT);

    private final AccessZone accessZone;

    private final GatewayHttpEngineProperties.Listener properties;

    private final GatewayHttpDataPlaneHandler handler;

    private DisposableServer server;

    public GatewayHttpListener(
            AccessZone accessZone,
            GatewayHttpEngineProperties.Listener properties,
            GatewayHttpDataPlaneHandler handler) {
        this.accessZone = Objects.requireNonNull(accessZone, "accessZone");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.handler = Objects.requireNonNull(handler, "handler");
    }

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

    public synchronized int port() {
        return server == null ? -1 : server.port();
    }

    public synchronized boolean running() {
        return server != null && !server.isDisposed();
    }

    public AccessZone accessZone() {
        return accessZone;
    }

    @Override
    public synchronized void close() {
        if (server != null) {
            server.disposeNow();
            server = null;
        }
    }

    private InetSocketAddress remoteAddress(java.net.SocketAddress address) {
        return address instanceof InetSocketAddress inet
                ? inet
                : null;
    }

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

    private static List<String> headerValues(
            Map<String, List<String>> headers,
            String name) {
        return headers.entrySet().stream()
                .filter(entry -> name.equalsIgnoreCase(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream())
                .toList();
    }
}
