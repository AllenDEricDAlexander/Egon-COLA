package top.egon.cola.component.gateway.engine.transport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayCommitGuardTest {

    @Test
    void advancesHttpCommitFactsMonotonically() {
        GatewayCommitGuard guard = GatewayCommitGuard.http();

        assertTrue(guard.advance(GatewayCommitPoint.REQUEST_STREAMING));
        assertTrue(guard.advance(
                GatewayCommitPoint.UPSTREAM_HEADERS_RECEIVED
        ));
        assertTrue(guard.advance(
                GatewayCommitPoint.DOWNSTREAM_HEADERS_COMMITTED
        ));
        assertTrue(guard.advance(
                GatewayCommitPoint.FIRST_BODY_BUFFER_SENT
        ));
        assertFalse(guard.advance(GatewayCommitPoint.REQUEST_STREAMING));
        assertTrue(guard.terminate());
        assertFalse(guard.advance(
                GatewayCommitPoint.FIRST_BODY_BUFFER_SENT
        ));

        assertEquals(GatewayCommitPoint.TERMINATED, guard.current());
        assertTrue(guard.upstreamAccepted());
        assertTrue(guard.downstreamCommitted());
        assertTrue(guard.payloadCommitted());
    }

    @Test
    void keepsWebsocketHandshakeAndFrameFactsSeparateFromHttp() {
        GatewayCommitGuard guard = GatewayCommitGuard.websocket();

        assertTrue(guard.advance(
                GatewayCommitPoint.UPSTREAM_HANDSHAKE_RECEIVED
        ));
        assertTrue(guard.advance(
                GatewayCommitPoint.CLIENT_HANDSHAKE_COMMITTED
        ));
        assertTrue(guard.advance(
                GatewayCommitPoint.FIRST_FRAME_FORWARDED
        ));
        assertThrows(IllegalArgumentException.class, () -> guard.advance(
                GatewayCommitPoint.UPSTREAM_HEADERS_RECEIVED
        ));
    }

    @Test
    void terminationDoesNotInventCommitFacts() {
        GatewayCommitGuard guard = GatewayCommitGuard.http();

        guard.terminate();

        assertFalse(guard.upstreamAccepted());
        assertFalse(guard.downstreamCommitted());
        assertFalse(guard.payloadCommitted());
    }
}
