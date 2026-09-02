package top.egon.cola.archetype.source.webopen.domain.teaching.events;

import top.egon.cola.archetype.source.webopen.domain.events.OrganizationDomainEvent;
import java.time.Instant;

public record SchoolClassChangedEvent(
        String eventId, Long aggregateId, Instant occurredAt, Long gradeId, String changeType)
        implements OrganizationDomainEvent {}
