package top.egon.cola.component.gateway.engine.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GatewayBodySizeLimiterTest {

    @Test
    void rejectsRequestAcrossMultipleChunks() {
        GatewayBodySizeLimiter limiter = new GatewayBodySizeLimiter();
        ByteBuf first = pooledBytes("123");
        ByteBuf second = pooledBytes("456");
        ByteBuf discarded = pooledBytes("789");

        GatewayRequestBodyTooLargeException failure = assertThrows(
                GatewayRequestBodyTooLargeException.class,
                () -> limiter.aggregateRequest(
                        Flux.fromIterable(List.of(
                                wrap(first),
                                wrap(second),
                                wrap(discarded)
                        )),
                        5
                ).block()
        );

        assertEquals("GATEWAY_REQUEST_BODY_TOO_LARGE", failure.code());
        assertEquals(0, first.refCnt());
        assertEquals(0, second.refCnt());
        assertEquals(0, discarded.refCnt());
    }

    @Test
    void rejectsResponseAcrossMultipleChunksWithoutAggregating() {
        GatewayBodySizeLimiter limiter = new GatewayBodySizeLimiter();
        ByteBuf first = pooledBytes("123");
        ByteBuf second = pooledBytes("456");
        ByteBuf discarded = pooledBytes("789");
        GatewayOutboundHttpResponse response =
                new GatewayOutboundHttpResponse(
                        200,
                        Map.of(),
                        Flux.fromIterable(List.of(
                                wrap(first),
                                wrap(second),
                                wrap(discarded)
                        ))
                );

        GatewayResponseBodyTooLargeException failure = assertThrows(
                GatewayResponseBodyTooLargeException.class,
                () -> GatewayDataBufferTestSupport.join(
                        limiter.limitResponse(response, 5).body(),
                        5
                )
        );

        assertEquals("GATEWAY_RESPONSE_BODY_TOO_LARGE", failure.code());
        assertEquals(0, first.refCnt());
        assertEquals(0, second.refCnt());
        assertEquals(0, discarded.refCnt());
    }

    @Test
    void releasesDiscardedResponseBuffersWhenDownstreamCancels() {
        GatewayBodySizeLimiter limiter = new GatewayBodySizeLimiter();
        ByteBuf first = pooledBytes("123");
        ByteBuf discarded = pooledBytes("456");
        GatewayOutboundHttpResponse response =
                new GatewayOutboundHttpResponse(
                        200,
                        Map.of(),
                        Flux.fromIterable(List.of(
                                wrap(first),
                                wrap(discarded)
                        ))
                );

        assertEquals(
                "123",
                GatewayDataBufferTestSupport.joinUtf8(
                        limiter.limitResponse(response, 10)
                                .body()
                                .take(1),
                        10
                )
        );

        assertEquals(0, first.refCnt());
        assertEquals(0, discarded.refCnt());
    }

    @Test
    void rejectsDeclaredContentLengthBeforeBodySubscription() {
        GatewayBodySizeLimiter limiter = new GatewayBodySizeLimiter();
        AtomicInteger abandonments = new AtomicInteger();
        GatewayOutboundHttpResponse response =
                new GatewayOutboundHttpResponse(
                        200,
                        Map.of("content-length", List.of("6")),
                        Flux.never()
                ).onAbandon(abandonments::incrementAndGet);

        assertThrows(
                GatewayResponseBodyTooLargeException.class,
                () -> limiter.limitResponse(response, 5)
        );
        response.abandon();

        assertEquals(1, abandonments.get());
    }

    @Test
    void preservesAbandonmentWhenDecoratingResponseBody() {
        GatewayBodySizeLimiter limiter = new GatewayBodySizeLimiter();
        AtomicInteger abandonments = new AtomicInteger();
        GatewayOutboundHttpResponse response =
                new GatewayOutboundHttpResponse(
                        200,
                        Map.of(),
                        Flux.never()
                ).onAbandon(abandonments::incrementAndGet);

        GatewayOutboundHttpResponse limited =
                limiter.limitResponse(response, 5);
        limited.abandon();
        limited.abandon();
        response.abandon();

        assertEquals(1, abandonments.get());
    }

    private ByteBuf pooledBytes(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        return PooledByteBufAllocator.DEFAULT.buffer(bytes.length)
                .writeBytes(bytes);
    }

    private DataBuffer wrap(ByteBuf buffer) {
        return new NettyDataBufferFactory(
                PooledByteBufAllocator.DEFAULT
        ).wrap(buffer);
    }
}
