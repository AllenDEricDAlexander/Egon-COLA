package top.egon.cola.component.ddc.listener;

import org.redisson.api.RTopic;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;

import java.util.concurrent.atomic.AtomicBoolean;

public class DdcRedisChangeSubscription implements AutoCloseable {

    private final RTopic topic;

    private final int listenerId;

    private final AtomicBoolean active = new AtomicBoolean(true);

    public DdcRedisChangeSubscription(RTopic topic, DdcRedisChangeListener listener) {
        this.topic = topic;
        this.listenerId = topic.addListener(DdcPublishMessage.class, listener);
    }

    public boolean isActive() {
        return active.get();
    }

    @Override
    public void close() {
        if (active.compareAndSet(true, false)) {
            topic.removeListener(listenerId);
        }
    }
}
