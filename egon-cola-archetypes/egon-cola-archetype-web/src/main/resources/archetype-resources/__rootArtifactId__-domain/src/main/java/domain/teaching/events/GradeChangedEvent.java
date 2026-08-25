package ${package}.domain.teaching.events;

import ${package}.domain.events.OrganizationDomainEvent;
import java.time.Instant;

public record GradeChangedEvent(String eventId, Long aggregateId, Instant occurredAt, String changeType)
        implements OrganizationDomainEvent {}
