package top.egon.cola.component.gateway.engine.http.logging;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayBodyLogTapTest {

    @Test
    void disabledTapReturnsTheOriginalPublisherWithoutObservingIt() {
        Flux<DataBuffer> source = Flux.defer(() -> Flux.just(buffer("json")));
        AtomicInteger observed = new AtomicInteger();

        Flux<DataBuffer> tapped = GatewayBodyLogTap.tap(
                source,
                false,
                "application/json",
                GatewayBodyLogDirection.REQUEST,
                GatewayBodyLogTap.DEFAULT_SAMPLE_BYTES,
                event -> observed.incrementAndGet()
        );

        assertSame(source, tapped);
        assertEquals(0, observed.get());
    }

    @Test
    void samplesJsonWithinTheHardLimitWithoutConsumingTheBuffer() {
        List<GatewayBodyLogEvent> events = new ArrayList<>();

        StepVerifier.create(GatewayBodyLogTap.tap(
                        Flux.just(buffer("{\"input\":\"unchanged\"}")),
                        true,
                        "application/json; charset=utf-8",
                        GatewayBodyLogDirection.REQUEST,
                        GatewayBodyLogTap.MAX_SAMPLE_BYTES + 1,
                        events::add
                ))
                .assertNext(value -> {
                    byte[] bytes = new byte[value.readableByteCount()];
                    value.read(bytes);
                    DataBufferUtils.release(value);
                    assertArrayEquals(
                            "{\"input\":\"unchanged\"}"
                                    .getBytes(StandardCharsets.UTF_8),
                            bytes
                    );
                })
                .verifyComplete();

        assertEquals(1, events.size());
        assertFalse(events.getFirst().metadataOnly());
        assertEquals(21, events.getFirst().totalBytes());
        assertTrue(events.getFirst().sample().length <= 64 * 1024);
    }

    @Test
    void logsOnlyMetadataForMultipartMediaBinaryAndWebsocketPayloads() {
        for (String contentType : List.of(
                "multipart/form-data; boundary=x",
                "image/png",
                "audio/mpeg",
                "application/octet-stream"
        )) {
            List<GatewayBodyLogEvent> events = new ArrayList<>();
            StepVerifier.create(GatewayBodyLogTap.tap(
                            Flux.just(buffer("secret")),
                            true,
                            contentType,
                            GatewayBodyLogDirection.RESPONSE,
                            8192,
                            events::add
                    ))
                    .thenConsumeWhile(value -> {
                        DataBufferUtils.release(value);
                        return true;
                    })
                    .verifyComplete();
            assertTrue(events.getFirst().metadataOnly());
            assertEquals(0, events.getFirst().sample().length);
        }

        List<GatewayBodyLogEvent> websocket = new ArrayList<>();
        StepVerifier.create(GatewayBodyLogTap.tap(
                        Flux.just(buffer("frame")),
                        true,
                        "text/plain",
                        GatewayBodyLogDirection.WEBSOCKET,
                        8192,
                        websocket::add
                ))
                .thenConsumeWhile(value -> {
                    DataBufferUtils.release(value);
                    return true;
                })
                .verifyComplete();
        assertTrue(websocket.getFirst().metadataOnly());
    }

    @Test
    void filtersCredentialHeaderNamesAndIsolatesObserverFailures() {
        assertEquals(
                List.of("Content-Type", "Traceparent"),
                GatewayBodyLogTap.safeHeaderNames(List.of(
                        "Authorization",
                        "Cookie",
                        "OpenAI-Api-Key",
                        "Content-Type",
                        "Traceparent"
                ))
        );

        StepVerifier.create(GatewayBodyLogTap.tap(
                        Flux.just(buffer("ok")),
                        true,
                        "application/json",
                        GatewayBodyLogDirection.RESPONSE,
                        8192,
                        event -> {
                            throw new IllegalStateException("log sink down");
                        }
                ))
                .thenConsumeWhile(value -> {
                    DataBufferUtils.release(value);
                    return true;
                })
                .verifyComplete();
    }

    private DataBuffer buffer(String value) {
        return DefaultDataBufferFactory.sharedInstance.wrap(
                value.getBytes(StandardCharsets.UTF_8)
        );
    }
}
