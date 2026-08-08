package top.egon.cola.component.ddc.listener;

import org.redisson.api.RTopic;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis 配置变更订阅句柄。 Redis configuration-change subscription handle.
 *
 * <p>对象创建时向一个或多个 {@link RTopic} 注册同一个
 * {@link DdcRedisChangeListener}，并保存每次注册返回的监听器编号；应用停止时由
 * {@link #close()} 统一移除监听器。消息校验和配置刷新不在本类处理，均由监听器负责。
 * The handle registers the same {@link DdcRedisChangeListener} with one or more topics on creation,
 * stores each listener identifier, and removes them through {@link #close()}; validation and refresh remain listener responsibilities.</p>
 *
 * <p>如果注册过程中任一 Topic 失败，本类会移除此前已经注册成功的监听器，避免残留部分订阅。
 * If any topic registration fails, already registered listeners are removed to avoid a partial subscription.</p>
 */
public class DdcRedisChangeSubscription implements AutoCloseable {

    /** 已成功注册的 Topic 与监听器编号，用于关闭时精确解除订阅。 Successfully registered topics and listener identifiers used for precise removal on close. */
    private final List<Registration> registrations;

    /** 订阅是否尚未关闭；用于保证并发或重复关闭时只清理一次。 Whether the subscription remains open, ensuring concurrent or repeated close cleans up only once. */
    private final AtomicBoolean active = new AtomicBoolean(true);

    /**
     * 为单个 Topic 创建配置变更订阅。 Creates a configuration-change subscription for one topic.
     *
     * @param topic 需要订阅的 Redis Topic。 Redis topic to subscribe to
     * @param listener 配置变更消息监听器。 configuration-change message listener
     */
    public DdcRedisChangeSubscription(RTopic topic, DdcRedisChangeListener listener) {
        this(List.of(topic), listener);
    }

    /**
     * 为多个 Topic 创建配置变更订阅。 Creates a configuration-change subscription for multiple topics.
     *
     * <p>构造完成即表示全部监听器注册成功；如果其中一次注册抛出异常，已注册的监听器会先被移除，
     * 然后继续抛出原异常。 Construction completes only after all listeners are registered; on failure, prior registrations
     * are removed before the original exception is rethrown.</p>
     *
     * @param topics 需要订阅的 Redis Topic 列表。 Redis topics to subscribe to
     * @param listener 配置变更消息监听器。 configuration-change message listener
     * @throws RuntimeException 任一 Topic 注册监听器失败时抛出。 thrown when listener registration fails for any topic
     */
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

    /**
     * 返回当前订阅句柄是否尚未关闭。 Returns whether this subscription handle has not been closed.
     *
     * <p>该状态只表示本地监听器尚未被移除，不代表 Redis 连接一定可用。
     * This state only means local listeners have not been removed; it does not prove Redis connectivity.</p>
     *
     * @return 尚未执行成功关闭时返回 {@code true}。 {@code true} until a successful close begins
     */
    public boolean isActive() {
        return active.get();
    }

    /**
     * 移除本对象注册的全部监听器。 Removes every listener registered by this handle.
     *
     * <p>关闭操作是幂等的，并发或重复调用只会执行一次监听器清理。
     * Closing is idempotent, so concurrent or repeated calls perform listener cleanup only once.</p>
     */
    @Override
    public void close() {
        if (active.compareAndSet(true, false)) {
            registrations.forEach(Registration::remove);
        }
    }

    /**
     * 单次 Redis Topic 监听器注册信息。 Describes one Redis topic listener registration.
     *
     * @param topic 已注册监听器的 Redis Topic。 Redis topic holding the listener
     * @param listenerId Redisson 返回的监听器编号。 listener identifier returned by Redisson
     */
    private record Registration(RTopic topic, int listenerId) {

        /** 移除当前注册信息对应的监听器。 Removes the listener represented by this registration. */
        private void remove() {
            topic.removeListener(listenerId);
        }
    }
}
