package ${package}.domain.teaching.events;

import ${package}.domain.events.OrganizationDomainEvent;
import java.time.Instant;

public record SchoolClassChangedEvent(
        String eventId, String aggregateId, Instant occurredAt, String gradeId, String changeType)
        implements OrganizationDomainEvent {}
