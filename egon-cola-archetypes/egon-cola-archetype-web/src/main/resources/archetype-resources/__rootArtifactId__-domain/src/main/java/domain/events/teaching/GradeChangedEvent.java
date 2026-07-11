package ${package}.domain.events.teaching;

import ${package}.domain.events.OrganizationDomainEvent;
import java.time.Instant;

public record GradeChangedEvent(String eventId, String aggregateId, Instant occurredAt, String changeType)
        implements OrganizationDomainEvent {}
