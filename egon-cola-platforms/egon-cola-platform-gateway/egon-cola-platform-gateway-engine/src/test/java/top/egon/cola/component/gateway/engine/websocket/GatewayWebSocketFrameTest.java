package top.egon.cola.component.gateway.engine.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayWebSocketFrameTest {

    @Test
    void preservesBinaryPayloadAndFragmentMetadataWithoutTextConversion() {
        byte[] invalidUtf8 = new byte[]{0, (byte) 0xff, (byte) 0x80, 1};
        DataBuffer payload = DefaultDataBufferFactory.sharedInstance.wrap(
                invalidUtf8
        );

        GatewayWebSocketFrame frame = new GatewayWebSocketFrame(
                GatewayWebSocketFrameType.BINARY,
                false,
                payload,
                null
        );

        assertFalse(frame.finalFragment());
        assertArrayEquals(invalidUtf8, frame.payloadBytes());
        assertTrue(frame.release());
        assertFalse(frame.release());
    }

    @Test
    void distinguishesContinuationPingPongAndSendableCloseStatuses() {
        for (GatewayWebSocketFrameType type : new GatewayWebSocketFrameType[]{
                GatewayWebSocketFrameType.CONTINUATION,
                GatewayWebSocketFrameType.PING,
                GatewayWebSocketFrameType.PONG
        }) {
            GatewayWebSocketFrame frame = GatewayWebSocketFrame.data(
                    type,
                    true,
                    DefaultDataBufferFactory.sharedInstance.wrap(
                            new byte[]{1}
                    )
            );
            assertTrue(frame.finalFragment());
            frame.release();
        }

        assertTrue(new GatewayWebSocketCloseStatus(1000, "done")
                .sendable());
        assertFalse(GatewayWebSocketCloseStatus.abnormal().sendable());
        assertThrows(
                IllegalArgumentException.class,
                () -> new GatewayWebSocketCloseStatus(1005, "reserved")
        );
    }
}
