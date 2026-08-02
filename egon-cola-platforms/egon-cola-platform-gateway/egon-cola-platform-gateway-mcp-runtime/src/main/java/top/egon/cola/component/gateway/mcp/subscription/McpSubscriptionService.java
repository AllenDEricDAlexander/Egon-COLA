package top.egon.cola.component.gateway.mcp.subscription;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.mcp.security.McpSecurityDigests;
import top.egon.cola.component.gateway.mcp.transport.McpSubscriptionEventStore;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Publishes resource changes to deterministic Redis streams for cross-node use.
 */
public final class McpSubscriptionService {

    private final McpSubscriptionEventStore events;

    private final ObjectMapper objectMapper;

    private final Clock clock;

    private final Duration ttl;

    private final Duration wait;

    public McpSubscriptionService(
            McpSubscriptionEventStore events,
            ObjectMapper objectMapper,
            Clock clock,
            Duration ttl,
            Duration wait) {
        this.events = Objects.requireNonNull(events, "events");
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
        this.clock = Objects.requireNonNull(clock, "clock");
        this.ttl = positive(ttl, "ttl");
        this.wait = positive(wait, "wait");
    }

    public Publisher<Subscription> subscribe(String sessionId, String uri) {
        Subscription subscription = new Subscription(
                McpSecurityDigests.token(sessionId + '\0' + uri),
                sessionId,
                uri,
                clock.instant()
        );
        return Mono.from(events.append(
                        streamKey("subscription\0" + sessionId),
                        "subscribed",
                        json(Map.of(
                                "subscriptionId", subscription.subscriptionId(),
                                "uri", uri
                        )),
                        ttl
                ))
                .thenReturn(subscription);
    }

    public Publisher<ResourceEvent> publishUpdated(String uri) {
        return publish(uri, "UPDATED");
    }

    public Publisher<ResourceEvent> publishListChanged(String uri) {
        return publish(uri, "LIST_CHANGED");
    }

    public Publisher<ResourceEvent> listen(String uri, String afterEventId) {
        return Flux.from(events.listen(
                        streamKey("resource\0" + uri),
                        afterEventId,
                        wait
                ))
                .map(this::decode);
    }

    private Mono<ResourceEvent> publish(String uri, String kind) {
        Instant occurredAt = clock.instant();
        String data = json(Map.of(
                "uri", uri,
                "kind", kind,
                "occurredAt", occurredAt.toString()
        ));
        return Mono.from(events.append(
                        streamKey("resource\0" + uri),
                        "resource-event",
                        data,
                        ttl
                ))
                .map(event -> new ResourceEvent(
                        event.eventId(),
                        uri,
                        kind,
                        occurredAt
                ));
    }

    private ResourceEvent decode(McpSubscriptionEventStore.Event event) {
        try {
            JsonNode node = objectMapper.readTree(event.data());
            return new ResourceEvent(
                    event.eventId(),
                    node.path("uri").asText(),
                    node.path("kind").asText(),
                    Instant.parse(node.path("occurredAt").asText())
            );
        } catch (RuntimeException | JsonProcessingException failure) {
            throw new IllegalStateException(
                    "MCP subscription event is invalid",
                    failure
            );
        }
    }

    private String streamKey(String value) {
        return McpSecurityDigests.token(value);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException(
                    "MCP subscription serialization failed",
                    failure
            );
        }
    }

    private Duration positive(Duration value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    public record Subscription(
            String subscriptionId,
            String sessionId,
            String uri,
            Instant createdAt
    ) {
    }

    public record ResourceEvent(
            String eventId,
            String uri,
            String kind,
            Instant occurredAt
    ) {
    }
}
