package ${package}.domain.teaching.service;

import ${package}.domain.teaching.vos.TeachingEvent;

public interface TeachingEventPublisher {
    void publish(TeachingEvent event);
}
