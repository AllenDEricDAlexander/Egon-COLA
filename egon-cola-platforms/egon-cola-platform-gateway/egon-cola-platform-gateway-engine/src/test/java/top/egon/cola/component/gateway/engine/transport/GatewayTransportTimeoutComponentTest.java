package top.egon.cola.component.gateway.engine.transport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.engine.http.GatewayOutboundHttpResponse;
import top.egon.cola.component.gateway.engine.http.HttpUpstreamRequest;
import top.egon.cola.component.gateway.engine.http.ReactorNettyHttpUpstreamAdapter;
import top.egon.cola.component.gateway.engine.transport.fixture.StreamingHttpTestUpstream;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayTransportTimeoutComponentTest {

    private StreamingHttpTestUpstream upstream;

    private ReactorNettyHttpUpstreamAdapter adapter;

    @BeforeEach
    void setUp() {
        upstream = new StreamingHttpTestUpstream();
        adapter = new ReactorNettyHttpUpstreamAdapter(
                4,
                4,
                Duration.ofSeconds(5)
        );
    }

    @AfterEach
    void tearDown() {
        adapter.close();
        upstream.close();
    }

    @Test
    void responseHeaderTimeoutClosesTheAttempt() {
        long started = System.nanoTime();

        assertThrows(RuntimeException.class, () -> invoke(
                "/slow-headers",
                Flux.empty(),
                Duration.ofSeconds(1),
                Duration.ofMillis(80),
                Duration.ofSeconds(1),
                Optional.of(Duration.ofSeconds(2))
        ).block(Duration.ofSeconds(2)));

        long elapsedMillis = Duration.ofNanos(
                System.nanoTime() - started
        ).toMillis();
        assertTrue(elapsedMillis < 1000, "elapsedMillis=" + elapsedMillis);
        assertEquals(1, upstream.invocations("/slow-headers"));
    }

    @Test
    void requestIdleTimeoutInterruptsAStalledStreamingUpload() {
        upstream.expectUpload(0, 0);
        Flux<DataBuffer> body = Flux.concat(
                Flux.just(buffer("first")),
                Mono.delay(Duration.ofMillis(250)).map(ignored ->
                        buffer("second"))
        );

        RuntimeException failure = assertThrows(
                RuntimeException.class,
                () -> invoke(
                        "/upload",
                        body,
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1),
                        Duration.ofMillis(60),
                        Optional.of(Duration.ofSeconds(2))
                ).block(Duration.ofSeconds(2))
        );

        GatewayStreamIdleTimeoutException idle = assertInstanceOf(
                GatewayStreamIdleTimeoutException.class,
                root(failure)
        );
        assertEquals(GatewayStreamDirection.REQUEST, idle.direction());
        assertEquals(1, upstream.invocations("/upload"));
    }

    @Test
    void responseIdleTimeoutFiresAfterTheFirstBodyChunk() {
        GatewayOutboundHttpResponse response = invoke(
                "/idle-response",
                Flux.empty(),
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                Duration.ofMillis(60),
                Optional.of(Duration.ofSeconds(2))
        ).block(Duration.ofSeconds(2));

        RuntimeException failure = assertThrows(
                RuntimeException.class,
                () -> consume(response.body())
        );
        GatewayStreamIdleTimeoutException idle = assertInstanceOf(
                GatewayStreamIdleTimeoutException.class,
                root(failure)
        );
        assertEquals(GatewayStreamDirection.RESPONSE, idle.direction());
    }

    @Test
    void totalTimeoutEndsAContinuouslyActiveResponse() {
        GatewayOutboundHttpResponse response = invoke(
                "/total",
                Flux.empty(),
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                Optional.of(Duration.ofMillis(120))
        ).block(Duration.ofSeconds(2));

        RuntimeException failure = assertThrows(
                RuntimeException.class,
                () -> consume(response.body())
        );
        assertInstanceOf(GatewayTotalTimeoutException.class, root(failure));
        assertEquals(1, upstream.invocations("/total"));
    }

    @Test
    void connectTimeoutHasItsOwnFailureCategory() {
        RuntimeException failure = assertThrows(
                RuntimeException.class,
                () -> GatewayTransportTimeouts.connect(
                        Mono.never(),
                        Duration.ofMillis(30)
                ).block(Duration.ofSeconds(1))
        );

        assertInstanceOf(GatewayConnectTimeoutException.class, root(failure));
    }

    private Mono<GatewayOutboundHttpResponse> invoke(
            String path,
            Flux<DataBuffer> body,
            Duration connect,
            Duration responseHeaders,
            Duration idle,
            Optional<Duration> total) {
        return adapter.invoke(new HttpUpstreamRequest(
                upstream.provider(),
                "/upload".equals(path) ? "POST" : "GET",
                path,
                Map.of("Content-Type", List.of("application/octet-stream")),
                body,
                connect,
                responseHeaders,
                idle,
                total,
                false
        ));
    }

    private DataBuffer buffer(String value) {
        return DefaultDataBufferFactory.sharedInstance.wrap(
                value.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
    }

    private void consume(Flux<DataBuffer> body) {
        body.doOnNext(DataBufferUtils::release)
                .blockLast(Duration.ofSeconds(2));
    }

    private Throwable root(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
