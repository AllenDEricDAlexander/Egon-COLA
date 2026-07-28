package top.egon.cola.component.gateway.engine.http;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.core.http.HttpRequestNormalizer;
import top.egon.cola.component.gateway.core.route.HttpRouteCompiler;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultGatewayHttpDataPlaneHandlerTraceTest {

    @Test
    void returnsSelectedTraceAndPublishesOneRejectedEvent() {
        List<GatewayCallEventV1> events = new ArrayList<>();
        DefaultGatewayHttpDataPlaneHandler handler =
                new DefaultGatewayHttpDataPlaneHandler(
                        new HttpRequestNormalizer(32, 8192),
                        () -> new HttpRouteCompiler().compile(List.of()),
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
                        (zone, request, normalized, route, traceId) ->
                                reactor.core.publisher.Mono.just(
                                        GatewayHttpSecurityProcessor.Outcome
                                                .anonymous()
                                ),
                        events::add,
                        "engine-1",
                        "dev",
                        "codex-local"
                );
        String traceId = "0123456789abcdef0123456789abcdef";
        GatewayOutboundHttpResponse response = handler.handle(
                AccessZone.PUBLIC,
                new GatewayInboundHttpRequest(
                        "GET",
                        "example.test",
                        "/missing",
                        Map.of("X-Trace-Id", List.of(traceId)),
                        new InetSocketAddress("127.0.0.1", 12345),
                        Flux.empty()
                )
        ).block();

        response.body().collectList().block();

        assertEquals(List.of(traceId), response.headers().get("x-trace-id"));
        assertEquals(1, events.size());
        assertEquals(traceId, events.getFirst().trace().traceId());
        assertEquals("dev", events.getFirst().routing().env());
        assertEquals(
                "codex-local",
                events.getFirst().routing().namespace()
        );
        assertEquals(
                "GATEWAY_ROUTE_NOT_FOUND",
                events.getFirst().result().gatewayErrorCode()
        );
    }
}
