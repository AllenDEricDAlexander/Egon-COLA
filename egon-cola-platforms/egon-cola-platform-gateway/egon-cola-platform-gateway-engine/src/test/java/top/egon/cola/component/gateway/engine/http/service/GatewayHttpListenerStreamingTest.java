package top.egon.cola.component.gateway.engine.http.service;

import top.egon.cola.component.gateway.engine.http.domain.GatewayHttpEngineProperties;
import top.egon.cola.component.gateway.engine.http.domain.GatewayHttpFlushMode;
import top.egon.cola.component.gateway.engine.http.domain.GatewayInboundHttpRequest;
import top.egon.cola.component.gateway.engine.http.service.GatewayOutboundHttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayHttpListenerStreamingTest {

    @Test
    void recognizesOnlyStrictWebSocketUpgradeRequests() {
        assertTrue(GatewayHttpListener.webSocketUpgrade(request(
                "GET",
                Map.of(
                        "Connection", List.of("keep-alive, Upgrade"),
                        "Upgrade", List.of("websocket")
                )
        )));
        assertFalse(GatewayHttpListener.webSocketUpgrade(request(
                "POST",
                Map.of(
                        "Connection", List.of("upgrade"),
                        "Upgrade", List.of("websocket")
                )
        )));
        assertFalse(GatewayHttpListener.webSocketUpgrade(request(
                "GET",
                Map.of("Upgrade", List.of("websocket"))
        )));
        assertFalse(GatewayHttpListener.webSocketUpgrade(request(
                "GET",
                Map.of("Connection", List.of("upgrade"))
        )));
    }

    @Test
    void perBufferResponseFlushesBeforeTheStreamCompletes() {
        GatewayHttpServer server = new GatewayHttpServer(
                properties(),
                (zone, request) -> Mono.just(
                        new GatewayOutboundHttpResponse(
                                200,
                                Map.of("content-type", List.of(
                                        "text/event-stream"
                                )),
                                Flux.concat(
                                        Mono.just(buffer("data:first\n\n")),
                                        Mono.delay(Duration.ofMillis(700))
                                                .map(ignored -> buffer(
                                                        "data:second\n\n"
                                                ))
                                ),
                                GatewayHttpFlushMode.PER_BUFFER
                        )
                )
        );
        server.start();
        long started = System.nanoTime();
        try {
            byte[] first = HttpClient.create()
                    .get()
                    .uri("http://127.0.0.1:"
                            + server.publicPort()
                            + "/events")
                    .response((response, body) -> body.next()
                            .map(buffer -> {
                                byte[] bytes = new byte[buffer.readableBytes()];
                                buffer.readBytes(bytes);
                                return bytes;
                            }))
                    .single()
                    .block(Duration.ofSeconds(2));

            long elapsedMillis = Duration.ofNanos(
                    System.nanoTime() - started
            ).toMillis();
            assertTrue(new String(
                    first,
                    java.nio.charset.StandardCharsets.UTF_8
            ).contains("data:first"));
            assertTrue(elapsedMillis < 550, "elapsedMillis=" + elapsedMillis);
        } finally {
            server.close();
        }
    }

    private GatewayInboundHttpRequest request(
            String method,
            Map<String, List<String>> headers) {
        return new GatewayInboundHttpRequest(
                method,
                "api.example.com",
                "/v1/realtime",
                headers,
                new InetSocketAddress("127.0.0.1", 12345),
                Flux.empty()
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

    private org.springframework.core.io.buffer.DataBuffer buffer(
            String value) {
        return DefaultDataBufferFactory.sharedInstance.wrap(
                value.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
    }
}
