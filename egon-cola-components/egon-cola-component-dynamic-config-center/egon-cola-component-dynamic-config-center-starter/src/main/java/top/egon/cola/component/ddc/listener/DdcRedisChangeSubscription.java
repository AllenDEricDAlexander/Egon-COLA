package top.egon.cola.component.ddc.listener;

import org.redisson.api.RTopic;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class DdcRedisChangeSubscription implements AutoCloseable {

    private final List<Registration> registrations;

    private final AtomicBoolean active = new AtomicBoolean(true);

    public DdcRedisChangeSubscription(RTopic topic, DdcRedisChangeListener listener) {
        this(List.of(topic), listener);
    }

    public DdcRedisChangeSubscription(List<RTopic> topics, DdcRedisChangeListener listener) {
        List<Registration> registered = new ArrayList<>();
        try {
            for (RTopic topic : topics) {
                registered.add(new Registration(
                        topic,
                        topic.addListener(DdcPublishMessage.class, listener)
                ));
            }
        } catch (RuntimeException exception) {
            registered.forEach(Registration::remove);
            throw exception;
        }
        this.registrations = List.copyOf(registered);
    }

    public boolean isActive() {
        return active.get();
    }

    @Override
    public void close() {
        if (active.compareAndSet(true, false)) {
            registrations.forEach(Registration::remove);
        }
    }

    private record Registration(RTopic topic, int listenerId) {

        private void remove() {
            topic.removeListener(listenerId);
        }
    }
}
