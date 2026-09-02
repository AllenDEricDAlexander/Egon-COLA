package top.egon.cola.archetype.source.web.domain.client;

import top.egon.cola.archetype.source.web.domain.events.OrganizationDomainEvent;

public interface OrganizationEventPublisher {
    void publish(OrganizationDomainEvent event);
}
