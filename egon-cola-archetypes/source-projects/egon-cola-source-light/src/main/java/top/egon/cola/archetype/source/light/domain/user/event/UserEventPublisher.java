package top.egon.cola.archetype.source.light.domain.user.event;

import top.egon.cola.archetype.source.light.domain.user.vos.UserEvent;

/** Domain-event publication port for user changes. */
public interface UserEventPublisher {
    void publish(UserEvent event);
}
