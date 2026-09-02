package top.egon.cola.archetype.source.webopen.domain.user.events;

import top.egon.cola.archetype.source.webopen.domain.events.OrganizationDomainEvent;
import java.time.Instant;

public record UserChangedEvent(String eventId, Long aggregateId, Instant occurredAt, String changeType)
        implements OrganizationDomainEvent {}
