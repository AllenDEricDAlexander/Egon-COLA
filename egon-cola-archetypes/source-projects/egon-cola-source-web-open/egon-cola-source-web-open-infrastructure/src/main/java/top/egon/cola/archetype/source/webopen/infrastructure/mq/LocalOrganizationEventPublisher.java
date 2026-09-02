package top.egon.cola.archetype.source.webopen.infrastructure.mq;

import top.egon.cola.archetype.source.webopen.domain.client.OrganizationEventPublisher;
import top.egon.cola.archetype.source.webopen.domain.events.OrganizationDomainEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class LocalOrganizationEventPublisher implements OrganizationEventPublisher {
    private final CopyOnWriteArrayList<OrganizationDomainEvent> events = new CopyOnWriteArrayList<>();
    @Override public void publish(OrganizationDomainEvent event) { events.add(event); }
    public List<OrganizationDomainEvent> publishedEvents() { return List.copyOf(events); }
    public List<OrganizationDomainEvent> events() { return publishedEvents(); }
    public void clear() { events.clear(); }
}
