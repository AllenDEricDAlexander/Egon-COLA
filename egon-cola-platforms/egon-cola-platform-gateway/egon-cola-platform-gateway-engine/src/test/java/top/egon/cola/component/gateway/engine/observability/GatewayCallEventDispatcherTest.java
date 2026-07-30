package top.egon.cola.component.gateway.engine.observability;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;
import top.egon.cola.component.gateway.contract.trace.GatewayTraceContext;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayCallEventDispatcherTest {

    @Test
    void drainsAcceptedEventsWithoutExposingSensitiveRequestData()
            throws Exception {
        List<String> payloads = new CopyOnWriteArrayList<>();
        GatewayCallEventDispatcher dispatcher =
                new GatewayCallEventDispatcher(
                        4,
                        32 * 1024,
                        Duration.ofSeconds(1),
                        new GatewayCallEventSerializer(),
                        (event, payload) -> payloads.add(new String(payload))
                );

        dispatcher.onComplete(event());
        dispatcher.close();

        assertEquals(1, payloads.size());
        assertTrue(payloads.getFirst().contains("\"traceId\""));
        assertFalse(payloads.getFirst().contains("authorization"));
        assertEquals(1, dispatcher.health().sent());
    }

    private static GatewayCallEventV1 event() {
        GatewayCallObservation observation = GatewayCallObservation.start(
                GatewayTraceContext.fromHeaders(null, null, null),
                "HTTP",
                "PUBLIC",
                "engine-1"
        );
        return observation.complete(
                "COMPLETE",
                "SUCCESS",
                null,
                200,
                null
        ).orElseThrow();
    }
}
