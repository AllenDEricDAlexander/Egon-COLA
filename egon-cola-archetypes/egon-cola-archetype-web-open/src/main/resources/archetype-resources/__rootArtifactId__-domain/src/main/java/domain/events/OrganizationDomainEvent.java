package ${package}.domain.events;

import java.time.Instant;

public interface OrganizationDomainEvent {
    Long eventId();
    Long aggregateId();
    Instant occurredAt();
}
