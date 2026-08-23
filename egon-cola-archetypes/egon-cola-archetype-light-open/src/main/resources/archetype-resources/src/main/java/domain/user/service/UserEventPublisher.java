package ${package}.domain.user.service;

import ${package}.domain.user.vos.UserEvent;

public interface UserEventPublisher {
    void publish(UserEvent event);
}
