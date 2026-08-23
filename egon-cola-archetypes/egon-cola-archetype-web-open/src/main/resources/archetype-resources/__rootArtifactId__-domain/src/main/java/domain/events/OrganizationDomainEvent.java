package ${package}.domain.events;

import java.time.Instant;

public interface OrganizationDomainEvent {
    String eventId();
    String aggregateId();
    Instant occurredAt();
}
