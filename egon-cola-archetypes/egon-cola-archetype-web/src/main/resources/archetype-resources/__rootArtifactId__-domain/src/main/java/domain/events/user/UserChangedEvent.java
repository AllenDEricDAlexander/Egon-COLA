package ${package}.domain.events.user;

import ${package}.domain.events.OrganizationDomainEvent;
import java.time.Instant;

public record UserChangedEvent(String eventId, String aggregateId, Instant occurredAt, String changeType)
        implements OrganizationDomainEvent {}
