package top.egon.cola.archetype.source.light.domain.user.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import top.egon.cola.archetype.source.light.domain.user.vos.UserEvent;

/** User domain-event publication capability. */
public interface UserEventService {
    void publish(@Valid @NotNull UserEvent event);
}
