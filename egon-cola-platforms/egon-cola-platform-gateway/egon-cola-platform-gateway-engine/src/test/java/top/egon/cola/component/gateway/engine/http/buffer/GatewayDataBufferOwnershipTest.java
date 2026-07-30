package top.egon.cola.component.gateway.engine.http.buffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.AbstractByteBufAllocator;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferWrapper;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.core.io.buffer.PooledDataBuffer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GatewayDataBufferOwnershipTest {

    private final NettyDataBufferFactory bufferFactory =
            new NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT);

    @Test
    void retainAndWrapAddsExactlyOneOwnedReference() {
        ByteBuf nativeBuffer = PooledByteBufAllocator.DEFAULT.buffer();
        nativeBuffer.writeBytes(new byte[]{1, 2, 3});

        try {
            DataBuffer owned = GatewayDataBufferOwnership.retainAndWrap(
                    bufferFactory,
                    nativeBuffer
            );

            assertEquals(2, nativeBuffer.refCnt());
            assertFalse(nativeBuffer.release());
            assertEquals(1, nativeBuffer.refCnt());
            assertTrue(GatewayDataBufferOwnership.release(owned));
            assertEquals(0, nativeBuffer.refCnt());
        } finally {
            releaseRemaining(nativeBuffer);
        }
    }

    @Test
    void retainSharesNativeBufferAndAddsOneReference() {
        NettyDataBuffer buffer = bufferFactory.allocateBuffer(3);
        buffer.write(new byte[]{1, 2, 3});
        ByteBuf nativeBuffer = buffer.getNativeBuffer();

        try {
            DataBuffer retained = GatewayDataBufferOwnership.retain(buffer);

            assertSame(
                    nativeBuffer,
                    ((NettyDataBuffer) retained).getNativeBuffer()
            );
            assertEquals(2, nativeBuffer.refCnt());
            assertFalse(GatewayDataBufferOwnership.release(retained));
            assertEquals(1, nativeBuffer.refCnt());
            assertTrue(GatewayDataBufferOwnership.release(buffer));
            assertEquals(0, nativeBuffer.refCnt());
        } finally {
            releaseRemaining(nativeBuffer);
        }
    }

    @Test
    void readOnlySampleIsBoundedAndDoesNotMoveReadPosition() {
        NettyDataBuffer buffer = bufferFactory.allocateBuffer(4);
        buffer.write(new byte[]{10, 11, 12, 13});
        buffer.readPosition(1);
        ByteBuf nativeBuffer = buffer.getNativeBuffer();

        try {
            byte[] sample = GatewayDataBufferOwnership.readOnlySample(
                    buffer,
                    2
            );

            assertArrayEquals(new byte[]{11, 12}, sample);
            assertEquals(1, buffer.readPosition());
            assertEquals(3, buffer.readableByteCount());
            assertEquals(1, nativeBuffer.refCnt());
            assertTrue(GatewayDataBufferOwnership.release(buffer));
            assertEquals(0, nativeBuffer.refCnt());
        } finally {
            releaseRemaining(nativeBuffer);
        }
    }

    @Test
    void transferToNettyMovesNativeOwnershipWithoutAnotherRetain() {
        NettyDataBuffer buffer = bufferFactory.allocateBuffer(3);
        buffer.write(new byte[]{1, 2, 3});
        ByteBuf nativeBuffer = buffer.getNativeBuffer();

        try {
            ByteBuf transferred = GatewayDataBufferOwnership.transferToNetty(
                    buffer,
                    PooledByteBufAllocator.DEFAULT
            );

            assertSame(nativeBuffer, transferred);
            assertEquals(1, nativeBuffer.refCnt());
            assertTrue(transferred.release());
            assertEquals(0, nativeBuffer.refCnt());
        } finally {
            releaseRemaining(nativeBuffer);
        }
    }

    @Test
    void transferToNettyCopiesAndReleasesNonNettyChunk() {
        TrackingPooledDataBuffer input = new TrackingPooledDataBuffer(
                DefaultDataBufferFactory.sharedInstance.wrap(
                        new byte[]{4, 5, 6}
                )
        );
        ByteBuf transferred = GatewayDataBufferOwnership.transferToNetty(
                input,
                PooledByteBufAllocator.DEFAULT
        );
        try {
            byte[] bytes = new byte[transferred.readableBytes()];
            transferred.getBytes(transferred.readerIndex(), bytes);

            assertArrayEquals(new byte[]{4, 5, 6}, bytes);
            assertFalse(input.isAllocated());
        } finally {
            transferred.release();
        }
    }

    @Test
    void transferToNettyReleasesInputWhenAllocationFails() {
        TrackingPooledDataBuffer input = new TrackingPooledDataBuffer(
                DefaultDataBufferFactory.sharedInstance.wrap(
                        new byte[]{4, 5, 6}
                )
        );
        ByteBufAllocator failingAllocator =
                new AbstractByteBufAllocator(false) {
                    @Override
                    public boolean isDirectBufferPooled() {
                        return false;
                    }

                    @Override
                    protected ByteBuf newHeapBuffer(
                            int initialCapacity,
                            int maxCapacity) {
                        throw new IllegalStateException("allocation failed");
                    }

                    @Override
                    protected ByteBuf newDirectBuffer(
                            int initialCapacity,
                            int maxCapacity) {
                        throw new AssertionError("allocation failed");
                    }
                };

        assertThrows(
                IllegalStateException.class,
                () -> GatewayDataBufferOwnership.transferToNetty(
                        input,
                        failingAllocator
                )
        );

        assertFalse(input.isAllocated());
    }

    @Test
    void transferToNettyReleasesInputWhenAllocationRaisesError() {
        TrackingPooledDataBuffer input = new TrackingPooledDataBuffer(
                DefaultDataBufferFactory.sharedInstance.wrap(
                        new byte[]{7, 8, 9}
                )
        );
        ByteBufAllocator failingAllocator =
                new AbstractByteBufAllocator(true) {
                    @Override
                    public boolean isDirectBufferPooled() {
                        return false;
                    }

                    @Override
                    protected ByteBuf newHeapBuffer(
                            int initialCapacity,
                            int maxCapacity) {
                        throw new IllegalStateException("allocation failed");
                    }

                    @Override
                    protected ByteBuf newDirectBuffer(
                            int initialCapacity,
                            int maxCapacity) {
                        throw new AssertionError("allocation failed");
                    }
                };

        assertThrows(
                AssertionError.class,
                () -> GatewayDataBufferOwnership.transferToNetty(
                        input,
                        failingAllocator
                )
        );

        assertFalse(input.isAllocated());
    }

    private void releaseRemaining(ByteBuf buffer) {
        while (buffer.refCnt() > 0) {
            buffer.release();
        }
    }

    private static final class TrackingPooledDataBuffer
            extends DataBufferWrapper implements PooledDataBuffer {

        private boolean allocated = true;

        private TrackingPooledDataBuffer(DataBuffer delegate) {
            super(delegate);
        }

        @Override
        public boolean isAllocated() {
            return allocated;
        }

        @Override
        public PooledDataBuffer retain() {
            if (!allocated) {
                throw new IllegalStateException("buffer already released");
            }
            return this;
        }

        @Override
        public PooledDataBuffer touch(Object hint) {
            return this;
        }

        @Override
        public boolean release() {
            if (!allocated) {
                return false;
            }
            allocated = false;
            return true;
        }
    }
}
