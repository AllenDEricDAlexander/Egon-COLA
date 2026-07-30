package top.egon.cola.component.gateway.engine.http;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.engine.http.buffer.GatewayDataBufferOwnership;
import top.egon.cola.component.gateway.engine.security.GatewayTransportSecurity;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
                    return handler.handle(accessZone, inbound)
                            .flatMap(outbound -> {
                                response.status(outbound.status());
                                outbound.headers().forEach((name, values) ->
                                        values.forEach(value ->
                                                response.header(name, value)
                                        )
                                );
                                return response.send(outbound.body().map(
                                                buffer ->
                                                        GatewayDataBufferOwnership
                                                                .transferToNetty(
                                                                        buffer,
                                                                        response.alloc()
                                                                )
                                        ))
                                        .then();
                            });
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
}
