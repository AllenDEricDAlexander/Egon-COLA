package top.egon.cola.component.gateway.engine.transport;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ResourceLeakDetector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import top.egon.cola.component.gateway.engine.http.buffer.GatewayDataBufferPipeline;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayDataBufferLeakTest {

    private final NettyDataBufferFactory buffers =
            new NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT);

    private ResourceLeakDetector.Level previousLevel;

    @BeforeEach
    void enableParanoidLeakDetection() {
        previousLevel = ResourceLeakDetector.getLevel();
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
    }

    @AfterEach
    void restoreLeakDetection() {
        ResourceLeakDetector.setLevel(previousLevel);
    }

    @Test
    void normalCompletionReleasesEveryTransferredPooledBuffer() {
        OwnedBuffer first = buffer(8);
        OwnedBuffer second = buffer(16);

        StepVerifier.create(GatewayDataBufferPipeline
                        .releaseOnDiscardOrCancel(Flux.<DataBuffer>just(
                                first.dataBuffer(),
                                second.dataBuffer()
                        )))
                .assertNext(this::release)
                .assertNext(this::release)
                .verifyComplete();

        assertReleased(first, second);
    }

    @Test
    void oversizeFailureReleasesOverflowAndDiscardedBuffers() {
        OwnedBuffer transferred = buffer(8);
        OwnedBuffer overflow = buffer(8);
        OwnedBuffer discarded = buffer(8);
        RuntimeException failure = new RuntimeException("too large");

        StepVerifier.create(GatewayDataBufferPipeline
                        .releaseOnDiscardOrCancel(
                                GatewayDataBufferPipeline.limitBytes(
                                        Flux.<DataBuffer>fromIterable(List.of(
                                                transferred.dataBuffer(),
                                                overflow.dataBuffer(),
                                                discarded.dataBuffer()
                                        )),
                                        10,
                                        () -> failure
                                )
                        ))
                .assertNext(this::release)
                .expectErrorMatches(error -> error == failure)
                .verify();

        assertReleased(transferred, overflow, discarded);
    }

    @Test
    void downstreamCancellationReleasesEveryUntransferredBuffer() {
        OwnedBuffer transferred = buffer(8);
        OwnedBuffer discarded = buffer(8);

        StepVerifier.create(
                        GatewayDataBufferPipeline.releaseOnDiscardOrCancel(
                                Flux.<DataBuffer>fromIterable(List.of(
                                        transferred.dataBuffer(),
                                        discarded.dataBuffer()
                                ))
                        ),
                        1
                )
                .assertNext(this::release)
                .thenCancel()
                .verify();

        assertReleased(transferred, discarded);
    }

    @Test
    void idleTimeoutReleasesTheLastTransferredBufferAndCancelsSource() {
        OwnedBuffer transferred = buffer(8);

        StepVerifier.create(GatewayTransportTimeouts.responseIdle(
                        Flux.concat(
                                Flux.<DataBuffer>just(
                                        transferred.dataBuffer()
                                ),
                                Flux.never()
                        ),
                        Duration.ofMillis(30)
                ))
                .assertNext(this::release)
                .expectError(GatewayStreamIdleTimeoutException.class)
                .verify(Duration.ofSeconds(1));

        assertReleased(transferred);
    }

    private OwnedBuffer buffer(int length) {
        NettyDataBuffer buffer = buffers.allocateBuffer(length);
        for (int index = 0; index < length; index++) {
            buffer.write((byte) index);
        }
        return new OwnedBuffer(buffer, buffer.getNativeBuffer());
    }

    private void release(DataBuffer buffer) {
        assertTrue(DataBufferUtils.release(buffer));
    }

    private void assertReleased(OwnedBuffer... values) {
        for (OwnedBuffer value : values) {
            assertEquals(0, value.nativeBuffer().refCnt());
        }
    }

    private record OwnedBuffer(
            NettyDataBuffer dataBuffer,
            ByteBuf nativeBuffer
    ) {
    }
}
