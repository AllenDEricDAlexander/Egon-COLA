package ${package}.domain.events;

import java.time.Instant;

public interface OrganizationDomainEvent {
    String eventId();
    Long aggregateId();
    Instant occurredAt();
}
