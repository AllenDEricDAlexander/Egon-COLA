package top.egon.cola.component.gateway.engine.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.mcp.subscription.McpSubscriptionService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpSubscriptionHaTest {

    @Test
    void stableUpdateIsVisibleThroughRcListenOnAnotherNode() {
        SharedMcpTransportStore shared = new SharedMcpTransportStore();
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-02T01:00:00Z"),
                ZoneOffset.UTC
        );
        McpSubscriptionService stableNode = new McpSubscriptionService(
                shared,
                new ObjectMapper(),
                clock,
                Duration.ofMinutes(30),
                Duration.ofMillis(20)
        );
        McpSubscriptionService rcNode = new McpSubscriptionService(
                shared,
                new ObjectMapper(),
                clock,
                Duration.ofMinutes(30),
                Duration.ofMillis(20)
        );
        String uri = "egon://finance/report/daily";

        Mono.from(stableNode.subscribe("session-1", uri)).block();
        Mono.from(stableNode.publishUpdated(uri)).block();
        McpSubscriptionService.ResourceEvent event = Flux.from(
                rcNode.listen(uri, null)
        ).next().block(Duration.ofSeconds(1));

        assertEquals(uri, event.uri());
        assertEquals("UPDATED", event.kind());
        assertTrue(event.eventId().endsWith("-0"));
    }
}
