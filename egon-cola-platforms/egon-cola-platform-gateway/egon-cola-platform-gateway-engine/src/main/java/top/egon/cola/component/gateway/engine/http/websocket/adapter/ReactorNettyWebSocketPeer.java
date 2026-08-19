package top.egon.cola.component.gateway.engine.http.websocket.adapter;

import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketCloseStatus;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketFrame;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketFrameType;
import top.egon.cola.component.gateway.engine.http.websocket.service.GatewayWebSocketPeer;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;

import java.util.Objects;

/**
 * DataBuffer-preserving adapter shared by upstream and downstream WebSockets.
 * 补充说明 / Supplementary summary: {@code ReactorNettyWebSocketPeer} 是类型，位于当前 Gateway 模块的相关包中，负责ReactorNettyWebSocketPeer相关的职责与边界。
 * English supplement: {@code ReactorNettyWebSocketPeer} is a type in the current Gateway module; it owns the reactor netty web socket peer-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class ReactorNettyWebSocketPeer implements GatewayWebSocketPeer {

    /**
     * 中文说明：保存 connection 对应的状态、依赖或配置值；字段类型为 {@code Connection}，由 {@code ReactorNettyWebSocketPeer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by connection; its type is {@code Connection}, and {@code ReactorNettyWebSocketPeer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ReactorNettyWebSocketPeer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReactorNettyWebSocketPeer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Connection connection;

    /**
     * 中文说明：保存 inbound 对应的状态、依赖或配置值；字段类型为 {@code WebsocketInbound}，由 {@code ReactorNettyWebSocketPeer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by inbound; its type is {@code WebsocketInbound}, and {@code ReactorNettyWebSocketPeer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ReactorNettyWebSocketPeer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReactorNettyWebSocketPeer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final WebsocketInbound inbound;

    /**
     * 中文说明：保存 outbound 对应的状态、依赖或配置值；字段类型为 {@code WebsocketOutbound}，由 {@code ReactorNettyWebSocketPeer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by outbound; its type is {@code WebsocketOutbound}, and {@code ReactorNettyWebSocketPeer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ReactorNettyWebSocketPeer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReactorNettyWebSocketPeer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final WebsocketOutbound outbound;

    /**
     * 中文说明：保存 buffers 对应的状态、依赖或配置值；字段类型为 {@code NettyDataBufferFactory}，由 {@code ReactorNettyWebSocketPeer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by buffers; its type is {@code NettyDataBufferFactory}, and {@code ReactorNettyWebSocketPeer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ReactorNettyWebSocketPeer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReactorNettyWebSocketPeer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final NettyDataBufferFactory buffers;

    /**
     * 中文说明：创建 {@code ReactorNettyWebSocketPeer} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code ReactorNettyWebSocketPeer} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param connection 参数 connection；parameter connection。
     * @param inbound 参数 inbound；parameter inbound。
     * @param outbound 参数 outbound；parameter outbound。
     */
    public ReactorNettyWebSocketPeer(
            Connection connection,
            WebsocketInbound inbound,
            WebsocketOutbound outbound) {
        this.connection = Objects.requireNonNull(connection, "connection");
        this.inbound = Objects.requireNonNull(inbound, "inbound");
        this.outbound = Objects.requireNonNull(outbound, "outbound");
        buffers = new NettyDataBufferFactory(connection.channel().alloc());
    }

    /**
     * 中文说明：执行 receive 操作；该方法是 {@code ReactorNettyWebSocketPeer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the receive operation; this method is the invocation entry point on {@code ReactorNettyWebSocketPeer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyWebSocketPeer.receive(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 receive 的处理结果；returns the result of the operation.
     */
    @Override
    public Flux<GatewayWebSocketFrame> receive() {
        return inbound.receiveFrames().map(this::fromNetty);
    }

    /**
     * 中文说明：执行 send 操作；该方法是 {@code ReactorNettyWebSocketPeer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the send operation; this method is the invocation entry point on {@code ReactorNettyWebSocketPeer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyWebSocketPeer.send(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param frames 参数 frames；parameter frames。
     * @return 返回 send 的处理结果；returns the result of the operation.
     */
    @Override
    public Mono<Void> send(Flux<GatewayWebSocketFrame> frames) {
        return outbound.sendObject(frames
                        .map(this::toNetty)
                        .doOnDiscard(
                                GatewayWebSocketFrame.class,
                                GatewayWebSocketFrame::release
                        ))
                .then();
    }

    /**
     * 中文说明：执行 sendClose 操作；该方法是 {@code ReactorNettyWebSocketPeer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the send close operation; this method is the invocation entry point on {@code ReactorNettyWebSocketPeer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyWebSocketPeer.sendClose(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param status 参数 status；parameter status。
     * @return 返回 sendClose 的处理结果；returns the result of the operation.
     */
    @Override
    public Mono<Void> sendClose(GatewayWebSocketCloseStatus status) {
        Objects.requireNonNull(status, "status");
        if (!status.sendable()) {
            return Mono.fromRunnable(this::dispose);
        }
        return outbound.sendClose(status.code(), status.reason());
    }

    /**
     * 中文说明：执行 dispose 操作；该方法是 {@code ReactorNettyWebSocketPeer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the dispose operation; this method is the invocation entry point on {@code ReactorNettyWebSocketPeer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyWebSocketPeer.dispose(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Override
    public void dispose() {
        connection.dispose();
    }

    /**
     * 中文说明：执行 disposed 操作；该方法是 {@code ReactorNettyWebSocketPeer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the disposed operation; this method is the invocation entry point on {@code ReactorNettyWebSocketPeer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyWebSocketPeer.disposed(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 disposed 的处理结果；returns the result of the operation.
     */
    @Override
    public boolean disposed() {
        return connection.isDisposed();
    }

    /**
     * 中文说明：执行 fromNetty 操作；该方法是 {@code ReactorNettyWebSocketPeer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the from netty operation; this method is the invocation entry point on {@code ReactorNettyWebSocketPeer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyWebSocketPeer.fromNetty(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param frame 参数 frame；parameter frame。
     * @return 返回 fromNetty 的处理结果；returns the result of the operation.
     */
    private GatewayWebSocketFrame fromNetty(WebSocketFrame frame) {
        if (frame instanceof CloseWebSocketFrame close) {
            GatewayWebSocketCloseStatus status = close.statusCode() < 0
                    ? null
                    : new GatewayWebSocketCloseStatus(
                            close.statusCode(),
                            close.reasonText()
                    );
            return new GatewayWebSocketFrame(
                    GatewayWebSocketFrameType.CLOSE,
                    true,
                    buffers.wrap(connection.channel().alloc().buffer(0)),
                    status
            );
        }
        ByteBuf content = frame.content().retain();
        return GatewayWebSocketFrame.data(
                frameType(frame),
                frame.isFinalFragment(),
                buffers.wrap(content)
        );
    }

    /**
     * 中文说明：执行 toNetty 操作；该方法是 {@code ReactorNettyWebSocketPeer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the to netty operation; this method is the invocation entry point on {@code ReactorNettyWebSocketPeer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyWebSocketPeer.toNetty(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param frame 参数 frame；parameter frame。
     * @return 返回 toNetty 的处理结果；returns the result of the operation.
     */
    private WebSocketFrame toNetty(GatewayWebSocketFrame frame) {
        if (frame.type() == GatewayWebSocketFrameType.CLOSE) {
            GatewayWebSocketCloseStatus status = frame.closeStatus();
            frame.release();
            return status == null
                    ? new CloseWebSocketFrame()
                    : new CloseWebSocketFrame(status.code(), status.reason());
        }
        ByteBuf payload = transfer(frame);
        return switch (frame.type()) {
            case TEXT -> new TextWebSocketFrame(
                    frame.finalFragment(),
                    0,
                    payload
            );
            case BINARY -> new BinaryWebSocketFrame(
                    frame.finalFragment(),
                    0,
                    payload
            );
            case CONTINUATION -> new ContinuationWebSocketFrame(
                    frame.finalFragment(),
                    0,
                    payload
            );
            case PING -> new PingWebSocketFrame(payload);
            case PONG -> new PongWebSocketFrame(payload);
            case CLOSE -> throw new IllegalStateException(
                    "close frame handled separately"
            );
        };
    }

    /**
     * 中文说明：执行 transfer 操作；该方法是 {@code ReactorNettyWebSocketPeer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the transfer operation; this method is the invocation entry point on {@code ReactorNettyWebSocketPeer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyWebSocketPeer.transfer(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param frame 参数 frame；parameter frame。
     * @return 返回 transfer 的处理结果；returns the result of the operation.
     */
    private ByteBuf transfer(GatewayWebSocketFrame frame) {
        DataBuffer payload = frame.payload();
        if (payload instanceof NettyDataBuffer netty) {
            ByteBuf nativeBuffer = netty.getNativeBuffer().retain();
            frame.release();
            return nativeBuffer;
        }
        ByteBuf copy = connection.channel().alloc().buffer(
                payload.readableByteCount()
        );
        try (DataBuffer.ByteBufferIterator byteBuffers =
                     payload.readableByteBuffers()) {
            byteBuffers.forEachRemaining(copy::writeBytes);
        } finally {
            frame.release();
        }
        return copy;
    }

    /**
     * 中文说明：执行 frameType 操作；该方法是 {@code ReactorNettyWebSocketPeer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the frame type operation; this method is the invocation entry point on {@code ReactorNettyWebSocketPeer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyWebSocketPeer.frameType(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param frame 参数 frame；parameter frame。
     * @return 返回 frameType 的处理结果；returns the result of the operation.
     */
    private GatewayWebSocketFrameType frameType(WebSocketFrame frame) {
        if (frame instanceof TextWebSocketFrame) {
            return GatewayWebSocketFrameType.TEXT;
        }
        if (frame instanceof BinaryWebSocketFrame) {
            return GatewayWebSocketFrameType.BINARY;
        }
        if (frame instanceof ContinuationWebSocketFrame) {
            return GatewayWebSocketFrameType.CONTINUATION;
        }
        if (frame instanceof PingWebSocketFrame) {
            return GatewayWebSocketFrameType.PING;
        }
        if (frame instanceof PongWebSocketFrame) {
            return GatewayWebSocketFrameType.PONG;
        }
        throw new IllegalArgumentException(
                "unsupported WebSocket frame: "
                        + frame.getClass().getSimpleName()
        );
    }
}
