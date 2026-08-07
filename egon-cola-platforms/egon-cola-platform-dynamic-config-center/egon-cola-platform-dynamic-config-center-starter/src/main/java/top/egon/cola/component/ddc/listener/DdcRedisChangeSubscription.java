package top.egon.cola.component.ddc.listener;

import org.redisson.api.RTopic;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis 配置变更订阅句柄。
 *
 * <p>对象创建时向一个或多个 {@link RTopic} 注册同一个
 * {@link DdcRedisChangeListener}，并保存每次注册返回的监听器编号；应用停止时由
 * {@link #close()} 统一移除监听器。消息校验和配置刷新不在本类处理，均由监听器负责。</p>
 *
 * <p>如果注册过程中任一 Topic 失败，本类会移除此前已经注册成功的监听器，避免残留部分订阅。</p>
 */
public class DdcRedisChangeSubscription implements AutoCloseable {

    /** 已成功注册的 Topic 与监听器编号，用于关闭时精确解除订阅。 */
    private final List<Registration> registrations;

    /** 订阅是否尚未关闭；用于保证并发或重复关闭时只清理一次。 */
    private final AtomicBoolean active = new AtomicBoolean(true);

    /**
     * 为单个 Topic 创建配置变更订阅。
     *
     * @param topic    需要订阅的 Redis Topic
     * @param listener 配置变更消息监听器
     */
    public DdcRedisChangeSubscription(RTopic topic, DdcRedisChangeListener listener) {
        this(List.of(topic), listener);
    }

    /**
     * 为多个 Topic 创建配置变更订阅。
     *
     * <p>构造完成即表示全部监听器注册成功；如果其中一次注册抛出异常，已注册的监听器会先被移除，
     * 然后继续抛出原异常。</p>
     *
     * @param topics   需要订阅的 Redis Topic 列表
     * @param listener 配置变更消息监听器
     * @throws RuntimeException 任一 Topic 注册监听器失败时抛出
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
     * 返回当前订阅句柄是否尚未关闭。
     *
     * <p>该状态只表示本地监听器尚未被移除，不代表 Redis 连接一定可用。</p>
     *
     * @return 尚未执行成功关闭时返回 {@code true}
     */
    public boolean isActive() {
        return active.get();
    }

    /**
     * 移除本对象注册的全部监听器。
     *
     * <p>关闭操作是幂等的，并发或重复调用只会执行一次监听器清理。</p>
     */
    @Override
    public void close() {
        if (active.compareAndSet(true, false)) {
            registrations.forEach(Registration::remove);
        }
    }

    /**
     * 单次 Redis Topic 监听器注册信息。
     *
     * @param topic      已注册监听器的 Redis Topic
     * @param listenerId Redisson 返回的监听器编号
     */
    private record Registration(RTopic topic, int listenerId) {

        /** 移除当前注册信息对应的监听器。 */
        private void remove() {
            topic.removeListener(listenerId);
        }
    }
}
