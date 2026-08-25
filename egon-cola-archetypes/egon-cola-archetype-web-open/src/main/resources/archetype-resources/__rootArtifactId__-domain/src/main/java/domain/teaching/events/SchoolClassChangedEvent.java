package ${package}.domain.teaching.events;

import ${package}.domain.events.OrganizationDomainEvent;
import java.time.Instant;

public record SchoolClassChangedEvent(
        Long eventId, Long aggregateId, Instant occurredAt, Long gradeId, String changeType)
        implements OrganizationDomainEvent {}
