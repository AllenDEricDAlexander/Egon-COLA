package top.egon.cola.archetype.source.webopen.domain.teaching.events;

import top.egon.cola.archetype.source.webopen.domain.events.OrganizationDomainEvent;
import java.time.Instant;

public record GradeChangedEvent(String eventId, Long aggregateId, Instant occurredAt, String changeType)
        implements OrganizationDomainEvent {}
