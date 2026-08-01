package top.egon.cola.component.gateway.engine.websocket;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.WebsocketServerSpec;
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
import top.egon.cola.component.gateway.engine.transport.GatewayCommitGuard;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactorNettyWebSocketUpstreamAdapterTest {

    private static final String TEST_CERTIFICATE = """
            -----BEGIN CERTIFICATE-----
            MIICwzCCAaugAwIBAgIJAJwnCYEzCf/NMA0GCSqGSIb3DQEBCwUAMBQxEjAQBgNV
            BAMMCWxvY2FsaG9zdDAeFw0yNjA3MzAwOTU0NDVaFw0zNjA3MjcwOTU0NDVaMBQx
            EjAQBgNVBAMMCWxvY2FsaG9zdDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoC
            ggEBALaKv/6Ye6h2OCv7hwHr2pP+ydR/Wg6ky3cVGJDUVj1115mVQsjzJDVWtq8x
            kwKxSTxk7olRTLzeWFtd5OwQDzKQ7fBWWh0VzxUFBBwODh6LESFod4NnlnvUcJ8s
            jA+wjaNdGJOnH0FhlUibOfXB1ajtrdnquTe1VZCB+VZrUYk7Wb+KaY7vajIlNVWZ
            Ck1s9FJSd+jduHW4LSB9SJisy7jqoRlrLYNgcOoGe87WDqR/3nBQ+U5T4+xY7+B+
            9jfroHJPyG7Whz29tIwRVNqtsYE5wInYYgRGzfodH9lzR+/ns9r2IUs/4N3gadXW
            gePKgcHYvtWsgrbLjaTCVvvDzGUCAwEAAaMYMBYwFAYDVR0RBA0wC4IJbG9jYWxo
            b3N0MA0GCSqGSIb3DQEBCwUAA4IBAQCrxq+EUrV7ph6mmZRJOnSuRdOR4D1UKAiB
            RF3Yh12gg20bT9DVyBYH8Ab+xZHyBtemWMj0N/STNOErXjhZ1PhsEIh84eAipzF2
            NbJOsPG4nYSWJziDm86JGgoy3PCb3cbzOMH968vO8Azj6dZCY1Ox7ROxuYpRw5ue
            8XfTqhTm8OBKBjyD+FXpQZqGyc93acinWfSKQwlrkn+E3Jt5yeSLxExGf7wWeQ9V
            KsyZV/j66/rzNMf7/P4g4PFvEtN7VO1F8iwLZczIWzSXPalcYoxg+IgKOEaYd+Q7
            pjn8eifhpHyZ37KIwcuRLeK17JuUn7H8Bwen+DRL2ezr8Vc8q2ga
            -----END CERTIFICATE-----
            """;

    private static final String TEST_PRIVATE_KEY = """
            -----BEGIN PRIVATE KEY-----
            MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC2ir/+mHuodjgr
            +4cB69qT/snUf1oOpMt3FRiQ1FY9ddeZlULI8yQ1VravMZMCsUk8ZO6JUUy83lhb
            XeTsEA8ykO3wVlodFc8VBQQcDg4eixEhaHeDZ5Z71HCfLIwPsI2jXRiTpx9BYZVI
            mzn1wdWo7a3Z6rk3tVWQgflWa1GJO1m/immO72oyJTVVmQpNbPRSUnfo3bh1uC0g
            fUiYrMu46qEZay2DYHDqBnvO1g6kf95wUPlOU+PsWO/gfvY366ByT8hu1oc9vbSM
            EVTarbGBOcCJ2GIERs36HR/Zc0fv57Pa9iFLP+Dd4GnV1oHjyoHB2L7VrIK2y42k
            wlb7w8xlAgMBAAECggEAZpmuOIe0WYe5It+JjsbmYHDBOLMsBzcRNamh2SXAI2Ns
            /2Ip25DuNRy8KdSPcN/87fk9KemMXEwNFa280gJkyGM1mfSvSdHMR2A+YxZzUS+R
            PVnecvlyV7+cXZtITjlKIxxciAFTTAhxRBIbjcqZMQ4GvYl0+Z7urP6hCrXfamYC
            fbqrZyoSO5EApKHH1jRYug+uiX3/GH/KRjXrgPhi7AoZeOFE26LSIYdUj5l1ZOqz
            HQU/f/MrvtJfzNV4UkyLDF4xoJNoR9FJghdSUm1aqirTpfNtY02zr+iBTx2u6pnq
            Zc0yJMntoorUCaFDiGywDjgjxEVjH11xgYzI/Z67AQKBgQDeN8Y8+X02k2sWDFRM
            HDHfQo+lMzAIc1CEbrT09d7/mf/E80Di79jlla4njyaPdxF8+lv5P/AjzFY6Yabn
            ej30xcy7ULQSOn1SaJztWc+GyJA5Al8tdkU22C/lLmwhII7u8AG1CT3rRuvo2IyR
            Bz08rjper4MIIV2bLzcWjg+D8QKBgQDSSuDwKf4+JPkkDmbD+X4kACm6/TOI/X4E
            jk9OY8G7vogUegsPoueMojMXhABNH3m48iHWElGLWVEx34womHMd5Kg0WbptfA35
            iRwHXNe83QplW+Qm+Emm8FwjZy2HGnQdyvYJHPqhojz8RjldWU/NmsaH+2Y5QEcM
            5yRgzCqztQKBgQDZaAvk39pnKLdeLNXUWMlaOo66+2eE/PzLdFxKRLrVq18W2z6i
            SDIV++kU/vKk7cMIWRDevHU/MM0z9RIL6gbvkQ2KPZzPMLYnh/3wISvuHA8uF2ny
            2oFA9SV2vYJArs/oaJvi+JXBt/NaLXLo+QIqefLVbDVwIOSpzPnCcV4woQKBgGzc
            QlFRC8IT1b0qj1xH3Vq5kuCvjmwN/ZxSJw/HaFTOLxeajftMM12D4br8pENIVfS9
            s3JkDT57wCJ3PNEPChihc8mV4YInw/w3VAEjuLKWInmR9iuOQGJg7vGMye60bQO3
            O2JAVCY0HCnkGHIQd9VRmBBwvdxJP/6X8ScCyeA5AoGAc1RtDS+V6JQGJM7qQs3P
            LFkmKrKUXr9ApL5kod2btWoORkeehhkW9c6xJBhowvkB/uCJDnha7/PXd9iRlB5/
            hl50Bap2YFltz6BlH6yVx5H5+Se9Q+7QUO01sYTRQHIkr3fD8JkLeIH2LJm5GpRZ
            nRZt1+BmxsIOVGWW33g/w88=
            -----END PRIVATE KEY-----
            """;

    @Test
    void preparesWsWithOriginalPathHeadersAndNegotiatedSubprotocol() {
        AtomicReference<String> uri = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> origin = new AtomicReference<>();
        AtomicReference<String> extensions = new AtomicReference<>();
        DisposableServer server = HttpServer.create()
                .host("127.0.0.1")
                .port(0)
                .handle((request, response) -> {
                    uri.set(request.uri());
                    authorization.set(request.requestHeaders().get(
                            "Authorization"
                    ));
                    origin.set(request.requestHeaders().get("Origin"));
                    extensions.set(request.requestHeaders().get(
                            "Sec-WebSocket-Extensions"
                    ));
                    return response.sendWebsocket(
                            (inbound, outbound) -> Mono.never(),
                            WebsocketServerSpec.builder()
                                    .protocols("realtime")
                                    .handlePing(true)
                                    .build()
                    );
                })
                .bindNow();
        ReactorNettyWebSocketUpstreamAdapter adapter =
                new ReactorNettyWebSocketUpstreamAdapter(
                        HttpClient.create()
                );
        try {
            GatewayWebSocketHandshakeResult result = adapter.prepare(
                    context(server.port(), false)
            ).block(Duration.ofSeconds(2));

            GatewayWebSocketHandshakeResult.Accepted accepted =
                    assertInstanceOf(
                            GatewayWebSocketHandshakeResult.Accepted.class,
                            result
                    );
            assertEquals(
                    "/v1/realtime?model=gpt%2Drealtime",
                    uri.get()
            );
            assertEquals("Bearer transparent", authorization.get());
            assertEquals("https://client.example", origin.get());
            assertNull(extensions.get());
            assertEquals(
                    "realtime",
                    accepted.session().selectedSubprotocol()
            );
            accepted.session().dispose();
        } finally {
            server.disposeNow();
        }
    }

    @Test
    void supportsWssWithAnExplicitTestTrustContext() throws Exception {
        SslContext serverSsl = SslContextBuilder.forServer(
                stream(TEST_CERTIFICATE),
                stream(TEST_PRIVATE_KEY)
        ).build();
        SslContext clientSsl = SslContextBuilder.forClient()
                .trustManager(stream(TEST_CERTIFICATE))
                .build();
        DisposableServer server = HttpServer.create()
                .host("127.0.0.1")
                .port(0)
                .secure(spec -> spec.sslContext(serverSsl))
                .handle((request, response) -> response.sendWebsocket(
                        (inbound, outbound) -> Mono.never()
                ))
                .bindNow();
        ReactorNettyWebSocketUpstreamAdapter adapter =
                new ReactorNettyWebSocketUpstreamAdapter(
                        HttpClient.create(),
                        clientSsl
                );
        try {
            GatewayWebSocketHandshakeResult result = adapter.prepare(
                    context(server.port(), true)
            ).block(Duration.ofSeconds(2));

            GatewayWebSocketHandshakeResult.Accepted accepted =
                    assertInstanceOf(
                            GatewayWebSocketHandshakeResult.Accepted.class,
                            result
                    );
            assertNull(accepted.session().selectedSubprotocol());
            assertFalse(accepted.session().upstream().disposed());
            accepted.session().dispose();
        } finally {
            server.disposeNow();
        }
    }

    @Test
    void bridgesRawBinaryFramesAcrossTheReactorNettyAdapter()
            throws Exception {
        DisposableServer server = HttpServer.create()
                .host("127.0.0.1")
                .port(0)
                .handle((request, response) -> response.sendWebsocket(
                        (inbound, outbound) -> outbound.sendObject(
                                inbound.receiveFrames().map(frame ->
                                        frame.retain()
                                )
                        ).then(),
                        WebsocketServerSpec.builder()
                                .protocols("realtime")
                                .handlePing(true)
                                .build()
                ))
                .bindNow();
        GatewayWebSocketProxy proxy = new GatewayWebSocketProxy(
                new ReactorNettyWebSocketUpstreamAdapter(
                        HttpClient.create()
                )
        );
        byte[] binary = new byte[]{0, (byte) 0xff, (byte) 0x80, 1};
        EchoPeer downstream = new EchoPeer(binary);
        Disposable bridge = null;
        try {
            GatewayWebSocketProxyContext context = context(
                    server.port(),
                    false
            );
            GatewayWebSocketHandshakeResult.Accepted accepted =
                    assertInstanceOf(
                            GatewayWebSocketHandshakeResult.Accepted.class,
                            proxy.prepare(context).block(
                                    Duration.ofSeconds(2)
                            )
                    );

            bridge = proxy.bridge(accepted.session(), downstream)
                    .subscribe();

            assertTrue(downstream.received.await(1, TimeUnit.SECONDS));
            org.junit.jupiter.api.Assertions.assertArrayEquals(
                    binary,
                    downstream.echoed.get()
            );
        } finally {
            if (bridge != null) {
                bridge.dispose();
            }
            server.disposeNow();
        }
        assertTrue(downstream.disposed);
    }

    @Test
    void returnsOrdinaryHttpRejectionWithoutCommittingClientHandshake() {
        DisposableServer server = HttpServer.create()
                .host("127.0.0.1")
                .port(0)
                .handle((request, response) -> response.status(401).send())
                .bindNow();
        ReactorNettyWebSocketUpstreamAdapter adapter =
                new ReactorNettyWebSocketUpstreamAdapter(
                        HttpClient.create()
                );
        GatewayWebSocketProxyContext context = context(
                server.port(),
                false
        );
        try {
            GatewayWebSocketHandshakeResult result = adapter.prepare(context)
                    .block(Duration.ofSeconds(2));

            GatewayWebSocketHandshakeResult.Rejected rejected =
                    assertInstanceOf(
                            GatewayWebSocketHandshakeResult.Rejected.class,
                            result
                    );
            assertEquals(401, rejected.httpStatus());
            assertEquals(
                    top.egon.cola.component.gateway.engine.transport
                            .GatewayCommitPoint.NEW,
                    context.commitGuard().current()
            );
        } finally {
            server.disposeNow();
        }
    }

    private GatewayWebSocketProxyContext context(
            int port,
            boolean secure) {
        return new GatewayWebSocketProxyContext(
                provider(port, secure),
                "/v1/realtime?model=gpt%2Drealtime",
                Map.of(
                        "Authorization",
                        List.of("Bearer transparent"),
                        "Origin",
                        List.of("https://client.example"),
                        "Sec-WebSocket-Extensions",
                        List.of("permessage-deflate")
                ),
                List.of("realtime", "fallback"),
                policy(),
                GatewayCommitGuard.websocket(),
                GatewayWebSocketObserver.noop()
        );
    }

    private EffectiveGatewayTransportPolicy policy() {
        return new EffectiveGatewayTransportPolicy(
                GatewayRouteProfile.OPENAI_HTTP,
                GatewayTransportProtocol.WEBSOCKET,
                GatewayRequestBodyMode.STREAMING,
                GatewayTransportResponseMode.AUTO_STREAM,
                1024,
                OptionalLong.empty(),
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                Duration.ofSeconds(3),
                Optional.empty(),
                Optional.of(Duration.ofSeconds(5)),
                OptionalLong.of(64 * 1024),
                false,
                false,
                true
        );
    }

    private ProviderInstance provider(
            int port,
            boolean secure) {
        return new ProviderInstance(
                new ProviderServiceKey(
                        "test-biz",
                        "test-app",
                        "test",
                        "default",
                        ProviderProtocolType.HTTP,
                        "openai",
                        "default",
                        "v1",
                        secure ? "https" : "http"
                ),
                "provider-1",
                "lease-1",
                secure ? "localhost" : "127.0.0.1",
                port,
                secure,
                Map.of(),
                Instant.now().plusSeconds(60),
                ProviderRegistryState.REGISTERED,
                ProviderHealthState.HEALTHY,
                ProviderHealthState.HEALTHY
        );
    }

    private ByteArrayInputStream stream(String value) {
        return new ByteArrayInputStream(
                value.getBytes(StandardCharsets.US_ASCII)
        );
    }

    private static final class EchoPeer implements GatewayWebSocketPeer {

        private final byte[] outbound;

        private final AtomicReference<byte[]> echoed =
                new AtomicReference<>();

        private final CountDownLatch received = new CountDownLatch(1);

        private volatile boolean disposed;

        private EchoPeer(byte[] outbound) {
            this.outbound = outbound;
        }

        @Override
        public Flux<GatewayWebSocketFrame> receive() {
            DataBuffer payload = DefaultDataBufferFactory.sharedInstance
                    .wrap(outbound);
            return Flux.just(GatewayWebSocketFrame.data(
                    GatewayWebSocketFrameType.BINARY,
                    true,
                    payload
            )).concatWith(Flux.never());
        }

        @Override
        public Mono<Void> send(Flux<GatewayWebSocketFrame> frames) {
            return frames.doOnNext(frame -> {
                echoed.set(frame.payloadBytes());
                frame.release();
                received.countDown();
            }).then();
        }

        @Override
        public Mono<Void> sendClose(GatewayWebSocketCloseStatus status) {
            return Mono.empty();
        }

        @Override
        public void dispose() {
            disposed = true;
        }

        @Override
        public boolean disposed() {
            return disposed;
        }
    }
}
