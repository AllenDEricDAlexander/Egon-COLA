package top.egon.cola.component.gateway.engine.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.rule.GatewayRequestBodyMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteProfile;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimePolicy;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportResponseMode;
import top.egon.cola.component.gateway.core.http.HttpRequestNormalizer;
import top.egon.cola.component.gateway.core.provider.ProviderHealthState;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderRegistryState;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.route.GatewayResponseMode;
import top.egon.cola.component.gateway.core.route.HttpRouteCompiler;
import top.egon.cola.component.gateway.core.route.RuntimeHttpRoute;
import top.egon.cola.component.gateway.core.transport
        .EffectiveGatewayTransportPolicy;
import top.egon.cola.component.gateway.engine.balance.ProviderSelectionHandle;
import top.egon.cola.component.gateway.engine.discovery.ProviderCallOutcome;
import top.egon.cola.component.gateway.engine.observability
        .GatewayCallCompletionListener;
import top.egon.cola.component.gateway.engine.rule.CompiledGatewayRules;
import top.egon.cola.component.gateway.engine.rpc.RpcMethodIndex;
import top.egon.cola.component.gateway.engine.traffic.GatewayTrafficGovernance;
import top.egon.cola.component.gateway.engine.traffic
        .GatewayTrafficPolicyCompiler;
