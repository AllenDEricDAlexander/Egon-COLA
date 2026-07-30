package top.egon.cola.component.gateway.engine.websocket;

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
 */
public final class ReactorNettyWebSocketPeer implements GatewayWebSocketPeer {

    private final Connection connection;

    private final WebsocketInbound inbound;

    private final WebsocketOutbound outbound;

    private final NettyDataBufferFactory buffers;

    public ReactorNettyWebSocketPeer(
            Connection connection,
            WebsocketInbound inbound,
            WebsocketOutbound outbound) {
        this.connection = Objects.requireNonNull(connection, "connection");
        this.inbound = Objects.requireNonNull(inbound, "inbound");
        this.outbound = Objects.requireNonNull(outbound, "outbound");
        buffers = new NettyDataBufferFactory(connection.channel().alloc());
    }

    @Override
    public Flux<GatewayWebSocketFrame> receive() {
        return inbound.receiveFrames().map(this::fromNetty);
    }

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

    @Override
    public Mono<Void> sendClose(GatewayWebSocketCloseStatus status) {
        Objects.requireNonNull(status, "status");
        if (!status.sendable()) {
            return Mono.fromRunnable(this::dispose);
        }
        return outbound.sendClose(status.code(), status.reason());
    }

    @Override
    public void dispose() {
        connection.dispose();
    }

    @Override
    public boolean disposed() {
        return connection.isDisposed();
    }

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
