package ${package}.infrastructure.mq;

import java.time.Instant;
import java.util.Map;

public record OrganizationEventMessage(
        String eventId, String eventType, String aggregateId, Instant occurredAt, Map<String, String> payload) {
    public OrganizationEventMessage { payload = Map.copyOf(payload); }
}
