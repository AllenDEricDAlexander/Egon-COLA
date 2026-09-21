package top.egon.cola.archetype.source.web.infrastructure.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.archetype.source.web.domain.events.OrganizationDomainEvent;
import top.egon.cola.archetype.source.web.domain.service.OrganizationEventService;
import top.egon.cola.archetype.source.web.infrastructure.mq.MqMessageService;
import top.egon.cola.archetype.source.web.infrastructure.mq.MqRouteEnum;
import top.egon.cola.archetype.source.web.infrastructure.mq.OrganizationEventMessage;

/** Resolves the declared event route and hands the wire carrier to the single MQ boundary. */
@Validated
@Service("organizationEventService")
@RequiredArgsConstructor
@Slf4j
public class OrganizationEventServiceImpl implements OrganizationEventService {
    @Qualifier("mqMessageService")
    private final MqMessageService mqMessageService;

    @Override
    public void publish(OrganizationDomainEvent event) {
        MqRouteEnum route = MqRouteEnum.requireEventRoute(event);
        mqMessageService.publish(route, new OrganizationEventMessage(
                event.eventId(), route.getEventType(), event.aggregateId(), event.occurredAt(),
                route.payloadOf(event)));
    }
}
