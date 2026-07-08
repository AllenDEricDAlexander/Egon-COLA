package top.egon.cola.component.accessguard.event;

import java.util.List;

public class NoopAccessGuardEventPublisher implements AccessGuardEventPublisher {

    private final List<AccessGuardEventListener> listeners;

    public NoopAccessGuardEventPublisher(List<AccessGuardEventListener> listeners) {
        this.listeners = List.copyOf(listeners);
    }

    @Override
    public void publish(AccessGuardEvent event) {
        listeners.forEach(listener -> listener.onEvent(event));
    }
}
