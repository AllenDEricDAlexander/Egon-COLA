package ${package}.domain.teaching.events;

import ${package}.domain.events.OrganizationDomainEvent;
import java.time.Instant;

public record SchoolClassMembershipChangedEvent(
        String eventId, String aggregateId, Instant occurredAt, String userId, String changeType)
        implements OrganizationDomainEvent {}
