package top.egon.cola.archetype.source.webopen.domain.events;

import java.time.Instant;

public interface OrganizationDomainEvent {
    String eventId();
    Long aggregateId();
    Instant occurredAt();
}
