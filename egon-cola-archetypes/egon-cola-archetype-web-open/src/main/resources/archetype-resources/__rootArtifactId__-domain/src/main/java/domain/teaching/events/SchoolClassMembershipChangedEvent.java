package ${package}.domain.teaching.events;

import ${package}.domain.events.OrganizationDomainEvent;
import java.time.Instant;

public record SchoolClassMembershipChangedEvent(
        Long eventId, Long aggregateId, Instant occurredAt, Long userId, String changeType)
        implements OrganizationDomainEvent {}
