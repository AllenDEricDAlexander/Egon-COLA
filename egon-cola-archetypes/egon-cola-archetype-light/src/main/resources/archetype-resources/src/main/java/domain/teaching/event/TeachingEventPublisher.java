package ${package}.domain.teaching.event;

import ${package}.domain.teaching.vos.TeachingEvent;

/** Domain-event publication port for teaching changes. */
public interface TeachingEventPublisher {
    void publish(TeachingEvent event);
}
