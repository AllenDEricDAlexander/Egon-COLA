package top.egon.cola.component.gateway.core.exchange;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.core.http.GatewayRequestRejectedException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AggregatedGatewayBodyTest {

    @Test
    void enforcesSingleConsumptionAndRelease() {
        AggregatedGatewayBody body = new AggregatedGatewayBody(
                new byte[]{1, 2, 3},
                3,
                false
        );

        assertArrayEquals(new byte[]{1, 2, 3}, body.consume());
        IllegalStateException consumed = assertThrows(
                IllegalStateException.class,
                body::consume
        );
        assertEquals("body already consumed", consumed.getMessage());

        body.close();
        assertTrue(body.closed());
        IllegalStateException closed = assertThrows(
                IllegalStateException.class,
                body::consume
        );
        assertEquals("body is closed", closed.getMessage());
    }

    @Test
    void rejectsBodyAboveRouteLimit() {
        GatewayRequestRejectedException exception = assertThrows(
                GatewayRequestRejectedException.class,
                () -> new AggregatedGatewayBody(new byte[4], 3, false)
        );
        assertEquals("GATEWAY_REQUEST_BODY_TOO_LARGE", exception.code());
    }
}
