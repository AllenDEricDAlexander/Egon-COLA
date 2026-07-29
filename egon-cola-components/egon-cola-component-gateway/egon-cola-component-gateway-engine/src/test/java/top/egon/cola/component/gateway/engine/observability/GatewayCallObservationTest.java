package top.egon.cola.component.gateway.engine.observability;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;
import top.egon.cola.component.gateway.contract.trace.GatewayTraceContext;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayCallObservationTest {

    @Test
    void publishesExactlyOneCompletionFact() {
        GatewayCallObservation observation = new GatewayCallObservation(
                Clock.fixed(Instant.ofEpochMilli(100), ZoneOffset.UTC),
                GatewayTraceContext.fromHeaders(null, null, null),
                "request-1",
                "HTTP",
                "PUBLIC",
                "engine-1"
        );
        observation.route(
                "GET",
                "/orders/{id}",
                "group-1",
                "release-1",
                "operation-1",
                "route-1"
        );
        observation.addRequestBytes(12);
        observation.addResponseBytes(21);
        observation.attempt(
                1,
                observation.trace().newChildSpanId(),
                "provider-1",
                100,
                2,
                "SUCCESS",
                null
        );

        Optional<GatewayCallEventV1> event = observation.complete(
                "COMPLETE",
                "SUCCESS",
                null,
                200,
                null
        );

        assertTrue(event.isPresent());
        assertTrue(observation.complete(
                "COMPLETE",
                "ERROR",
                "LATE",
                500,
                null
        ).isEmpty());
        assertEquals(12, event.orElseThrow().request().requestBytes());
        assertEquals(21, event.orElseThrow().result().responseBytes());
        assertEquals(1, event.orElseThrow().attempts().size());
    }
}
