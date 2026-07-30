package top.egon.cola.component.gateway.engine.websocket;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Transport frame whose payload remains binary and reference-counted.
 */
public final class GatewayWebSocketFrame {

    private final GatewayWebSocketFrameType type;

    private final boolean finalFragment;

    private final DataBuffer payload;

    private final GatewayWebSocketCloseStatus closeStatus;

    private final AtomicBoolean released = new AtomicBoolean();

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

    public static GatewayWebSocketFrame close(
            GatewayWebSocketCloseStatus status) {
        return new GatewayWebSocketFrame(
                GatewayWebSocketFrameType.CLOSE,
                true,
                DefaultDataBufferFactory.sharedInstance.wrap(new byte[0]),
                status
        );
    }

    public GatewayWebSocketFrameType type() {
        return type;
    }

    public boolean finalFragment() {
        return finalFragment;
    }

    public DataBuffer payload() {
        return payload;
    }

    public int payloadBytesCount() {
        return payload.readableByteCount();
    }

    public GatewayWebSocketCloseStatus closeStatus() {
        return closeStatus;
    }

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

    public boolean release() {
        if (!released.compareAndSet(false, true)) {
            return false;
        }
        DataBufferUtils.release(payload);
        return true;
    }
}
