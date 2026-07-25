package top.egon.cola.component.gateway.engine.http;

import io.netty.buffer.ByteBufUtil;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GatewayHttpListener implements AutoCloseable {

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
        server = HttpServer.create()
                .host(properties.host())
                .port(properties.port())
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
                                            .map(ByteBufUtil::getBytes)
                            );
                    return handler.handle(accessZone, inbound)
                            .flatMap(outbound -> {
                                response.status(outbound.status());
                                outbound.headers().forEach((name, values) ->
                                        values.forEach(value ->
                                                response.header(name, value)
                                        )
                                );
                                return response.sendByteArray(outbound.body())
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
