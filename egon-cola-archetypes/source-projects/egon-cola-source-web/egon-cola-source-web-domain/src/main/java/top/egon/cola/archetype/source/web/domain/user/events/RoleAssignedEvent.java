package top.egon.cola.archetype.source.web.domain.user.events;

import top.egon.cola.archetype.source.web.domain.events.OrganizationDomainEvent;
import java.time.Instant;

public record RoleAssignedEvent(String eventId, Long aggregateId, Instant occurredAt, String roleCode)
        implements OrganizationDomainEvent {}
