package ${package}.domain.user.events;

import ${package}.domain.events.OrganizationDomainEvent;
import java.time.Instant;

public record UserChangedEvent(String eventId, String aggregateId, Instant occurredAt, String changeType)
        implements OrganizationDomainEvent {}
