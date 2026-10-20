package top.egon.cola.component.yuheng.engine.http.service;

import top.egon.cola.component.yuheng.runtime.http.service.ReactorNettyHttpUpstreamAdapter;

import top.egon.cola.component.yuheng.runtime.http.service.GatewayOutboundHttpResponse;
import top.egon.cola.component.yuheng.runtime.http.domain.HttpUpstreamRequest;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import top.egon.cola.component.yuheng.core.provider.ProviderHealthState;
import top.egon.cola.component.yuheng.core.provider.ProviderInstance;
import top.egon.cola.component.yuheng.core.provider.ProviderProtocolType;
import top.egon.cola.component.yuheng.core.provider.ProviderRegistryState;
import top.egon.cola.component.yuheng.core.provider.ProviderServiceKey;

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

    @org.junit.jupiter.api.io.TempDir
    java.nio.file.Path tlsDirectory;

    @Test
    void forwardsWithConfiguredClientIdentityAndRejectsAnUnmatchedServerHostname() throws Exception {
        var certificate = tlsDirectory.resolve("test-certificate.pem");
        var key = tlsDirectory.resolve("test-private-key.pem");
        java.nio.file.Files.writeString(certificate, TEST_CERTIFICATE);
        java.nio.file.Files.writeString(key, TEST_PRIVATE_KEY);
        var serverTls = io.netty.handler.ssl.SslContextBuilder.forServer(certificate.toFile(), key.toFile())
                .sslProvider(io.netty.handler.ssl.SslProvider.JDK)
                .trustManager(certificate.toFile()).clientAuth(io.netty.handler.ssl.ClientAuth.REQUIRE).build();
        DisposableServer server = HttpServer.create().host("127.0.0.1").port(0)
                .secure(spec -> spec.sslContext(serverTls))
                .handle((request, response) -> response.sendString(reactor.core.publisher.Mono.just("mtls-ok")))
                .bindNow();
        var properties = new top.egon.cola.component.yuheng.engine.common.config.GatewayEngineRuntimeProperties();
        var tls = new top.egon.cola.component.yuheng.engine.common.config.GatewayEngineRuntimeProperties.Tls();
        tls.setEnabled(true);
        tls.setCertificateChainPath(certificate.toString());
        tls.setPrivateKeyPath(key.toString());
        tls.setTrustCertificateCollectionPath(certificate.toString());
        tls.setClientCertificateRequired(true);
        properties.getHttp().setUpstreamTls(tls);
        try (var adapter = new top.egon.cola.component.yuheng.engine.bootstrap.config.GatewayEngineConfiguration()
                .gatewayHttpUpstreamAdapter(properties)) {
            GatewayOutboundHttpResponse response = adapter.invoke(new HttpUpstreamRequest(
                    provider(server.port(), true, "localhost"), "GET", "/health", Map.of(), Flux.empty(),
                    Duration.ofSeconds(3))).block(Duration.ofSeconds(5));
            assertEquals(200, response.status());
            assertEquals("mtls-ok", GatewayDataBufferTestSupport.joinUtf8(response.body(), 64));

            RuntimeException mismatch = assertThrows(RuntimeException.class, () -> adapter.invoke(new HttpUpstreamRequest(
                    provider(server.port(), true, "127.0.0.1"), "GET", "/health", Map.of(), Flux.empty(),
                    Duration.ofSeconds(3))).block(Duration.ofSeconds(5)));
            assertTrueCauseContainsHandshakeFailure(mismatch);
        } finally {
            server.disposeNow();
        }
    }

    private static void assertTrueCauseContainsHandshakeFailure(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof javax.net.ssl.SSLHandshakeException) return;
        }
        throw new AssertionError("Expected TLS hostname validation to reject the server", failure);
    }

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
        return provider(port, false, "127.0.0.1");
    }

    private ProviderInstance provider(int port, boolean secure, String host) {
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
                        secure ? "https" : "http"
                ),
                "provider-a",
                "lease-a",
                host,
                port,
                secure,
                Map.of(),
                Instant.now().plusSeconds(30),
                ProviderRegistryState.REGISTERED,
                ProviderHealthState.HEALTHY,
                ProviderHealthState.HEALTHY
        );
    }

    // Public, disposable TLS fixtures shared with the existing WebSocket transport tests.
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
}
