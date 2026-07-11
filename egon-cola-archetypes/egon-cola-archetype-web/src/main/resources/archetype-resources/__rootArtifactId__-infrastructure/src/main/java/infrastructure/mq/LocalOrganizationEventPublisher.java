package ${package}.infrastructure.mq;

import ${package}.domain.client.OrganizationEventPublisher;
import ${package}.domain.events.OrganizationDomainEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class LocalOrganizationEventPublisher implements OrganizationEventPublisher {
    private final CopyOnWriteArrayList<OrganizationDomainEvent> events = new CopyOnWriteArrayList<>();
    @Override public void publish(OrganizationDomainEvent event) { events.add(event); }
    public List<OrganizationDomainEvent> publishedEvents() { return List.copyOf(events); }
    public List<OrganizationDomainEvent> events() { return publishedEvents(); }
    public void clear() { events.clear(); }
}
