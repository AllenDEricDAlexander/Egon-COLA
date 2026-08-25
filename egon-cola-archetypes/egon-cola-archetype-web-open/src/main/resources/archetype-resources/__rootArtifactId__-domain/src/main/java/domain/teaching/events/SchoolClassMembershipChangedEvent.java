package ${package}.domain.teaching.events;

import ${package}.domain.events.OrganizationDomainEvent;
import java.time.Instant;

public record SchoolClassMembershipChangedEvent(
        String eventId, Long aggregateId, Instant occurredAt, Long userId, String changeType)
        implements OrganizationDomainEvent {}
