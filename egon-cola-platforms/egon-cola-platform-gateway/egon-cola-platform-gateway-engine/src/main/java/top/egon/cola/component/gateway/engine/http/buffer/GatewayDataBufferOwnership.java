package top.egon.cola.component.gateway.engine.http.buffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Centralizes retain, release, and read-only sampling rules at the Engine
 * transport boundary.
 */
public final class GatewayDataBufferOwnership {

    private GatewayDataBufferOwnership() {
    }

    /**
     * Retains the native buffer exactly once before wrapping it. The returned
     * buffer owns that added reference until it is transferred or released.
     */
    public static NettyDataBuffer retainAndWrap(
            NettyDataBufferFactory bufferFactory,
            ByteBuf nativeBuffer) {
        Objects.requireNonNull(bufferFactory, "bufferFactory");
        Objects.requireNonNull(nativeBuffer, "nativeBuffer");
        nativeBuffer.retain();
        try {
            return bufferFactory.wrap(nativeBuffer);
        } catch (RuntimeException | Error failure) {
            nativeBuffer.release();
            throw failure;
        }
    }

    public static <T extends DataBuffer> T retain(T buffer) {
        return DataBufferUtils.retain(
                Objects.requireNonNull(buffer, "buffer")
        );
    }

    public static boolean release(DataBuffer buffer) {
        return DataBufferUtils.release(
                Objects.requireNonNull(buffer, "buffer")
        );
    }

    /**
     * Transfers a native Netty buffer as-is. Other DataBuffer implementations
     * are copied one chunk at a time into the target allocator and released as
     * soon as that chunk has been copied. The caller must not release the input
     * after this method returns successfully because outbound owns the result.
     */
    public static ByteBuf transferToNetty(
            DataBuffer buffer,
            ByteBufAllocator allocator) {
        Objects.requireNonNull(buffer, "buffer");
        Objects.requireNonNull(allocator, "allocator");
        if (buffer instanceof NettyDataBuffer nettyBuffer) {
            return nettyBuffer.getNativeBuffer();
        }
        int readableBytes = buffer.readableByteCount();
        ByteBuf copy = allocator.buffer(readableBytes, readableBytes);
        try (DataBuffer.ByteBufferIterator byteBuffers =
                     buffer.readableByteBuffers()) {
            while (byteBuffers.hasNext()) {
                copy.writeBytes(byteBuffers.next());
            }
            return copy;
        } catch (RuntimeException | Error failure) {
            copy.release();
            throw failure;
        } finally {
            release(buffer);
        }
    }

    /**
     * Copies at most {@code maxBytes} readable bytes without changing the
     * DataBuffer read position or retaining its pooled storage.
     */
    public static byte[] readOnlySample(DataBuffer buffer, int maxBytes) {
        Objects.requireNonNull(buffer, "buffer");
        if (maxBytes < 0) {
            throw new IllegalArgumentException(
                    "sample byte limit must not be negative"
            );
        }
        int sampleLength = Math.min(
                buffer.readableByteCount(),
                maxBytes
        );
        if (sampleLength == 0) {
            return new byte[0];
        }
        byte[] sample = new byte[sampleLength];
        ByteBuffer readOnlyView = buffer.toByteBuffer(
                buffer.readPosition(),
                sampleLength
        );
        readOnlyView.get(sample);
        return sample;
    }
}
