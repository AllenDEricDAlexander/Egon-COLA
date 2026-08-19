package top.egon.cola.component.gateway.engine.http.service;

import top.egon.cola.component.gateway.engine.http.service.GatewayOutboundHttpResponse;
import top.egon.cola.component.gateway.engine.http.domain.HttpUpstreamRequest;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import top.egon.cola.component.gateway.core.provider.ProviderHealthState;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderRegistryState;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReactorNettyHttpUpstreamAdapterTest {

    @Test
    void connectsOnlyToSelectedProviderAndFiltersForgedHeaders() {
        DisposableServer provider = HttpServer.create()
                .host("127.0.0.1")
                .port(0)
                .handle((request, response) -> response.sendString(
                        request.receive()
                                .aggregate()
                                .asString()
                                .map(body -> request.requestHeaders()
                                        .get("x-egon-principal", "missing")
                                        + ":"
                                        + body)
                ))
                .bindNow();
        ReactorNettyHttpUpstreamAdapter adapter =
                new ReactorNettyHttpUpstreamAdapter(
                        4,
                        4,
                        Duration.ofSeconds(30)
                );
        try {
            GatewayOutboundHttpResponse response = adapter.invoke(
                    new HttpUpstreamRequest(
                            provider(provider.port()),
                            "POST",
                            "/echo",
                            Map.of(
                                    "x-egon-principal",
                                    List.of("forged"),
                                    "x-test",
                                    List.of("safe")
                            ),
                            GatewayDataBufferTestSupport.body("hel", "lo"),
                            Duration.ofSeconds(3)
                    )
            ).block();

            String body = GatewayDataBufferTestSupport.joinUtf8(
                    response.body(),
                    64
            );
            assertEquals(200, response.status());
            assertEquals("missing:hello", body);
        } finally {
            adapter.close();
            provider.disposeNow();
        }
    }

    @Test
    void refusesAbsoluteUrlsFromRoutes() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new HttpUpstreamRequest(
                        provider(8080),
                        "GET",
                        "http://attacker.invalid/",
                        Map.of(),
                        Flux.empty(),
                        Duration.ofSeconds(1)
                )
        );
    }

    @Test
    void returnsResponseHeadersBeforeUpstreamBodyCompletes() {
        Sinks.Many<String> chunks = Sinks.many()
                .unicast()
                .onBackpressureBuffer();
        DisposableServer provider = HttpServer.create()
                .host("127.0.0.1")
                .port(0)
                .handle((request, response) ->
                        response.sendString(chunks.asFlux()))
                .bindNow();
        ReactorNettyHttpUpstreamAdapter adapter =
                new ReactorNettyHttpUpstreamAdapter(
                        4,
                        4,
                        Duration.ofSeconds(30)
                );
        try {
            GatewayOutboundHttpResponse response = adapter.invoke(
                    new HttpUpstreamRequest(
                            provider(provider.port()),
                            "GET",
                            "/stream",
                            Map.of(),
                            Flux.empty(),
                            Duration.ofSeconds(3)
                    )
            ).block(Duration.ofSeconds(1));

            assertNotNull(response);
            chunks.tryEmitNext("first");
            chunks.tryEmitNext("second");
            chunks.tryEmitComplete();
            String body = GatewayDataBufferTestSupport.joinUtf8(
                    response.body(),
                    64
            );
            assertEquals("firstsecond", body);
        } finally {
            adapter.close();
            provider.disposeNow();
        }
    }

    @Test
    void preservesRawResponseBytesAcrossNettyBoundary() {
        byte[] first = new byte[]{0, 1, 2, (byte) 0xff};
        byte[] second = new byte[]{3, 4, (byte) 0x80, 5};
        byte[] expected = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(
                second,
                0,
                expected,
                first.length,
                second.length
        );
        DisposableServer provider = HttpServer.create()
                .host("127.0.0.1")
                .port(0)
                .handle((request, response) -> response.send(
                        Flux.just(
                                response.alloc().buffer().writeBytes(first),
                                response.alloc().buffer().writeBytes(second)
                        )
                ))
                .bindNow();
        ReactorNettyHttpUpstreamAdapter adapter =
                new ReactorNettyHttpUpstreamAdapter(
                        4,
                        4,
                        Duration.ofSeconds(30)
                );
        try {
            GatewayOutboundHttpResponse response = adapter.invoke(
                    new HttpUpstreamRequest(
                            provider(provider.port()),
                            "GET",
                            "/raw",
                            Map.of(),
                            Flux.empty(),
                            Duration.ofSeconds(3)
                    )
            ).block();

            assertEquals(
                    GatewayDataBufferTestSupport.sha256(
                            GatewayDataBufferTestSupport.body(expected),
                            64
                    ),
                    GatewayDataBufferTestSupport.sha256(response.body(), 64)
            );
        } finally {
            adapter.close();
            provider.disposeNow();
        }
    }

    private ProviderInstance provider(int port) {
        return new ProviderInstance(
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
                port,
                false,
                Map.of(),
                Instant.now().plusSeconds(30),
                ProviderRegistryState.REGISTERED,
                ProviderHealthState.HEALTHY,
                ProviderHealthState.HEALTHY
        );
    }
}
