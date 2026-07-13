package ${package}.domain.user.events;

import ${package}.domain.events.OrganizationDomainEvent;
import java.time.Instant;

public record PermissionGrantedEvent(
        String eventId, String aggregateId, Instant occurredAt, String roleCode, String permissionCode)
        implements OrganizationDomainEvent {}
