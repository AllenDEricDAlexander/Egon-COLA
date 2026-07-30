package top.egon.cola.component.gateway.engine.http;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.http.client.HttpClient;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void preservesRawRequestBytesAcrossListenerBoundary() {
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
                (zone, request) -> GatewayDataBufferTestSupport.sha256Mono(
                                request.body(),
                                1024
                        )
                        .map(checksum -> GatewayOutboundHttpResponse.text(
                                200,
                                checksum
                        ))
        );
        byte[] first = new byte[]{0, 1, (byte) 0xff, 2};
        byte[] second = new byte[]{3, (byte) 0x80, 4};
        byte[] complete = new byte[first.length + second.length];
        System.arraycopy(first, 0, complete, 0, first.length);
        System.arraycopy(
                second,
                0,
                complete,
                first.length,
                second.length
        );
        server.start();
        try {
            String checksum = HttpClient.create()
                    .post()
                    .uri("http://127.0.0.1:"
                            + server.publicPort()
                            + "/checksum")
                    .send((request, outbound) -> outbound.send(
                            reactor.core.publisher.Flux.just(
                                    outbound.alloc().buffer()
                                            .writeBytes(first),
                                    outbound.alloc().buffer()
                                            .writeBytes(second)
                            )
                    ))
                    .responseSingle((response, body) -> body.asString())
                    .block();

            assertEquals(
                    GatewayDataBufferTestSupport.sha256(
                            GatewayDataBufferTestSupport.body(complete),
                            1024
                    ),
                    checksum
            );
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

    @Test
    void waitsForAcceptedRequestsUntilConfiguredDrainDeadline()
            throws Exception {
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
                        Duration.ofMillis(50),
                        10,
                        10
                );
        CountDownLatch accepted = new CountDownLatch(1);
        Sinks.One<GatewayOutboundHttpResponse> completion =
                Sinks.one();
        GatewayHttpServer server = new GatewayHttpServer(
                properties,
                (zone, request) -> {
                    accepted.countDown();
                    return completion.asMono();
                }
        );
        server.start();
        try {
            CompletableFuture<String> response =
                    CompletableFuture.supplyAsync(() -> HttpClient.create()
                            .get()
                            .uri("http://127.0.0.1:"
                                    + server.publicPort()
                                    + "/slow")
                            .responseSingle((headers, body) -> body.asString())
                            .block());
            assertTrue(accepted.await(1, TimeUnit.SECONDS));

            server.beginDrain();

            assertFalse(server.awaitDrain());
            completion.tryEmitValue(
                    GatewayOutboundHttpResponse.text(200, "DONE")
            );
            assertTrue(server.awaitDrain());
            assertEquals("DONE", response.get(1, TimeUnit.SECONDS));
        } finally {
            completion.tryEmitValue(
                    GatewayOutboundHttpResponse.text(500, "CLOSED")
            );
            server.close();
        }
    }
}
