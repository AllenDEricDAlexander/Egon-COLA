package top.egon.cola.component.gateway.engine.http.common.buffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayDataBufferPipelineTest {

    private final NettyDataBufferFactory bufferFactory =
            new NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT);

    @Test
    void observesEveryBufferWithOneSourceSubscriptionOnNormalCompletion() {
        OwnedBuffer first = buffer(1, 2, 3);
        OwnedBuffer second = buffer(4, 5);
        AtomicInteger subscriptions = new AtomicInteger();
        List<Long> observedBytes = new ArrayList<>();
        Flux<DataBuffer> source = Flux.defer(() -> {
            subscriptions.incrementAndGet();
            return Flux.<DataBuffer>just(
                    first.dataBuffer(),
                    second.dataBuffer()
            );
        });
        Flux<DataBuffer> body = GatewayDataBufferPipeline
                .releaseOnDiscardOrCancel(
                        GatewayDataBufferPipeline.observeBytes(
                                source,
                                observedBytes::add
                        )
                );

        StepVerifier.create(body)
                .assertNext(this::releaseTransferred)
                .assertNext(this::releaseTransferred)
                .verifyComplete();

        assertEquals(1, subscriptions.get());
        assertEquals(List.of(3L, 2L), observedBytes);
        assertReleased(first, second);
    }

    @Test
    void limitCancelsAtFirstOverflowAndReleasesUntransferredBuffers() {
        OwnedBuffer first = buffer(1, 2, 3);
        OwnedBuffer overflow = buffer(4, 5, 6);
        OwnedBuffer discarded = buffer(7, 8, 9);
        AtomicBoolean cancelled = new AtomicBoolean();
        RuntimeException failure = new RuntimeException("too large");
        Flux<DataBuffer> source = Flux.<DataBuffer>fromIterable(List.of(
                        first.dataBuffer(),
                        overflow.dataBuffer(),
                        discarded.dataBuffer()
                ))
                .doOnCancel(() -> cancelled.set(true));
        Flux<DataBuffer> body = GatewayDataBufferPipeline
                .releaseOnDiscardOrCancel(
                        GatewayDataBufferPipeline.limitBytes(
                                source,
                                5,
                                () -> failure
                        )
                );

        StepVerifier.create(body)
                .assertNext(this::releaseTransferred)
                .expectErrorSatisfies(error -> assertSame(failure, error))
                .verify();

        assertTrue(cancelled.get());
        assertReleased(first, overflow, discarded);
    }

    @Test
    void upstreamErrorIsPreservedAfterTransferredBufferIsReleased() {
        OwnedBuffer first = buffer(1, 2, 3);
        RuntimeException failure = new RuntimeException("upstream");
        Flux<DataBuffer> source = Flux.concat(
                Flux.<DataBuffer>just(first.dataBuffer()),
                Flux.error(failure)
        );
        Flux<DataBuffer> body = GatewayDataBufferPipeline
                .releaseOnDiscardOrCancel(source);

        StepVerifier.create(body)
                .assertNext(this::releaseTransferred)
                .expectErrorSatisfies(error -> assertSame(failure, error))
                .verify();

        assertReleased(first);
    }

    @Test
    void downstreamCancellationReleasesOnlyBuffersNotTransferred() {
        OwnedBuffer transferred = buffer(1, 2, 3);
        OwnedBuffer firstDiscarded = buffer(4, 5, 6);
        OwnedBuffer secondDiscarded = buffer(7, 8, 9);
        AtomicBoolean cancelled = new AtomicBoolean();
        Flux<DataBuffer> source = Flux.<DataBuffer>fromIterable(List.of(
                        transferred.dataBuffer(),
                        firstDiscarded.dataBuffer(),
                        secondDiscarded.dataBuffer()
                ))
                .doOnCancel(() -> cancelled.set(true));
        Flux<DataBuffer> body = GatewayDataBufferPipeline
                .releaseOnDiscardOrCancel(source);

        StepVerifier.create(body, 1)
                .assertNext(this::releaseTransferred)
                .thenCancel()
                .verify();

        assertTrue(cancelled.get());
        assertReleased(transferred, firstDiscarded, secondDiscarded);
    }

    @Test
    void responseBodyDoesNotAllocateWhenItIsNeverSubscribed() {
        AtomicReference<OwnedBuffer> allocated = new AtomicReference<>();
        Flux<DataBuffer> lazyBody = Flux.defer(() -> {
            OwnedBuffer buffer = buffer(1, 2, 3);
            allocated.set(buffer);
            return Flux.<DataBuffer>just(buffer.dataBuffer());
        });
        TestResponse response = new TestResponse(
                GatewayDataBufferPipeline.releaseOnDiscardOrCancel(lazyBody)
        );

        assertNull(allocated.get());
        assertTrue(response.body() != null);
    }

    @Test
    void discardHookReleasesFilteredBuffer() {
        OwnedBuffer discarded = buffer(1, 2, 3);
        Flux<DataBuffer> source = Flux.<DataBuffer>just(
                        discarded.dataBuffer()
                )
                .filter(ignored -> false);

        StepVerifier.create(
                        GatewayDataBufferPipeline.releaseOnDiscardOrCancel(source)
                )
                .verifyComplete();

        assertReleased(discarded);
    }

    @Test
    void observerFailureDoesNotInterruptBodyTransfer() {
        OwnedBuffer buffer = buffer(1, 2, 3);
        Flux<DataBuffer> body = GatewayDataBufferPipeline
                .releaseOnDiscardOrCancel(
                        GatewayDataBufferPipeline.observeBytes(
                                Flux.<DataBuffer>just(buffer.dataBuffer()),
                                ignored -> {
                                    throw new IllegalStateException("observer");
                                }
                        )
                );

        StepVerifier.create(body)
                .assertNext(this::releaseTransferred)
                .verifyComplete();

        assertReleased(buffer);
    }

    @Test
    void bodySampleIsBoundedAcrossBuffersWithoutMovingReadPositions() {
        OwnedBuffer first = buffer(1, 2, 3, 4);
        OwnedBuffer second = buffer(5, 6, 7, 8);
        List<byte[]> samples = new ArrayList<>();
        Flux<DataBuffer> body = GatewayDataBufferPipeline
                .releaseOnDiscardOrCancel(
                        GatewayDataBufferPipeline.sampleBodyWhenEnabled(
                                Flux.<DataBuffer>just(
                                        first.dataBuffer(),
                                        second.dataBuffer()
                                ),
                                true,
                                6,
                                samples::add
                        )
                );

        StepVerifier.create(body)
                .assertNext(this::releaseTransferred)
                .assertNext(this::releaseTransferred)
                .verifyComplete();

        assertEquals(2, samples.size());
        assertEquals(List.of((byte) 1, (byte) 2, (byte) 3, (byte) 4),
                toList(samples.get(0)));
        assertEquals(List.of((byte) 5, (byte) 6),
                toList(samples.get(1)));
        assertEquals(0, first.dataBuffer().readPosition());
        assertEquals(0, second.dataBuffer().readPosition());
        assertReleased(first, second);
    }

    @Test
    void externalIdleSignalFailsAndCancelsTheBody() {
        OwnedBuffer first = buffer(1, 2, 3);
        AtomicBoolean cancelled = new AtomicBoolean();
        Sinks.One<String> timeoutSignal = Sinks.one();
        RuntimeException failure = new RuntimeException("idle");
        Flux<DataBuffer> source = Flux.concat(
                        Flux.<DataBuffer>just(first.dataBuffer()),
                        Flux.never()
                )
                .doOnCancel(() -> cancelled.set(true));
        Flux<DataBuffer> body = GatewayDataBufferPipeline
                .releaseOnDiscardOrCancel(
                        GatewayDataBufferPipeline.enforceIdleTimeout(
                                source,
                                timeoutSignal.asMono(),
                                () -> failure
                        )
                );

        StepVerifier.create(body)
                .assertNext(this::releaseTransferred)
                .then(() -> timeoutSignal.tryEmitValue("idle"))
                .expectErrorSatisfies(error -> assertSame(failure, error))
                .verify();

        assertTrue(cancelled.get());
        assertReleased(first);
    }

    @Test
    void externalIdleSignalErrorCancelsBodyAndReleasesPendingBuffer() {
        OwnedBuffer pending = buffer(1, 2, 3);
        AtomicBoolean cancelled = new AtomicBoolean();
        Sinks.Empty<Void> timeoutSignal = Sinks.empty();
        RuntimeException failure = new RuntimeException("idle signal");
        Flux<DataBuffer> source = Flux.concat(
                        Flux.<DataBuffer>just(pending.dataBuffer()),
                        Flux.never()
                )
                .doOnCancel(() -> cancelled.set(true));
        Flux<DataBuffer> body = GatewayDataBufferPipeline
                .releaseOnDiscardOrCancel(
                        GatewayDataBufferPipeline.enforceIdleTimeout(
                                source,
                                timeoutSignal.asMono(),
                                () -> new IllegalStateException(
                                        "fallback failure"
                                )
                        )
                );

        try {
            StepVerifier.create(body, 0)
                    .then(() -> assertSame(
                            Sinks.EmitResult.OK,
                            timeoutSignal.tryEmitError(failure)
                    ))
                    .expectErrorSatisfies(error -> assertSame(
                            failure,
                            error
                    ))
                    .verify();

            assertTrue(cancelled.get());
            assertReleased(pending);
        } finally {
            releaseRemaining(pending);
        }
    }

    private OwnedBuffer buffer(int... values) {
        NettyDataBuffer dataBuffer = bufferFactory.allocateBuffer(values.length);
        for (int value : values) {
            dataBuffer.write((byte) value);
        }
        return new OwnedBuffer(dataBuffer, dataBuffer.getNativeBuffer());
    }

    private void releaseTransferred(DataBuffer buffer) {
        assertTrue(GatewayDataBufferOwnership.release(buffer));
    }

    private void assertReleased(OwnedBuffer... buffers) {
        for (OwnedBuffer buffer : buffers) {
            assertEquals(0, buffer.nativeBuffer().refCnt());
        }
    }

    private void releaseRemaining(OwnedBuffer... buffers) {
        for (OwnedBuffer buffer : buffers) {
            while (buffer.nativeBuffer().refCnt() > 0) {
                buffer.nativeBuffer().release();
            }
        }
    }

    private List<Byte> toList(byte[] bytes) {
        List<Byte> values = new ArrayList<>(bytes.length);
        for (byte value : bytes) {
            values.add(value);
        }
        return values;
    }

    private record OwnedBuffer(
            NettyDataBuffer dataBuffer,
            ByteBuf nativeBuffer) {
    }

    private record TestResponse(Flux<DataBuffer> body) {
    }
}
