package top.egon.cola.component.gateway.engine.http.service;

import top.egon.cola.component.gateway.engine.common.observability.service.GatewayCallCompletionListener;
import top.egon.cola.component.gateway.engine.http.domain.GatewayInboundHttpRequest;
import top.egon.cola.component.gateway.engine.http.service.GatewayOutboundHttpResponse;
import top.egon.cola.component.gateway.engine.http.security.GatewayHttpSecurityProcessor;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimePolicy;
import top.egon.cola.component.gateway.core.http.HttpRequestNormalizer;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.route.GatewayResponseMode;
import top.egon.cola.component.gateway.core.route.HttpRouteCompiler;
import top.egon.cola.component.gateway.core.route.RuntimeHttpRoute;
import top.egon.cola.component.gateway.engine.http.cors.GatewayCorsPolicyCompiler;
import top.egon.cola.component.gateway.engine.rule.service.GatewayTrafficGovernance;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultGatewayHttpDataPlaneHandlerCorsTest {

    @Test
    void preflightUsesRequestedMethodRouteAndStopsBeforeSecurity() {
        AtomicInteger securityCalls = new AtomicInteger();
        RuntimeHttpRoute route = route();
        var corsPolicies = new GatewayCorsPolicyCompiler().compile(List.of(
                new GatewayRuntimePolicy(
                        "cors",
                        "CORS",
                        "OPERATION",
                        Map.of(
                                "allowedOrigins",
                                List.of("https://app.example.com"),
                                "allowedMethods",
                                List.of("POST"),
                                "allowedHeaders",
                                List.of("content-type")
                        )
                )
        ));
        DefaultGatewayHttpDataPlaneHandler handler =
                new DefaultGatewayHttpDataPlaneHandler(
                        new HttpRequestNormalizer(32, 8192),
                        () -> new HttpRouteCompiler().compile(List.of(route)),
                        ignored -> {
                            throw new AssertionError(
                                    "provider must not be selected"
                            );
                        },
                        ignored -> {
                            throw new AssertionError(
                                    "upstream must not be invoked"
                            );
                        },
                        1024,
                        Duration.ofSeconds(1),
                        (zone, request, normalized, match, traceId) -> {
                            securityCalls.incrementAndGet();
                            return reactor.core.publisher.Mono.just(
                                    GatewayHttpSecurityProcessor.Outcome
                                            .anonymous()
                            );
                        },
                        GatewayCallCompletionListener.noop(),
                        "engine-1",
                        GatewayTrafficGovernance.noop(),
                        null,
                        (identity, outcome) -> {
                        },
                        () -> corsPolicies
                );

        GatewayOutboundHttpResponse response = handler.handle(
                AccessZone.PUBLIC,
                new GatewayInboundHttpRequest(
                        "OPTIONS",
                        "api.example.com",
                        "/orders",
                        Map.of(
                                "origin",
                                List.of("https://app.example.com"),
                                "access-control-request-method",
                                List.of("POST"),
                                "access-control-request-headers",
                                List.of("content-type")
                        ),
                        new InetSocketAddress("127.0.0.1", 12345),
                        Flux.empty()
                )
        ).block();

        assertEquals(204, response.status());
        assertEquals(0, securityCalls.get());
        assertEquals(
                List.of("https://app.example.com"),
                response.headers().get("access-control-allow-origin")
        );
    }

    private RuntimeHttpRoute route() {
        return new RuntimeHttpRoute(
                "route",
                "operation",
                "group",
                Set.of(AccessZone.PUBLIC),
                "api.example.com",
                Set.of("POST"),
                "/orders",
                true,
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
                Set.of("cors"),
                0,
                GatewayResponseMode.TRANSPARENT,
                Map.of()
        );
    }
}
