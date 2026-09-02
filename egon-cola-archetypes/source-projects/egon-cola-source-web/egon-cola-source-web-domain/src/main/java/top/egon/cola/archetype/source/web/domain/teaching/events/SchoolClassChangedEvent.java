package top.egon.cola.archetype.source.web.domain.teaching.events;

import top.egon.cola.archetype.source.web.domain.events.OrganizationDomainEvent;
import java.time.Instant;

public record SchoolClassChangedEvent(
        String eventId, Long aggregateId, Instant occurredAt, Long gradeId, String changeType)
        implements OrganizationDomainEvent {}
