package top.egon.cola.component.gateway.engine.http.service;

import top.egon.cola.component.gateway.engine.http.domain.GatewayHttpEngineProperties;
import top.egon.cola.component.gateway.engine.http.domain.GatewayInboundHttpRequest;
import top.egon.cola.component.gateway.engine.http.service.GatewayOutboundHttpResponse;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.Connection;
import reactor.netty.http.client.HttpClient;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.rule.GatewayRequestBodyMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteProfile;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportResponseMode;
import top.egon.cola.component.gateway.core.provider.ProviderHealthState;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderRegistryState;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.transport.EffectiveGatewayTransportPolicy;
import top.egon.cola.component.gateway.engine.common.transport.service.GatewayCommitGuard;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayPreparedWebSocketSession;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketCloseStatus;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketFrame;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketHandshakeResult;
import top.egon.cola.component.gateway.engine.http.websocket.service.GatewayWebSocketObserver;
import top.egon.cola.component.gateway.engine.http.websocket.service.GatewayWebSocketPeer;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketProxyContext;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayHttpServerWebSocketTest {

    @Test
    void waitsForUpstreamPreparationBeforeCommittingClientHandshake()
            throws Exception {
        Sinks.One<GatewayWebSocketHandshakeResult> prepared = Sinks.one();
        AtomicInteger bridges = new AtomicInteger();
        CountDownLatch bridgeStarted = new CountDownLatch(1);
        IdlePeer upstreamPeer = new IdlePeer();
        GatewayPreparedWebSocketSession session =
                new GatewayPreparedWebSocketSession(
                        context(),
                        upstreamPeer,
                        null
                );
        GatewayHttpDataPlaneHandler handler =
                new GatewayHttpDataPlaneHandler() {
                    @Override
                    public Mono<GatewayOutboundHttpResponse> handle(
                            AccessZone zone,
                            GatewayInboundHttpRequest request) {
                        return Mono.just(GatewayOutboundHttpResponse.text(
                                200,
                                "ordinary HTTP"
                        ));
                    }

                    @Override
                    public Mono<GatewayWebSocketHandshakeResult>
                    prepareWebSocket(
                            AccessZone zone,
                            GatewayInboundHttpRequest request) {
                        return prepared.asMono();
                    }

                    @Override
                    public Mono<Void> bridgeWebSocket(
                            GatewayPreparedWebSocketSession upstream,
                            GatewayWebSocketPeer downstream) {
                        bridges.incrementAndGet();
                        bridgeStarted.countDown();
                        return Mono.never();
                    }
                };
        GatewayHttpServer server = new GatewayHttpServer(
                properties(),
                handler
        );
        server.start();
        try {
            CompletableFuture<? extends Connection> client =
                    HttpClient.create()
                            .websocket()
                            .uri("ws://127.0.0.1:"
                                    + server.publicPort()
                                    + "/v1/realtime")
                            .connect()
                            .toFuture();

            Thread.sleep(100);
            assertFalse(client.isDone());

            prepared.tryEmitValue(
                    new GatewayWebSocketHandshakeResult.Accepted(session)
            );
            Connection connection = client.get(1, TimeUnit.SECONDS);
            assertTrue(bridgeStarted.await(1, TimeUnit.SECONDS));
            assertEquals(1, bridges.get());

            connection.disposeNow();
            server.close();
            assertTrue(upstreamPeer.disposed());
        } finally {
            server.close();
        }
    }

    private GatewayWebSocketProxyContext context() {
        return new GatewayWebSocketProxyContext(
                new ProviderInstance(
                        new ProviderServiceKey(
                                "test-biz",
                                "test-app",
                                "test",
                                "default",
                                ProviderProtocolType.HTTP,
                                "openai",
                                "default",
                                "v1",
                                "http"
                        ),
                        "provider-1",
                        "lease-1",
                        "127.0.0.1",
                        8080,
                        false,
                        Map.of(),
                        Instant.now().plusSeconds(30),
                        ProviderRegistryState.REGISTERED,
                        ProviderHealthState.HEALTHY,
                        ProviderHealthState.HEALTHY
                ),
                "/v1/realtime",
                Map.of(),
                List.of(),
                new EffectiveGatewayTransportPolicy(
                        GatewayRouteProfile.OPENAI_HTTP,
                        GatewayTransportProtocol.WEBSOCKET,
                        GatewayRequestBodyMode.STREAMING,
                        GatewayTransportResponseMode.AUTO_STREAM,
                        1024,
                        OptionalLong.empty(),
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(3),
                        Optional.empty(),
                        Optional.of(Duration.ofSeconds(5)),
                        OptionalLong.of(1024),
                        false,
                        false,
                        true
                ),
                GatewayCommitGuard.websocket(),
                GatewayWebSocketObserver.noop()
        );
    }

    private GatewayHttpEngineProperties properties() {
        return new GatewayHttpEngineProperties(
                new GatewayHttpEngineProperties.Listener(
                        true,
                        "127.0.0.1",
                        0
                ),
                new GatewayHttpEngineProperties.Listener(
                        false,
                        "127.0.0.1",
                        0
                ),
                64,
                8192,
                1024,
                Duration.ofSeconds(30),
                Duration.ofSeconds(5),
                10,
                10
        );
    }

    private static final class IdlePeer implements GatewayWebSocketPeer {

        private final AtomicBoolean disposed = new AtomicBoolean();

        @Override
        public Flux<GatewayWebSocketFrame> receive() {
            return Flux.never();
        }

        @Override
        public Mono<Void> send(Flux<GatewayWebSocketFrame> frames) {
            return frames.doOnNext(GatewayWebSocketFrame::release).then();
        }

        @Override
        public Mono<Void> sendClose(GatewayWebSocketCloseStatus status) {
            return Mono.empty();
        }

        @Override
        public void dispose() {
            disposed.set(true);
        }

        @Override
        public boolean disposed() {
            return disposed.get();
        }
    }
}
