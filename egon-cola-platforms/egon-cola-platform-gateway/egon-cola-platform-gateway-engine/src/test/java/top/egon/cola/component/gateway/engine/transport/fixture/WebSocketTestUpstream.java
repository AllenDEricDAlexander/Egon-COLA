package top.egon.cola.component.gateway.engine.transport.fixture;

import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.WebsocketServerSpec;
import top.egon.cola.component.gateway.core.provider.ProviderHealthState;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderRegistryState;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * In-process WebSocket echo upstream with explicit protocol negotiation.
 */
public final class WebSocketTestUpstream implements AutoCloseable {

    private final AtomicInteger handshakes = new AtomicInteger();

    private final AtomicReference<String> authorization =
            new AtomicReference<>();

    private final AtomicReference<String> origin = new AtomicReference<>();

    private final AtomicReference<String> extensions = new AtomicReference<>();

    private final DisposableServer server;

    public WebSocketTestUpstream() {
        server = HttpServer.create()
                .host("127.0.0.1")
                .port(0)
                .handle((request, response) -> {
                    if (!request.uri().startsWith("/v1/realtime")) {
                        return response.status(404).send();
                    }
                    handshakes.incrementAndGet();
                    authorization.set(request.requestHeaders().get(
                            "Authorization"
                    ));
                    origin.set(request.requestHeaders().get("Origin"));
                    extensions.set(request.requestHeaders().get(
                            "Sec-WebSocket-Extensions"
                    ));
                    return response.sendWebsocket(
                            (inbound, outbound) -> outbound.sendObject(
                                    inbound.receiveFrames()
                                            .map(frame -> frame.retain())
                            ).then(),
                            WebsocketServerSpec.builder()
                                    .protocols("realtime")
                                    .handlePing(true)
                                    .build()
                    );
                })
                .bindNow();
    }

    public ProviderInstance provider() {
        return new ProviderInstance(
                new ProviderServiceKey(
                        "test",
                        "default",
                        ProviderProtocolType.HTTP,
                        "openai-realtime",
                        "default",
                        "v1",
                        "http"
                ),
                "realtime-provider-1",
                "realtime-lease-1",
                "127.0.0.1",
                server.port(),
                false,
                Map.of(),
                Instant.now().plusSeconds(60),
                ProviderRegistryState.REGISTERED,
                ProviderHealthState.HEALTHY,
                ProviderHealthState.HEALTHY
        );
    }

    public int handshakes() {
        return handshakes.get();
    }

    public String authorization() {
        return authorization.get();
    }

    public String origin() {
        return origin.get();
    }

    public String extensions() {
        return extensions.get();
    }

    @Override
    public void close() {
        server.disposeNow();
    }
}
