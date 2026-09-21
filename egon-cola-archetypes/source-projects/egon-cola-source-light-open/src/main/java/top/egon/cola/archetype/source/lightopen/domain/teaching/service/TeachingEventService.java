package top.egon.cola.archetype.source.lightopen.domain.teaching.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.TeachingEvent;

/** Teaching domain-event publication capability. */
public interface TeachingEventService {
    void publish(@Valid @NotNull TeachingEvent event);
}
