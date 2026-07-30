package top.egon.cola.component.gateway.engine.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
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
import top.egon.cola.component.gateway.engine.transport.GatewayCommitGuard;
import top.egon.cola.component.gateway.engine.transport.GatewayCommitPoint;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayWebSocketProxyTest {

    @Test
    void rejectsAnUpstreamSubprotocolOutsideClientCandidatesBefore101() {
        FakePeer upstream = new FakePeer(Flux.never());
        GatewayWebSocketProxyContext context = context(
                1024,
                Duration.ofSeconds(5)
        );
        assertTrue(context.acceptsSubprotocol(null));
        WebSocketUpstreamAdapter adapter = ignored -> Mono.just(
                GatewayWebSocketHandshakeResult.accepted(
                        new GatewayPreparedWebSocketSession(
                                context,
                                upstream,
                                "attacker-protocol"
                        )
                )
        );

        GatewayWebSocketHandshakeResult result =
                new GatewayWebSocketProxy(adapter)
                        .prepare(context)
                        .block();

        GatewayWebSocketHandshakeResult.Rejected rejected =
                assertInstanceOf(
                        GatewayWebSocketHandshakeResult.Rejected.class,
                        result
                );
        assertEquals(502, rejected.httpStatus());
        assertTrue(upstream.disposed);
        assertEquals(GatewayCommitPoint.NEW, context.commitGuard().current());
    }

    @Test
    void bridgesTextBinaryFragmentsPingPongAndMirrorsCloseOnce() {
        List<GatewayWebSocketFrame> clientFrames = List.of(
                frame(GatewayWebSocketFrameType.TEXT, false, "part-1"),
                frame(
                        GatewayWebSocketFrameType.CONTINUATION,
                        true,
                        "part-2"
                ),
                binary(new byte[]{0, (byte) 0xff, 1}),
                frame(GatewayWebSocketFrameType.PING, true, "ping"),
                frame(GatewayWebSocketFrameType.PONG, true, "pong"),
                GatewayWebSocketFrame.close(
                        new GatewayWebSocketCloseStatus(1000, "done")
                )
        );
        FakePeer upstream = new FakePeer(Flux.never());
        FakePeer downstream = new FakePeer(Flux.fromIterable(clientFrames));
        GatewayWebSocketProxyContext context = context(
                1024,
                Duration.ofSeconds(5)
        );
        GatewayPreparedWebSocketSession session =
                new GatewayPreparedWebSocketSession(
                        context,
                        upstream,
                        "realtime"
                );

        new GatewayWebSocketProxy(ignored -> Mono.empty())
                .bridge(session, downstream)
                .block(Duration.ofSeconds(1));

        assertEquals(List.of(
                GatewayWebSocketFrameType.TEXT,
                GatewayWebSocketFrameType.CONTINUATION,
                GatewayWebSocketFrameType.BINARY,
                GatewayWebSocketFrameType.PING,
                GatewayWebSocketFrameType.PONG
        ), upstream.sent.stream().map(Snapshot::type).toList());
        assertEquals(
                List.of(0, 255, 1),
                upstream.sent.get(2).unsignedBytes()
        );
        assertEquals(
                List.of(new GatewayWebSocketCloseStatus(1000, "done")),
                upstream.closeStatuses
        );
        assertEquals(
                GatewayCommitPoint.TERMINATED,
                context.commitGuard().current()
        );
        assertTrue(upstream.disposed);
        assertTrue(downstream.disposed);
    }

    @Test
    void enabledBodyLogObserverReceivesOnlyWebSocketFrameMetadata() {
        List<String> frameMetadata = new CopyOnWriteArrayList<>();
        GatewayWebSocketObserver observer = new GatewayWebSocketObserver() {
            @Override
            public void observe(
                    String transportMode,
                    String commitPoint,
                    String terminationReason) {
            }

            @Override
            public void observeFrame(
                    String direction,
                    GatewayWebSocketFrameType frameType,
                    long payloadBytes,
                    boolean finalFragment) {
                frameMetadata.add(
                        direction + ":" + frameType + ":"
                                + payloadBytes + ":" + finalFragment
                );
            }
        };
        FakePeer upstream = new FakePeer(Flux.never());
        FakePeer downstream = new FakePeer(Flux.just(
                frame(GatewayWebSocketFrameType.TEXT, true, "secret"),
                GatewayWebSocketFrame.close(
                        new GatewayWebSocketCloseStatus(1000, "done")
                )
        ));
        GatewayWebSocketProxyContext context = context(
                1024,
                Duration.ofSeconds(5),
                true,
                observer
        );

        new GatewayWebSocketProxy(ignored -> Mono.empty())
                .bridge(
                        new GatewayPreparedWebSocketSession(
                                context,
                                upstream,
                                null
                        ),
                        downstream
                )
                .block(Duration.ofSeconds(1));

        assertEquals("REQUEST:TEXT:6:true", frameMetadata.getFirst());
        assertEquals(2, frameMetadata.size());
        assertFalse(frameMetadata.stream().anyMatch(
                value -> value.contains("secret")
        ));
    }

    @Test
    void closesTheViolatingPeerWith1009WithoutForwardingOversizedFrame() {
        FakePeer upstream = new FakePeer(Flux.never());
        FakePeer downstream = new FakePeer(Flux.just(binary(
                new byte[]{1, 2, 3, 4, 5}
        )));
        GatewayWebSocketProxyContext context = context(
                4,
                Duration.ofSeconds(5)
        );

        new GatewayWebSocketProxy(ignored -> Mono.empty())
                .bridge(
                        new GatewayPreparedWebSocketSession(
                                context,
                                upstream,
                                null
                        ),
                        downstream
                )
                .block(Duration.ofSeconds(1));

        assertTrue(upstream.sent.isEmpty());
        assertEquals(
                List.of(GatewayWebSocketCloseStatus.frameTooLarge()),
                downstream.closeStatuses
        );
    }

    @Test
    void websocketIdleClosesBothDirectionsAndCancelsReceivers() {
        FakePeer upstream = new FakePeer(Flux.never());
        FakePeer downstream = new FakePeer(Flux.never());
        GatewayWebSocketProxyContext context = context(
                1024,
                Duration.ofMillis(30)
        );

        new GatewayWebSocketProxy(ignored -> Mono.empty())
                .bridge(
                        new GatewayPreparedWebSocketSession(
                                context,
                                upstream,
                                null
                        ),
                        downstream
                )
                .block(Duration.ofSeconds(1));

        assertEquals(1, upstream.closeStatuses.size());
        assertEquals(1001, upstream.closeStatuses.getFirst().code());
        assertEquals(1, downstream.closeStatuses.size());
        assertEquals(1001, downstream.closeStatuses.getFirst().code());
        assertTrue(upstream.disposed);
        assertTrue(downstream.disposed);
    }

    @Test
    void activityInEitherDirectionResetsTheSharedIdleClock() {
        FakePeer upstream = new FakePeer(Flux.never());
        FakePeer downstream = new FakePeer(Flux.interval(
                        Duration.ofMillis(20)
                )
                .take(4)
                .map(ignored -> frame(
                        GatewayWebSocketFrameType.PING,
                        true,
                        "ping"
                ))
                .concatWith(Flux.never()));
        GatewayWebSocketProxyContext context = context(
                1024,
                Duration.ofMillis(50)
        );
        long started = System.nanoTime();

        new GatewayWebSocketProxy(ignored -> Mono.empty())
                .bridge(
                        new GatewayPreparedWebSocketSession(
                                context,
                                upstream,
                                null
                        ),
                        downstream
                )
                .block(Duration.ofSeconds(1));

        long elapsedMs = Duration.ofNanos(
                System.nanoTime() - started
        ).toMillis();
        assertTrue(elapsedMs >= 100, "elapsedMs=" + elapsedMs);
        assertEquals(4, upstream.sent.size());
        assertEquals(1001, downstream.closeStatuses.getFirst().code());
    }

    private GatewayWebSocketProxyContext context(
            long maxFrameBytes,
            Duration idleTimeout) {
        return context(
                maxFrameBytes,
                idleTimeout,
                false,
                GatewayWebSocketObserver.noop()
        );
    }

    private GatewayWebSocketProxyContext context(
            long maxFrameBytes,
            Duration idleTimeout,
            boolean bodyLogEnabled,
            GatewayWebSocketObserver observer) {
        return new GatewayWebSocketProxyContext(
                provider(8080, false),
                "/v1/realtime?model=gpt-realtime",
                Map.of(
                        "Authorization",
                        List.of("Bearer test"),
                        "Origin",
                        List.of("https://client.example")
                ),
                List.of("realtime", "fallback"),
                policy(maxFrameBytes, idleTimeout, bodyLogEnabled),
                GatewayCommitGuard.websocket(),
                observer
        );
    }

    private EffectiveGatewayTransportPolicy policy(
            long maxFrameBytes,
            Duration idleTimeout) {
        return policy(maxFrameBytes, idleTimeout, false);
    }

    private EffectiveGatewayTransportPolicy policy(
            long maxFrameBytes,
            Duration idleTimeout,
            boolean bodyLogEnabled) {
        return new EffectiveGatewayTransportPolicy(
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
                Optional.of(idleTimeout),
                OptionalLong.of(maxFrameBytes),
                bodyLogEnabled,
                false,
                true
        );
    }

    private ProviderInstance provider(
            int port,
            boolean secure) {
        return new ProviderInstance(
                new ProviderServiceKey(
                        "test",
                        "default",
                        ProviderProtocolType.HTTP,
                        "openai",
                        "default",
                        "v1",
                        secure ? "https" : "http"
                ),
                "provider-1",
                "lease-1",
                "127.0.0.1",
                port,
                secure,
                Map.of(),
                Instant.now().plusSeconds(60),
                ProviderRegistryState.REGISTERED,
                ProviderHealthState.HEALTHY,
                ProviderHealthState.HEALTHY
        );
    }

    private GatewayWebSocketFrame frame(
            GatewayWebSocketFrameType type,
            boolean finalFragment,
            String value) {
        return GatewayWebSocketFrame.data(
                type,
                finalFragment,
                DefaultDataBufferFactory.sharedInstance.wrap(
                        value.getBytes(StandardCharsets.UTF_8)
                )
        );
    }

    private GatewayWebSocketFrame binary(byte[] value) {
        return GatewayWebSocketFrame.data(
                GatewayWebSocketFrameType.BINARY,
                true,
                DefaultDataBufferFactory.sharedInstance.wrap(value)
        );
    }

    private static final class FakePeer implements GatewayWebSocketPeer {

        private final Flux<GatewayWebSocketFrame> inbound;

        private final List<Snapshot> sent = new CopyOnWriteArrayList<>();

        private final List<GatewayWebSocketCloseStatus> closeStatuses =
                new CopyOnWriteArrayList<>();

        private volatile boolean disposed;

        private FakePeer(Flux<GatewayWebSocketFrame> inbound) {
            this.inbound = inbound;
        }

        @Override
        public Flux<GatewayWebSocketFrame> receive() {
            return inbound;
        }

        @Override
        public Mono<Void> send(Flux<GatewayWebSocketFrame> frames) {
            return frames.doOnNext(frame -> {
                sent.add(new Snapshot(
                        frame.type(),
                        frame.finalFragment(),
                        frame.payloadBytes()
                ));
                frame.release();
            }).then();
        }

        @Override
        public Mono<Void> sendClose(GatewayWebSocketCloseStatus status) {
            return Mono.fromRunnable(() -> closeStatuses.add(status));
        }

        @Override
        public void dispose() {
            disposed = true;
        }

        @Override
        public boolean disposed() {
            return disposed;
        }
    }

    private record Snapshot(
            GatewayWebSocketFrameType type,
            boolean finalFragment,
            byte[] bytes) {

        private List<Integer> unsignedBytes() {
            List<Integer> values = new ArrayList<>();
            for (byte value : bytes) {
                values.add(Byte.toUnsignedInt(value));
            }
            return List.copyOf(values);
        }
    }
}
