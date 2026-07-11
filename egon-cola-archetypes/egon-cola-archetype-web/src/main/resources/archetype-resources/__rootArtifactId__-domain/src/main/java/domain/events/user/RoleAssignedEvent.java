package ${package}.domain.events.user;

import ${package}.domain.events.OrganizationDomainEvent;
import java.time.Instant;

public record RoleAssignedEvent(String eventId, String aggregateId, Instant occurredAt, String roleCode)
        implements OrganizationDomainEvent {}
