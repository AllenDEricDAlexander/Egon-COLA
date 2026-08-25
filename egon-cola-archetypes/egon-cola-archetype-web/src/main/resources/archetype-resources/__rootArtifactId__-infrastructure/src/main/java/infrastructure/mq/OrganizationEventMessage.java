package ${package}.infrastructure.mq;

import java.time.Instant;
import java.util.Map;

public record OrganizationEventMessage(
        Long eventId, String eventType, Long aggregateId, Instant occurredAt, Map<String, String> payload) {
    public OrganizationEventMessage { payload = Map.copyOf(payload); }
}
