package top.egon.cola.component.ddc.transport.redis;

import org.redisson.api.RTopic;
import org.redisson.api.listener.MessageListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 管理一个消息类型在一个或多个 Redis Topic 上的监听器生命周期。 /
 * Manages the listener lifecycle for one message type across one or more Redis topics.
 *
 * @param <T> Redis 消息类型 / Redis message type
 */
public final class DdcRedisTopicSubscription<T> implements AutoCloseable {

    private final List<Registration> registrations;

    private final AtomicBoolean active = new AtomicBoolean(true);

    /**
     * 注册全部 Topic；任一注册失败时回滚已经完成的监听器注册。 /
     * Registers all topics and rolls back completed listener registrations if any registration fails.
     *
     * @param topics      Redis Topic 列表 / Redis topics
     * @param messageType 消息类型 / message type
     * @param listener    消息监听器 / message listener
     */
    public DdcRedisTopicSubscription(List<RTopic> topics,
                                     Class<T> messageType,
                                     MessageListener<T> listener) {
        Objects.requireNonNull(topics, "topics");
        Objects.requireNonNull(messageType, "messageType");
        Objects.requireNonNull(listener, "listener");
        List<Registration> registered = new ArrayList<>();
        try {
            for (RTopic topic : topics) {
                RTopic requiredTopic = Objects.requireNonNull(topic, "topic");
                registered.add(new Registration(
                        requiredTopic,
                        requiredTopic.addListener(messageType, listener)
                ));
            }
        } catch (RuntimeException exception) {
            registered.forEach(Registration::remove);
            throw exception;
        }
        this.registrations = List.copyOf(registered);
    }

    /**
     * 返回本地监听器是否尚未移除。 / Returns whether local listeners have not yet been removed.
     *
     * @return 订阅句柄仍活跃时为 {@code true} / {@code true} while the handle remains active
     */
    public boolean isActive() {
        return active.get();
    }

    /**
     * 幂等移除本句柄注册的全部监听器。 / Idempotently removes every listener registered by this handle.
     */
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
