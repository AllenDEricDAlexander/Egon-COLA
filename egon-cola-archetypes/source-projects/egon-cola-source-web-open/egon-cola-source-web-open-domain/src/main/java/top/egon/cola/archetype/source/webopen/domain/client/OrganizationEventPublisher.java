package top.egon.cola.archetype.source.webopen.domain.client;

import top.egon.cola.archetype.source.webopen.domain.events.OrganizationDomainEvent;

public interface OrganizationEventPublisher {
    void publish(OrganizationDomainEvent event);
}
