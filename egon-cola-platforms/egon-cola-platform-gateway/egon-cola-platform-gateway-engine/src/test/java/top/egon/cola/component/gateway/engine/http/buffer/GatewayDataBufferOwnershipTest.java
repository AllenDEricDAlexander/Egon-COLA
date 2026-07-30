package top.egon.cola.component.gateway.engine.http.buffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    private void releaseRemaining(ByteBuf buffer) {
        while (buffer.refCnt() > 0) {
            buffer.release();
        }
    }
}