import top.egon.cola.component.gateway.engine.traffic.RuntimeTrafficPolicy;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultGatewayHttpDataPlaneHandlerRetryTest {

    @Test
    void retriesConfiguredStatusForReplayableIdempotentRequest() {
        ProviderInstance provider = provider();
        AtomicInteger upstreamCalls = new AtomicInteger();
        AtomicInteger drainedResponses = new AtomicInteger();
        List<ProviderCallOutcome> outcomes = new ArrayList<>();
        NettyDataBuffer retryBody = new NettyDataBufferFactory(
                PooledByteBufAllocator.DEFAULT
        ).allocateBuffer(5);
        retryBody.write("retry".getBytes(
                java.nio.charset.StandardCharsets.UTF_8
        ));
        ByteBuf retryNative = retryBody.getNativeBuffer();
        DefaultGatewayHttpDataPlaneHandler handler =
                new DefaultGatewayHttpDataPlaneHandler(
                        new HttpRequestNormalizer(32, 8192),
                        () -> new HttpRouteCompiler().compile(
                                List.of(route())
                        ),
                        ignored -> new ProviderSelectionHandle(
                                provider,
                                () -> {
                                }
                        ),
                        request -> {
                            int call = upstreamCalls.incrementAndGet();
                            if (call == 1) {
                                return Mono.just(
                                        new GatewayOutboundHttpResponse(
                                                503,
                                                Map.of(),
                                                Flux.<DataBuffer>just(retryBody)
                                                        .doFinally(ignored ->
                                                                drainedResponses
                                                                        .incrementAndGet())
                                        )
                                );
                            }
                            return Mono.just(
                                    GatewayOutboundHttpResponse.text(
                                            200,
                                            "response"
                                    )
                            );
                        },
                        1024,
                        Duration.ofSeconds(1),
                        (zone, request, normalized, route, traceId) ->
                                Mono.just(
                                        GatewayHttpSecurityProcessor.Outcome
                                                .anonymous()
                                ),
                        GatewayCallCompletionListener.noop(),
                        "engine-1",
                        retryGovernance(),
                        null,
                        (runtimeIdentity, outcome) -> outcomes.add(outcome)
                );

        GatewayOutboundHttpResponse response = handler.handle(
                AccessZone.INTERNAL,
                new GatewayInboundHttpRequest(
                        "GET",
                        "api.example.com",
                        "/orders",
                        Map.of(),
                        new InetSocketAddress("127.0.0.1", 12345),
                        Flux.empty()
                )
        ).block();
        String body = GatewayDataBufferTestSupport.joinUtf8(
                response.body(),
                1024
        );

        assertEquals(200, response.status());
        assertEquals("response", body);
        assertEquals(2, upstreamCalls.get());
        assertEquals(1, drainedResponses.get());
        assertEquals(0, retryNative.refCnt());
        assertEquals(
                List.of(
                        ProviderCallOutcome.RETRYABLE_FAILURE,
                        ProviderCallOutcome.SUCCESS
                ),
                outcomes
        );
    }

    @Test
    void openAiStreamingRouteReturnsRetryableStatusWithoutRetrying() {
        ProviderInstance provider = provider();
        AtomicInteger upstreamCalls = new AtomicInteger();
        DefaultGatewayHttpDataPlaneHandler handler =
                new DefaultGatewayHttpDataPlaneHandler(
                        new HttpRequestNormalizer(32, 8192),
                        () -> new HttpRouteCompiler().compile(
                                List.of(openAiRoute())
                        ),
                        ignored -> new ProviderSelectionHandle(
                                provider,
                                () -> {
                                }
                        ),
                        request -> {
                            upstreamCalls.incrementAndGet();
                            return Mono.just(
                                    GatewayOutboundHttpResponse.text(
                                            503,
                                            "upstream-unavailable"
                                    )
                            );
                        },
                        1024,
                        Duration.ofSeconds(1),
                        (zone, request, normalized, route, traceId) ->
                                Mono.just(
                                        GatewayHttpSecurityProcessor.Outcome
                                                .anonymous()
                                ),
                        GatewayCallCompletionListener.noop(),
                        "engine-1",
                        retryGovernance(),
                        null,
                        (runtimeIdentity, outcome) -> {
                        }
                );

        GatewayOutboundHttpResponse response = handler.handle(
                AccessZone.INTERNAL,
                new GatewayInboundHttpRequest(
                        "POST",
                        "api.example.com",
                        "/v1/responses",
                        Map.of(
                                "content-type",
                                List.of("application/json"),
                                "content-length",
                                List.of(Integer.toString(3 * 1024 * 1024))
                        ),
                        new InetSocketAddress("127.0.0.1", 12345),
                        Flux.empty()
                )
        ).block();

        assertEquals(503, response.status());
        assertEquals(
                "upstream-unavailable",
                GatewayDataBufferTestSupport.joinUtf8(response.body(), 1024)
        );
        assertEquals(1, upstreamCalls.get());
    }

    @Test
    void keepsProviderAttemptOpenUntilStreamingBodyTerminates() {
        ProviderInstance provider = provider();
        AtomicInteger selectionReleases = new AtomicInteger();
        List<ProviderCallOutcome> outcomes = new ArrayList<>();
        DefaultGatewayHttpDataPlaneHandler handler =
                new DefaultGatewayHttpDataPlaneHandler(
                        new HttpRequestNormalizer(32, 8192),
                        () -> new HttpRouteCompiler().compile(
                                List.of(route())
                        ),
                        ignored -> new ProviderSelectionHandle(
                                provider,
                                selectionReleases::incrementAndGet
                        ),
                        request -> Mono.just(
                                new GatewayOutboundHttpResponse(
                                        200,
                                        Map.of(),
                                        Flux.never()
                                )
                        ),
                        1024,
                        Duration.ofSeconds(1),
                        (zone, request, normalized, route, traceId) ->
                                Mono.just(
                                        GatewayHttpSecurityProcessor.Outcome
                                                .anonymous()
                                ),
                        GatewayCallCompletionListener.noop(),
                        "engine-1",
                        GatewayTrafficGovernance.noop(),
                        null,
                        (runtimeIdentity, outcome) -> outcomes.add(outcome)
                );

        GatewayOutboundHttpResponse response = handler.handle(
                AccessZone.INTERNAL,
                new GatewayInboundHttpRequest(
                        "GET",
                        "api.example.com",
                        "/orders",
                        Map.of(),
                        new InetSocketAddress("127.0.0.1", 12345),
                        Flux.empty()
                )
        ).block();

        assertEquals(0, selectionReleases.get());
        assertEquals(List.of(), outcomes);

        Disposable subscription = response.body().subscribe();
        subscription.dispose();

        assertEquals(1, selectionReleases.get());
        assertEquals(List.of(ProviderCallOutcome.CANCELLED), outcomes);
    }

    @Test
    void releasesProviderAttemptWhenCancelledBeforeResponseHandoff() {
        ProviderInstance provider = provider();
        AtomicInteger selectionReleases = new AtomicInteger();
        AtomicInteger upstreamCancellations = new AtomicInteger();
        List<ProviderCallOutcome> outcomes = new ArrayList<>();
        DefaultGatewayHttpDataPlaneHandler handler =
                new DefaultGatewayHttpDataPlaneHandler(
                        new HttpRequestNormalizer(32, 8192),
                        () -> new HttpRouteCompiler().compile(
                                List.of(route())
                        ),
                        ignored -> new ProviderSelectionHandle(
                                provider,
                                selectionReleases::incrementAndGet
                        ),
                        request -> Mono
                                .<GatewayOutboundHttpResponse>never()
                                .doOnCancel(
                                        upstreamCancellations::incrementAndGet
                                ),
                        4L * 1024 * 1024,
                        Duration.ofSeconds(1),
                        (zone, request, normalized, route, traceId) ->
                                Mono.just(
                                        GatewayHttpSecurityProcessor.Outcome
                                                .anonymous()
                                ),
                        GatewayCallCompletionListener.noop(),
                        "engine-1",
                        GatewayTrafficGovernance.noop(),
                        null,
                        (runtimeIdentity, outcome) -> outcomes.add(outcome)
                );

        Disposable subscription = handler.handle(
                AccessZone.INTERNAL,
                new GatewayInboundHttpRequest(
                        "GET",
                        "api.example.com",
                        "/orders",
                        Map.of(),
                        new InetSocketAddress("127.0.0.1", 12345),
                        Flux.empty()
                )
        ).subscribe();
        subscription.dispose();

        assertEquals(1, upstreamCancellations.get());
        assertEquals(1, selectionReleases.get());
        assertEquals(List.of(ProviderCallOutcome.CANCELLED), outcomes);
    }

    @Test
    void recordsStreamingBodyFailureBeforeReleasingProviderAttempt() {
        ProviderInstance provider = provider();
        AtomicInteger selectionReleases = new AtomicInteger();
        List<ProviderCallOutcome> outcomes = new ArrayList<>();
        DefaultGatewayHttpDataPlaneHandler handler =
                new DefaultGatewayHttpDataPlaneHandler(
                        new HttpRequestNormalizer(32, 8192),
                        () -> new HttpRouteCompiler().compile(
                                List.of(route())
                        ),
                        ignored -> new ProviderSelectionHandle(
                                provider,
                                selectionReleases::incrementAndGet
                        ),
                        request -> Mono.just(
                                new GatewayOutboundHttpResponse(
                                        200,
                                        Map.of(),
                                        Flux.error(new java.io.IOException(
                                                "stream failed"
                                        ))
                                )
                        ),
                        1024,
                        Duration.ofSeconds(1),
                        (zone, request, normalized, route, traceId) ->
                                Mono.just(
                                        GatewayHttpSecurityProcessor.Outcome
                                                .anonymous()
                                ),
                        GatewayCallCompletionListener.noop(),
                        "engine-1",
                        GatewayTrafficGovernance.noop(),
                        null,
                        (runtimeIdentity, outcome) -> outcomes.add(outcome)
                );

        GatewayOutboundHttpResponse response = handler.handle(
                AccessZone.INTERNAL,
                new GatewayInboundHttpRequest(
                        "GET",
                        "api.example.com",
                        "/orders",
                        Map.of(),
                        new InetSocketAddress("127.0.0.1", 12345),
                        Flux.empty()
                )
        ).block();

        assertEquals(0, selectionReleases.get());
        assertEquals(List.of(), outcomes);

        assertThrows(
                RuntimeException.class,
                () -> response.body().then().block()
        );

        assertEquals(1, selectionReleases.get());
        assertEquals(
                List.of(ProviderCallOutcome.RETRYABLE_FAILURE),
                outcomes
        );
    }

    private RuntimeHttpRoute route() {
        return new RuntimeHttpRoute(
                "route",
                "operation",
                "group",
                Set.of(AccessZone.INTERNAL),
                "api.example.com",
                Set.of("GET"),
                "/orders",
                true,
                provider().serviceKey(),
                Set.of("retry"),
                0,
                GatewayResponseMode.TRANSPARENT,
                Map.of("idempotent", "true")
        );
    }

    private RuntimeHttpRoute openAiRoute() {
        return new RuntimeHttpRoute(
                "openai-route",
                "openai-operation",
                "group",
                Set.of(AccessZone.INTERNAL),
                "api.example.com",
                Set.of("POST"),
                "/v1/responses",
                true,
                provider().serviceKey(),
                Set.of("retry"),
                0,
                GatewayResponseMode.TRANSPARENT,
                Map.of("idempotent", "true"),
                new EffectiveGatewayTransportPolicy(
                        GatewayRouteProfile.OPENAI_HTTP,
                        GatewayTransportProtocol.HTTP,
                        GatewayRequestBodyMode.STREAMING,
                        GatewayTransportResponseMode.SSE,
                        4L * 1024 * 1024,
                        OptionalLong.empty(),
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1),
                        Optional.of(Duration.ofSeconds(5)),
                        Optional.empty(),
                        OptionalLong.empty(),
                        false,
                        false,
                        true
                )
        );
    }

    private ProviderInstance provider() {
        return new ProviderInstance(
                new ProviderServiceKey(
                        "test-biz",
                        "test-app",
                        "test",
                        "default",
                        ProviderProtocolType.HTTP,
                        "orders",
                        "default",
                        "v1",
                        "http"
                ),
                "provider",
                "lease",
                "127.0.0.1",
                18090,
                false,
                Map.of(),
                Instant.now().plusSeconds(30),
                ProviderRegistryState.REGISTERED,
                ProviderHealthState.HEALTHY,
                ProviderHealthState.HEALTHY
        );
    }

    private GatewayTrafficGovernance retryGovernance() {
        RuntimeTrafficPolicy retry = new GatewayTrafficPolicyCompiler()
                .compile(List.of(new GatewayRuntimePolicy(
                        "retry",
                        "RETRY",
                        "OPERATION",
                        Map.of(
                                "maxAttempts", 2,
                                "initialBackoff", "PT0S",
                                "maximumBackoff", "PT0S",
                                "minimumAttemptBudget", "PT0.001S",
                                "retryableHttpStatuses", List.of(503)
                        )
                ))).get("retry");
        GatewayRuleContent content = new GatewayRuleContent(
                "group",
                "group",
                "test",
                "default",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        CompiledGatewayRules rules = new CompiledGatewayRules(
                new GatewayRuleSnapshot(
                        "v1",
                        "release",
                        Instant.EPOCH,
                        "content",
                        "artifact",
                        content
                ),
                new HttpRouteCompiler().compile(List.of()),
                RpcMethodIndex.empty(),
                Set.of(),
                Map.of(),
                Map.of("retry", retry),
                Map.of(),
                Map.of()
        );
        return new GatewayTrafficGovernance(() -> rules, null);
    }
}
