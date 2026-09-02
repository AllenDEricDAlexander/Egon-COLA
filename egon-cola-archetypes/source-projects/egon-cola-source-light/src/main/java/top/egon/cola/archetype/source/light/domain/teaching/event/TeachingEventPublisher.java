package top.egon.cola.archetype.source.light.domain.teaching.event;

import top.egon.cola.archetype.source.light.domain.teaching.vos.TeachingEvent;

/** Domain-event publication port for teaching changes. */
public interface TeachingEventPublisher {
    void publish(TeachingEvent event);
}
