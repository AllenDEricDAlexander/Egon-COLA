package ${package}.domain.user.events;

import ${package}.domain.events.OrganizationDomainEvent;
import java.time.Instant;

public record RoleAssignedEvent(Long eventId, Long aggregateId, Instant occurredAt, String roleCode)
        implements OrganizationDomainEvent {}
