package top.egon.cola.component.gateway.engine.websocket;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Transport frame whose payload remains binary and reference-counted.
 * 补充说明 / Supplementary summary: {@code GatewayWebSocketFrame} 是类型，位于当前 Gateway 模块的相关包中，负责网关WebSocketFrame相关的职责与边界。
 * English supplement: {@code GatewayWebSocketFrame} is a type in the current Gateway module; it owns the gateway web socket frame-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayWebSocketFrame {

    /**
     * 中文说明：保存 type 对应的状态、依赖或配置值；字段类型为 {@code GatewayWebSocketFrameType}，由 {@code GatewayWebSocketFrame} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by type; its type is {@code GatewayWebSocketFrameType}, and {@code GatewayWebSocketFrame} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayWebSocketFrame} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketFrame}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayWebSocketFrameType type;

    /**
     * 中文说明：保存 finalFragment 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayWebSocketFrame} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by final fragment; its type is {@code boolean}, and {@code GatewayWebSocketFrame} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayWebSocketFrame} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketFrame}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final boolean finalFragment;

    /**
     * 中文说明：保存 payload 对应的状态、依赖或配置值；字段类型为 {@code DataBuffer}，由 {@code GatewayWebSocketFrame} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by payload; its type is {@code DataBuffer}, and {@code GatewayWebSocketFrame} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayWebSocketFrame} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketFrame}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final DataBuffer payload;

    /**
     * 中文说明：保存 closeStatus 对应的状态、依赖或配置值；字段类型为 {@code GatewayWebSocketCloseStatus}，由 {@code GatewayWebSocketFrame} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by close status; its type is {@code GatewayWebSocketCloseStatus}, and {@code GatewayWebSocketFrame} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayWebSocketFrame} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketFrame}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayWebSocketCloseStatus closeStatus;

    /**
     * 中文说明：保存 released 对应的状态、依赖或配置值；字段类型为 {@code AtomicBoolean}，由 {@code GatewayWebSocketFrame} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by released; its type is {@code AtomicBoolean}, and {@code GatewayWebSocketFrame} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayWebSocketFrame} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketFrame}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicBoolean released = new AtomicBoolean();

    /**
     * 中文说明：创建 {@code GatewayWebSocketFrame} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayWebSocketFrame} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param type 参数 type；parameter type。
     * @param finalFragment 参数 finalFragment；parameter final fragment。
     * @param payload 参数 payload；parameter payload。
     * @param closeStatus 参数 closeStatus；parameter close status。
     */
    public GatewayWebSocketFrame(
            GatewayWebSocketFrameType type,
            boolean finalFragment,
            DataBuffer payload,
            GatewayWebSocketCloseStatus closeStatus) {
        this.type = Objects.requireNonNull(type, "type");
        this.finalFragment = finalFragment;
        this.payload = Objects.requireNonNull(payload, "payload");
        this.closeStatus = closeStatus;
        if (type == GatewayWebSocketFrameType.CLOSE) {
            if (!finalFragment) {
                throw new IllegalArgumentException(
                        "WebSocket close frame must be final"
                );
            }
        } else if (closeStatus != null) {
            throw new IllegalArgumentException(
                    "closeStatus requires CLOSE frame"
            );
        }
        if ((type == GatewayWebSocketFrameType.PING
                || type == GatewayWebSocketFrameType.PONG)
                && !finalFragment) {
            throw new IllegalArgumentException(
                    "WebSocket control frame must be final"
            );
        }
    }

    /**
     * 中文说明：执行 data 操作；该方法是 {@code GatewayWebSocketFrame} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the data operation; this method is the invocation entry point on {@code GatewayWebSocketFrame} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketFrame.data(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param type 参数 type；parameter type。
     * @param finalFragment 参数 finalFragment；parameter final fragment。
     * @param payload 参数 payload；parameter payload。
     * @return 返回 data 的处理结果；returns the result of the operation.
     */
    public static GatewayWebSocketFrame data(
            GatewayWebSocketFrameType type,
            boolean finalFragment,
            DataBuffer payload) {
        if (type == GatewayWebSocketFrameType.CLOSE) {
            throw new IllegalArgumentException(
                    "use close() for a close frame"
            );
        }
        return new GatewayWebSocketFrame(
                type,
                finalFragment,
                payload,
                null
        );
    }

    /**
     * 中文说明：执行 close 操作；该方法是 {@code GatewayWebSocketFrame} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the close operation; this method is the invocation entry point on {@code GatewayWebSocketFrame} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketFrame.close(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param status 参数 status；parameter status。
     * @return 返回 close 的处理结果；returns the result of the operation.
     */
    public static GatewayWebSocketFrame close(
            GatewayWebSocketCloseStatus status) {
        return new GatewayWebSocketFrame(
                GatewayWebSocketFrameType.CLOSE,
                true,
                DefaultDataBufferFactory.sharedInstance.wrap(new byte[0]),
                status
        );
    }

    /**
     * 中文说明：执行 type 操作；该方法是 {@code GatewayWebSocketFrame} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the type operation; this method is the invocation entry point on {@code GatewayWebSocketFrame} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketFrame.type(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 type 的处理结果；returns the result of the operation.
     */
    public GatewayWebSocketFrameType type() {
        return type;
    }

    /**
     * 中文说明：执行 finalFragment 操作；该方法是 {@code GatewayWebSocketFrame} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the final fragment operation; this method is the invocation entry point on {@code GatewayWebSocketFrame} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketFrame.finalFragment(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 finalFragment 的处理结果；returns the result of the operation.
     */
    public boolean finalFragment() {
        return finalFragment;
    }

    /**
     * 中文说明：执行 payload 操作；该方法是 {@code GatewayWebSocketFrame} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the payload operation; this method is the invocation entry point on {@code GatewayWebSocketFrame} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketFrame.payload(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 payload 的处理结果；returns the result of the operation.
     */
    public DataBuffer payload() {
        return payload;
    }

    /**
     * 中文说明：执行 payloadBytesCount 操作；该方法是 {@code GatewayWebSocketFrame} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the payload bytes count operation; this method is the invocation entry point on {@code GatewayWebSocketFrame} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketFrame.payloadBytesCount(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 payloadBytesCount 的处理结果；returns the result of the operation.
     */
    public int payloadBytesCount() {
        return payload.readableByteCount();
    }

    /**
     * 中文说明：执行 closeStatus 操作；该方法是 {@code GatewayWebSocketFrame} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the close status operation; this method is the invocation entry point on {@code GatewayWebSocketFrame} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketFrame.closeStatus(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 closeStatus 的处理结果；returns the result of the operation.
     */
    public GatewayWebSocketCloseStatus closeStatus() {
        return closeStatus;
    }

    /**
     * 中文说明：执行 payloadBytes 操作；该方法是 {@code GatewayWebSocketFrame} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the payload bytes operation; this method is the invocation entry point on {@code GatewayWebSocketFrame} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketFrame.payloadBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 payloadBytes 的处理结果；returns the result of the operation.
     */
    public byte[] payloadBytes() {
        byte[] copy = new byte[payload.readableByteCount()];
        int offset = 0;
        try (DataBuffer.ByteBufferIterator buffers =
                     payload.readableByteBuffers()) {
            while (buffers.hasNext()) {
                ByteBuffer bytes = buffers.next().duplicate();
                int count = bytes.remaining();
                bytes.get(copy, offset, count);
                offset += count;
            }
        }
        return copy;
    }

    /**
     * 中文说明：执行 发布 操作；该方法是 {@code GatewayWebSocketFrame} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the release operation; this method is the invocation entry point on {@code GatewayWebSocketFrame} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketFrame.release(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 发布 的处理结果；returns the result of the operation.
     */
    public boolean release() {
        if (!released.compareAndSet(false, true)) {
            return false;
        }
        DataBufferUtils.release(payload);
        return true;
    }
}
