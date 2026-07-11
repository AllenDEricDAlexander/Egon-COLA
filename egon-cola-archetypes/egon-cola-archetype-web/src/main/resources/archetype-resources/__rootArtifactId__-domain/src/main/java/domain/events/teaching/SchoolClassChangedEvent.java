package ${package}.domain.events.teaching;

import ${package}.domain.events.OrganizationDomainEvent;
import java.time.Instant;

public record SchoolClassChangedEvent(
        String eventId, String aggregateId, Instant occurredAt, String gradeId, String changeType)
        implements OrganizationDomainEvent {}
