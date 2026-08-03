package top.egon.cola.component.gateway.engine.transport;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import top.egon.cola.component.gateway.contract.rule.GatewayRequestBodyMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteProfile;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportResponseMode;
import top.egon.cola.component.gateway.core.transport.EffectiveGatewayTransportPolicy;
import top.egon.cola.component.gateway.engine.http.GatewayHttpEngineProperties;
import top.egon.cola.component.gateway.engine.http.GatewayHttpServer;
import top.egon.cola.component.gateway.engine.http.GatewayRequestBodyTooLargeException;
import top.egon.cola.component.gateway.engine.http.ReactorNettyHttpUpstreamAdapter;
import top.egon.cola.component.gateway.engine.http.proxy.GatewayHttpProxyContext;
import top.egon.cola.component.gateway.engine.http.proxy.StreamingHttpProxyStrategy;
import top.egon.cola.component.gateway.engine.transport.fixture.StreamingHttpTestUpstream;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayCancellationPropagationTest {

    @Test
    void downstreamCancellationClosesTheUpstreamResponseWithinOneSecond()
            throws Exception {
        try (StreamingHttpTestUpstream upstream =
                     new StreamingHttpTestUpstream();
             ReactorNettyHttpUpstreamAdapter adapter =
                     new ReactorNettyHttpUpstreamAdapter(
                             4,
                             4,
                             Duration.ofSeconds(5)
                     )) {
            StreamingHttpProxyStrategy strategy =
                    new StreamingHttpProxyStrategy();
            EffectiveGatewayTransportPolicy policy = policy(
                    1024 * 1024,
                    Duration.ofSeconds(2),
                    Optional.of(Duration.ofSeconds(5))
            );
            GatewayHttpServer gateway = new GatewayHttpServer(
                    properties(),
                    (zone, request) -> strategy.proxy(
                            new GatewayHttpProxyContext(
                                    adapter,
                                    upstream.provider(),
                                    request.method(),
                                    request.uri(),
                                    request.headers(),
                                    request.body(),
                                    policy
                            )
                    )
            );
            gateway.start();
            try {
                HttpClient.create()
                        .get()
                        .uri("http://127.0.0.1:"
                                + gateway.publicPort()
                                + "/cancel")
                        .response((response, body) -> body.take(1).then())
                        .then()
                        .block(Duration.ofSeconds(2));

                assertTrue(upstream.cancelObserved().await(
                        1,
                        TimeUnit.SECONDS
                ));
                assertEquals(1, upstream.invocations("/cancel"));
            } finally {
                gateway.close();
            }
        }
    }

    @Test
    void chunkedOversizeCancelsTheUploadAfterOneUpstreamInvocation()
            throws Exception {
        try (StreamingHttpTestUpstream upstream =
                     new StreamingHttpTestUpstream();
             ReactorNettyHttpUpstreamAdapter adapter =
                     new ReactorNettyHttpUpstreamAdapter(
                             4,
                             4,
                             Duration.ofSeconds(5)
                     )) {
            upstream.expectUpload(0, 0);
            Flux<DataBuffer> body = Flux.range(0, 8)
                    .map(index -> DefaultDataBufferFactory.sharedInstance.wrap(
                            new byte[4096]
                    ));

            RuntimeException failure = assertThrows(
                    RuntimeException.class,
                    () -> new StreamingHttpProxyStrategy().proxy(
                            new GatewayHttpProxyContext(
                                    adapter,
                                    upstream.provider(),
                                    "POST",
                                    "/upload",
                                    Map.of(
                                            "Content-Type",
                                            List.of("application/octet-stream")
                                    ),
                                    body,
                                    policy(
                                            10 * 1024,
                                            Duration.ofSeconds(2),
                                            Optional.of(Duration.ofSeconds(5))
                                    )
                            )
                    ).block(Duration.ofSeconds(2))
            );

            assertInstanceOf(
                    GatewayRequestBodyTooLargeException.class,
                    root(failure)
            );
            assertTrue(upstream.awaitInvocation(
                    "/upload",
                    1,
                    TimeUnit.SECONDS
            ));
            assertEquals(1, upstream.invocations("/upload"));
        }
    }

    private EffectiveGatewayTransportPolicy policy(
            long maxRequestBytes,
            Duration streamIdle,
            Optional<Duration> total) {
        return new EffectiveGatewayTransportPolicy(
                GatewayRouteProfile.OPENAI_HTTP,
                GatewayTransportProtocol.HTTP,
                GatewayRequestBodyMode.STREAMING,
                GatewayTransportResponseMode.AUTO_STREAM,
                maxRequestBytes,
                OptionalLong.empty(),
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                streamIdle,
                total,
                Optional.empty(),
                OptionalLong.empty(),
                false,
                false,
                true
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
                Duration.ofSeconds(2),
                4,
                4
        );
    }

    private Throwable root(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
