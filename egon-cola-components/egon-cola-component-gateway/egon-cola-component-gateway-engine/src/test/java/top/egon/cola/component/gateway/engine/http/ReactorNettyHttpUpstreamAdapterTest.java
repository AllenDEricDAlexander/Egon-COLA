package top.egon.cola.component.gateway.engine.http;

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

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
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
                            Flux.just("hello".getBytes(StandardCharsets.UTF_8)),
                            Duration.ofSeconds(3)
                    )
            ).block();

            String body = response.body()
                    .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                    .collectList()
                    .map(parts -> String.join("", parts))
                    .block();
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
            String body = response.body()
                    .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                    .collectList()
                    .map(parts -> String.join("", parts))
                    .block(Duration.ofSeconds(1));
            assertEquals("firstsecond", body);
        } finally {
            adapter.close();
            provider.disposeNow();
        }
    }

    private ProviderInstance provider(int port) {
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
