package top.egon.cola.component.gateway.engine.http.proxy;

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
import top.egon.cola.component.gateway.engine.http.GatewayDataBufferTestSupport;
import top.egon.cola.component.gateway.engine.http.GatewayHttpFlushMode;
import top.egon.cola.component.gateway.engine.http.GatewayOutboundHttpResponse;
import top.egon.cola.component.gateway.engine.http.HttpUpstreamAdapter;
import top.egon.cola.component.gateway.engine.http.HttpUpstreamRequest;
import top.egon.cola.component.gateway.engine.http.logging.GatewayBodyLogDirection;
import top.egon.cola.component.gateway.engine.http.logging.GatewayBodyLogEvent;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayHttpProxyStrategyTest {

    @Test
    void aggregatedBodyIsReadOnceAndMarkedReplayable() {
        AtomicInteger subscriptions = new AtomicInteger();
        AtomicInteger invocations = new AtomicInteger();
        AtomicReference<HttpUpstreamRequest> captured = new AtomicReference<>();
        HttpUpstreamAdapter adapter = request -> {
            invocations.incrementAndGet();
            captured.set(request);
            return Mono.just(GatewayOutboundHttpResponse.text(200, "ok"));
        };
        GatewayHttpProxyStrategy strategy =
                new AggregatedHttpProxyStrategy();

        strategy.proxy(context(
                adapter,
                Flux.defer(() -> {
                    subscriptions.incrementAndGet();
                    return GatewayDataBufferTestSupport.body(
                            "{ \n  ",
                            "\"unknown\" : true }"
                    );
                }),
                policy(
                        GatewayRequestBodyMode.AGGREGATED,
                        GatewayTransportResponseMode.STANDARD,
                        OptionalLong.of(1024)
                )
        )).block();

        assertEquals(1, invocations.get());
        assertEquals(1, subscriptions.get());
        assertTrue(captured.get().replayable());
        assertEquals(
                "{ \n  \"unknown\" : true }",
                GatewayDataBufferTestSupport.joinUtf8(
                        captured.get().body(),
                        1024
                )
        );
    }

    @Test
    void streamingBodyIsPassedThroughOnceWithoutReplayability() {
        byte[] payload = new byte[2 * 1024 * 1024 + 37];
        for (int index = 0; index < payload.length; index++) {
            payload[index] = (byte) (index * 31);
        }
        AtomicInteger subscriptions = new AtomicInteger();
        AtomicReference<byte[]> forwarded = new AtomicReference<>();
        HttpUpstreamAdapter adapter = request -> {
            forwarded.set(GatewayDataBufferTestSupport.join(
                    request.body(),
                    payload.length
            ));
            assertFalse(request.replayable());
            return Mono.just(new GatewayOutboundHttpResponse(
                    200,
                    Map.of("content-type", List.of("application/octet-stream")),
                    Flux.empty()
            ));
        };

        new StreamingHttpProxyStrategy().proxy(context(
                adapter,
                Flux.defer(() -> {
                    subscriptions.incrementAndGet();
                    return Flux.just(buffer(payload));
                }),
                policy(
                        GatewayRequestBodyMode.STREAMING,
                        GatewayTransportResponseMode.BINARY_STREAM,
                        OptionalLong.empty()
                )
        )).block();

        assertEquals(1, subscriptions.get());
        assertArrayEquals(payload, forwarded.get());
    }

    @Test
    void enabledBodyLogObservesBoundedRequestAndResponseSamples() {
        List<GatewayBodyLogEvent> events = new java.util.ArrayList<>();
        byte[] requestBody = "request-body".getBytes(StandardCharsets.UTF_8);
        byte[] responseBody = "response-body".getBytes(StandardCharsets.UTF_8);
        AtomicReference<byte[]> forwarded = new AtomicReference<>();
        HttpUpstreamAdapter adapter = request -> {
            forwarded.set(GatewayDataBufferTestSupport.join(
                    request.body(),
                    requestBody.length
            ));
            return Mono.just(new GatewayOutboundHttpResponse(
                    200,
                    Map.of("content-type", List.of("application/json")),
                    Flux.just(buffer(responseBody))
            ));
        };
        EffectiveGatewayTransportPolicy loggingPolicy = policy(
                GatewayRequestBodyMode.STREAMING,
                GatewayTransportResponseMode.AUTO_STREAM,
                OptionalLong.empty(),
                true
        );

        GatewayOutboundHttpResponse response =
                new StreamingHttpProxyStrategy().proxy(
                        new GatewayHttpProxyContext(
                                adapter,
                                provider(),
                                "POST",
                                "/v1/responses",
                                Map.of(
                                        "content-type",
                                        List.of("application/json")
                                ),
                                Flux.just(buffer(requestBody)),
                                loggingPolicy,
                                4,
                                events::add
                        )
                ).block();
        byte[] received = GatewayDataBufferTestSupport.join(
                response.body(),
                responseBody.length
        );

        assertArrayEquals(requestBody, forwarded.get());
        assertArrayEquals(responseBody, received);
        assertEquals(2, events.size());
        assertEquals(
                List.of(
                        GatewayBodyLogDirection.REQUEST,
                        GatewayBodyLogDirection.RESPONSE
                ),
                events.stream().map(GatewayBodyLogEvent::direction).toList()
        );
        assertEquals(4, events.get(0).sample().length);
        assertEquals(requestBody.length, events.get(0).totalBytes());
        assertEquals(4, events.get(1).sample().length);
        assertEquals(responseBody.length, events.get(1).totalBytes());
    }

    @Test
    void sseResponseUsesPerBufferFlushAndNoBufferingHeaders() {
        byte[] events = "data: {\"x\":1}\n\ndata: [DONE]\n\n"
                .getBytes(StandardCharsets.UTF_8);
        HttpUpstreamAdapter adapter = request -> Mono.just(
                new GatewayOutboundHttpResponse(
                        200,
                        Map.of(
                                "Content-Type", List.of("text/event-stream"),
                                "Content-Length", List.of("999")
                        ),
                        Flux.just(buffer(events))
                )
        );

        GatewayOutboundHttpResponse response =
                new StreamingHttpProxyStrategy().proxy(context(
                        adapter,
                        Flux.empty(),
                        policy(
                                GatewayRequestBodyMode.STREAMING,
                                GatewayTransportResponseMode.AUTO_STREAM,
                                OptionalLong.empty()
                        )
                )).block(Duration.ofSeconds(1));

        assertEquals(GatewayHttpFlushMode.PER_BUFFER, response.flushMode());
        assertFalse(response.headers().containsKey("content-length"));
        assertEquals(
                List.of("no-cache, no-transform"),
                response.headers().get("cache-control")
        );
        assertEquals(List.of("no"), response.headers().get("x-accel-buffering"));
        assertArrayEquals(
                events,
                GatewayDataBufferTestSupport.join(response.body(), 1024)
        );
    }

    @Test
    void multipartBoundaryAndBytesPassThroughUnchanged() {
        byte[] multipart = ("--AaB03x\r\n"
                + "Content-Disposition: form-data; name=\"file\"; "
                + "filename=\"audio.wav\"\r\n"
                + "Content-Type: audio/wav\r\n\r\n")
                .getBytes(StandardCharsets.ISO_8859_1);
        byte[] tail = new byte[]{0, (byte) 0xff, 1, 2, '\r', '\n'};
        byte[] expected = new byte[multipart.length + tail.length];
        System.arraycopy(multipart, 0, expected, 0, multipart.length);
        System.arraycopy(tail, 0, expected, multipart.length, tail.length);
        AtomicReference<HttpUpstreamRequest> captured = new AtomicReference<>();
        AtomicReference<byte[]> forwarded = new AtomicReference<>();
        HttpUpstreamAdapter adapter = request -> {
            captured.set(request);
            forwarded.set(GatewayDataBufferTestSupport.join(
                    request.body(),
                    expected.length
            ));
            return Mono.just(GatewayOutboundHttpResponse.text(200, "ok"));
        };

        new StreamingHttpProxyStrategy().proxy(new GatewayHttpProxyContext(
                adapter,
                provider(),
                "POST",
                "/v1/audio/transcriptions",
                Map.of(
                        "content-type",
                        List.of("multipart/form-data; boundary=AaB03x")
                ),
                Flux.just(buffer(expected)),
                policy(
                        GatewayRequestBodyMode.STREAMING,
                        GatewayTransportResponseMode.STANDARD,
                        OptionalLong.of(1024)
                )
        )).block();

        assertEquals(
                List.of("multipart/form-data; boundary=AaB03x"),
                captured.get().headers().get("content-type")
        );
        assertFalse(captured.get().replayable());
        assertArrayEquals(expected, forwarded.get());
    }

    @Test
    void binaryResponseLargerThanFourMiBRemainsRaw() {
        byte[] payload = new byte[4 * 1024 * 1024 + 19];
        for (int index = 0; index < payload.length; index++) {
            payload[index] = (byte) (index * 17 + 0x80);
        }
        HttpUpstreamAdapter adapter = request -> Mono.just(
                new GatewayOutboundHttpResponse(
                        200,
                        Map.of("content-type", List.of("audio/mpeg")),
                        Flux.just(buffer(payload))
                )
        );

        GatewayOutboundHttpResponse response =
                new StreamingHttpProxyStrategy().proxy(context(
                        adapter,
                        Flux.empty(),
                        policy(
                                GatewayRequestBodyMode.STREAMING,
                                GatewayTransportResponseMode.BINARY_STREAM,
                                OptionalLong.empty()
                        )
                )).block();

        assertEquals(GatewayHttpFlushMode.STANDARD, response.flushMode());
        assertArrayEquals(
                payload,
                GatewayDataBufferTestSupport.join(
                        response.body(),
                        payload.length
                )
        );
    }

    @Test
    void streamingCancellationCancelsOriginalRequestBody() {
        AtomicBoolean cancelled = new AtomicBoolean();
        HttpUpstreamAdapter adapter = request -> request.body()
                .then(Mono.just(GatewayOutboundHttpResponse.text(200, "ok")));

        reactor.core.Disposable subscription =
                new StreamingHttpProxyStrategy().proxy(context(
                        adapter,
                        Flux.<DataBuffer>never()
                                .doOnCancel(() -> cancelled.set(true)),
                        policy(
                                GatewayRequestBodyMode.STREAMING,
                                GatewayTransportResponseMode.STANDARD,
                                OptionalLong.of(1024)
                        )
                )).subscribe();
        subscription.dispose();

        assertTrue(cancelled.get());
    }

    @Test
    void selectorRejectsMissingMode() {
        GatewayHttpProxyStrategySelector selector =
                new GatewayHttpProxyStrategySelector(
                        new AggregatedHttpProxyStrategy(),
                        new StreamingHttpProxyStrategy()
                );

        assertThrows(NullPointerException.class, () -> selector.select(null));
    }

    private GatewayHttpProxyContext context(
            HttpUpstreamAdapter adapter,
            Flux<DataBuffer> body,
            EffectiveGatewayTransportPolicy policy) {
        return new GatewayHttpProxyContext(
                adapter,
                provider(),
                "POST",
                "/v1/responses?trace=1",
                Map.of("content-type", List.of("application/json")),
                body,
                policy
        );
    }

    private EffectiveGatewayTransportPolicy policy(
            GatewayRequestBodyMode requestMode,
            GatewayTransportResponseMode responseMode,
            OptionalLong maxResponseBytes) {
        return policy(
                requestMode,
                responseMode,
                maxResponseBytes,
                false
        );
    }

    private EffectiveGatewayTransportPolicy policy(
            GatewayRequestBodyMode requestMode,
            GatewayTransportResponseMode responseMode,
            OptionalLong maxResponseBytes,
            boolean bodyLogEnabled) {
        return new EffectiveGatewayTransportPolicy(
                GatewayRouteProfile.OPENAI_HTTP,
                GatewayTransportProtocol.HTTP,
                requestMode,
                responseMode,
                4 * 1024 * 1024,
                maxResponseBytes,
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                Duration.ofSeconds(5),
                Optional.of(Duration.ofMinutes(2)),
                Optional.empty(),
                OptionalLong.empty(),
                bodyLogEnabled,
                false,
                true
        );
    }

    private ProviderInstance provider() {
        return new ProviderInstance(
                new ProviderServiceKey(
                        "local",
                        "default",
                        ProviderProtocolType.HTTP,
                        "provider",
                        "default",
                        "v1",
                        "http"
                ),
                "provider-a",
                "lease-a",
                "127.0.0.1",
                8080,
                false,
                Map.of(),
                Instant.now().plusSeconds(30),
                ProviderRegistryState.REGISTERED,
                ProviderHealthState.HEALTHY,
                ProviderHealthState.HEALTHY
        );
    }

    private DataBuffer buffer(byte[] bytes) {
        return DefaultDataBufferFactory.sharedInstance.wrap(bytes);
    }
}
