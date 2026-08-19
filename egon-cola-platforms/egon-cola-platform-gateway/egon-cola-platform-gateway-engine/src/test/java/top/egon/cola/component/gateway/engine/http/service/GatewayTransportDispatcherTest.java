package top.egon.cola.component.gateway.engine.http.service;

import top.egon.cola.component.gateway.engine.http.service.GatewayOutboundHttpResponse;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
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
import top.egon.cola.component.gateway.engine.http.proxy.domain.GatewayHttpProxyContext;
import top.egon.cola.component.gateway.engine.http.proxy.service.GatewayHttpProxyStrategySelector;
import top.egon.cola.component.gateway.engine.http.websocket.service.GatewayWebSocketProxy;

import java.util.concurrent.atomic.AtomicInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayTransportDispatcherTest {

    @Test
    void selectsOnlyTheConfiguredHttpBodyStrategy() {
        AtomicInteger aggregated = new AtomicInteger();
        AtomicInteger streaming = new AtomicInteger();
        GatewayTransportDispatcher dispatcher = new GatewayTransportDispatcher(
                new GatewayHttpProxyStrategySelector(
                        context -> response(aggregated),
                        context -> response(streaming)
                ),
                new GatewayWebSocketProxy(context -> Mono.error(
                        new AssertionError("unexpected websocket dispatch")
                ))
        );

        GatewayOutboundHttpResponse result = dispatcher.dispatchHttp(
                context(GatewayRequestBodyMode.STREAMING)
        ).block();

        assertEquals(200, result.status());
        assertEquals(0, aggregated.get());
        assertEquals(1, streaming.get());
    }

    private Mono<GatewayOutboundHttpResponse> response(
            AtomicInteger counter) {
        counter.incrementAndGet();
        return Mono.just(GatewayOutboundHttpResponse.text(200, "ok"));
    }

    private GatewayHttpProxyContext context(GatewayRequestBodyMode mode) {
        return new GatewayHttpProxyContext(
                request -> Mono.error(new AssertionError("unused adapter")),
                new ProviderInstance(
                        new ProviderServiceKey(
                                "test-biz",
                                "test-app",
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
                ),
                "POST",
                "/v1/responses",
                Map.of(),
                Flux.empty(),
                new EffectiveGatewayTransportPolicy(
                        GatewayRouteProfile.OPENAI_HTTP,
                        GatewayTransportProtocol.HTTP,
                        mode,
                        GatewayTransportResponseMode.AUTO_STREAM,
                        1024,
                        OptionalLong.empty(),
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(3),
                        Optional.of(Duration.ofSeconds(10)),
                        Optional.empty(),
                        OptionalLong.empty(),
                        false,
                        false,
                        true
                )
        );
    }
}
