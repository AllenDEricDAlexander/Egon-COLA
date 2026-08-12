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
 * 补充说明 / Supplementary summary: {@code GatewayDataBufferOwnership} 是类型，位于当前 Gateway 模块的相关包中，负责网关Data缓冲区Ownership相关的职责与边界。
 * English supplement: {@code GatewayDataBufferOwnership} is a type in the current Gateway module; it owns the gateway data buffer ownership-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayDataBufferOwnership {

    /**
     * 中文说明：创建 {@code GatewayDataBufferOwnership} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayDataBufferOwnership} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     */
    private GatewayDataBufferOwnership() {
    }

    /**
     * Retains the native buffer exactly once before wrapping it. The returned
     * buffer owns that added reference until it is transferred or released.
     * 补充说明 / Supplementary summary: 执行 retainAndWrap 操作；该方法是 {@code GatewayDataBufferOwnership} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English supplement: Executes the retain and wrap operation; this method is the invocation entry point on {@code GatewayDataBufferOwnership} and performs the corresponding runtime, management, or protocol work.
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDataBufferOwnership.retainAndWrap(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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

    /**
     * 中文说明：执行 retain 操作；该方法是 {@code GatewayDataBufferOwnership} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the retain operation; this method is the invocation entry point on {@code GatewayDataBufferOwnership} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDataBufferOwnership.retain(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param buffer 参数 缓冲区；parameter buffer。
     * @return 返回 retain 的处理结果；returns the result of the operation.
     */
    public static <T extends DataBuffer> T retain(T buffer) {
        return DataBufferUtils.retain(
                Objects.requireNonNull(buffer, "buffer")
        );
    }

    /**
     * 中文说明：执行 发布 操作；该方法是 {@code GatewayDataBufferOwnership} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the release operation; this method is the invocation entry point on {@code GatewayDataBufferOwnership} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDataBufferOwnership.release(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param buffer 参数 缓冲区；parameter buffer。
     * @return 返回 发布 的处理结果；returns the result of the operation.
     */
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
     * 补充说明 / Supplementary summary: 执行 transferToNetty 操作；该方法是 {@code GatewayDataBufferOwnership} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English supplement: Executes the transfer to netty operation; this method is the invocation entry point on {@code GatewayDataBufferOwnership} and performs the corresponding runtime, management, or protocol work.
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDataBufferOwnership.transferToNetty(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    public static ByteBuf transferToNetty(
            DataBuffer buffer,
            ByteBufAllocator allocator) {
        Objects.requireNonNull(buffer, "buffer");
        Objects.requireNonNull(allocator, "allocator");
        if (buffer instanceof NettyDataBuffer nettyBuffer) {
            return nettyBuffer.getNativeBuffer();
        }
        ByteBuf copy = null;
        try {
            int readableBytes = buffer.readableByteCount();
            copy = allocator.buffer(readableBytes, readableBytes);
            try (DataBuffer.ByteBufferIterator byteBuffers =
                         buffer.readableByteBuffers()) {
                while (byteBuffers.hasNext()) {
                    copy.writeBytes(byteBuffers.next());
                }
            }
            return copy;
        } catch (RuntimeException | Error failure) {
            if (copy != null) {
                copy.release();
            }
            throw failure;
        } finally {
            release(buffer);
        }
    }

    /**
     * Copies at most {@code maxBytes} readable bytes without changing the
     * DataBuffer read position or retaining its pooled storage.
     * 补充说明 / Supplementary summary: 执行 readOnlySample 操作；该方法是 {@code GatewayDataBufferOwnership} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English supplement: Executes the read only sample operation; this method is the invocation entry point on {@code GatewayDataBufferOwnership} and performs the corresponding runtime, management, or protocol work.
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDataBufferOwnership.readOnlySample(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
