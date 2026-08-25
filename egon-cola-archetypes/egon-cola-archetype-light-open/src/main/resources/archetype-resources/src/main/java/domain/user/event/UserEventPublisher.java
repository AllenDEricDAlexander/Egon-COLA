package ${package}.domain.user.event;

import ${package}.domain.user.vos.UserEvent;

/** Domain-event publication port for user changes. */
public interface UserEventPublisher {
    void publish(UserEvent event);
}
