package top.egon.cola.archetype.source.webopen.domain.service;

import jakarta.validation.constraints.NotNull;
import top.egon.cola.archetype.source.webopen.domain.events.OrganizationDomainEvent;

/** Domain-facing publication port for organization events; the transport sits behind Infrastructure. */
public interface OrganizationEventService {

    void publish(@NotNull OrganizationDomainEvent event);
}
