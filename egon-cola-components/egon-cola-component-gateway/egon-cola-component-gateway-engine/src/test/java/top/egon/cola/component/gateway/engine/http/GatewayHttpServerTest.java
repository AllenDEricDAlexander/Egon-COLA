package top.egon.cola.component.gateway.engine.http;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayHttpServerTest {

    @Test
    void bindsIndependentPortsAndIgnoresForgedAccessZoneHeader() {
        GatewayHttpEngineProperties properties =
                new GatewayHttpEngineProperties(
                        new GatewayHttpEngineProperties.Listener(
                                true,
                                "127.0.0.1",
                                0
                        ),
                        new GatewayHttpEngineProperties.Listener(
                                true,
                                "127.0.0.1",
                                0
                        ),
                        64,
                        8192,
                        2 * 1024 * 1024,
                        Duration.ofSeconds(30),
                        Duration.ofSeconds(5),
                        10,
                        10
                );
        GatewayHttpServer server = new GatewayHttpServer(
                properties,
                (zone, request) -> request.body()
                        .collectList()
                        .map(ignored -> GatewayOutboundHttpResponse.text(
                                200,
                                zone.name()
                        ))
        );
        server.start();
        try {
            assertNotEquals(server.publicPort(), server.internalPort());
            String publicBody = HttpClient.create()
                    .headers(headers ->
                            headers.set("x-access-zone", "INTERNAL"))
                    .get()
                    .uri("http://127.0.0.1:"
                            + server.publicPort()
                            + "/zone")
                    .responseSingle((response, body) -> body.asString())
                    .block();
            String internalBody = HttpClient.create()
                    .headers(headers ->
                            headers.set("x-access-zone", "PUBLIC"))
                    .get()
                    .uri("http://127.0.0.1:"
                            + server.internalPort()
                            + "/zone")
                    .responseSingle((response, body) -> body.asString())
                    .block();

            assertEquals("PUBLIC", publicBody);
            assertEquals("INTERNAL", internalBody);
        } finally {
            server.close();
        }
    }

    @Test
    void stopsAcceptingNewRequestsBeforeClosingListeners() {
        GatewayHttpEngineProperties properties =
                new GatewayHttpEngineProperties(
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
        GatewayHttpServer server = new GatewayHttpServer(
                properties,
                (zone, request) -> Mono.just(
                        GatewayOutboundHttpResponse.text(200, "OK")
                )
        );
        server.start();
        try {
            server.beginDrain();
            String response = HttpClient.create()
                    .get()
                    .uri("http://127.0.0.1:"
                            + server.publicPort()
                            + "/")
                    .responseSingle((headers, body) -> body.asString())
                    .block();
            assertTrue(response.contains("GATEWAY_ENGINE_DRAINING"));
        } finally {
            server.close();
        }
    }
}
