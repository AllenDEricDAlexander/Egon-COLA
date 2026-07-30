package top.egon.cola.component.gateway.engine.observability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;
import top.egon.cola.component.gateway.contract.trace.GatewayTraceContext;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayCallEventWireCompatibilityTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void transportLifecycleMetadataDoesNotChangeV1EventWireShape()
            throws Exception {
        GatewayCallObservation observation = new GatewayCallObservation(
                Clock.fixed(Instant.ofEpochMilli(100), ZoneOffset.UTC),
                GatewayTraceContext.fromHeaders(null, null, null),
                "request-1",
                "HTTP",
                "PUBLIC",
                "engine-1"
        );
        observation.transport(
                "HTTP_STREAMING",
                "FIRST_BODY_BUFFER_SENT",
                "CLIENT_CANCELLED"
        );

        GatewayCallEventV1 event = observation.complete(
                "FORWARD",
                "CANCELLED",
                null,
                null,
                null
        ).orElseThrow();
        JsonNode json = mapper.valueToTree(event);

        assertEquals(Set.of(
                "eventSchemaVersion",
                "eventId",
                "occurredAt",
                "completedAt",
                "trace",
                "request",
                "routing",
                "governance",
                "result",
                "attempts"
        ), fields(json));
        assertEquals(Set.of(
                "terminalStage",
                "rateLimitDecision",
                "circuitDecision",
                "securityDecision",
                "retryCount"
        ), fields(json.get("governance")));
    }

    private Set<String> fields(JsonNode node) {
        return StreamSupport.stream(
                        ((Iterable<String>) () -> node.fieldNames())
                                .spliterator(),
                        false
                )
                .collect(Collectors.toSet());
    }
}
