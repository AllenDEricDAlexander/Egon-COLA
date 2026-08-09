package top.egon.cola.component.gateway.mcp.transport;

import org.reactivestreams.Publisher;

import java.time.Duration;
import java.time.Instant;

/**
 * Cross-node append/read contract used by MCP response and subscription streams.
 */
public interface McpSubscriptionEventStore {

    Publisher<Event> append(
            String streamId,
            String type,
            String data,
            Duration ttl
    );

    Publisher<Event> listen(
            String streamId,
            String afterEventId,
            Duration wait
    );

    record Event(
            String eventId,
            String type,
            String data,
            Instant createdAt
    ) {

        public Event {
            eventId = required(eventId, "eventId");
            type = required(type, "type");
            data = java.util.Objects.requireNonNull(data, "data");
            createdAt = java.util.Objects.requireNonNull(
                    createdAt,
                    "createdAt"
            );
        }

        private static String required(String value, String field) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(field + " is required");
            }
            return value.trim();
        }
    }
}
