package ${package}.domain.client;

import ${package}.domain.events.OrganizationDomainEvent;

public interface OrganizationEventPublisher {
    void publish(OrganizationDomainEvent event);
}
