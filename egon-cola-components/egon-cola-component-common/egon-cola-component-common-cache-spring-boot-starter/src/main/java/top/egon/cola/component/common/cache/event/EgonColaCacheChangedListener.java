package top.egon.cola.component.common.cache.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.listener.MessageListener;
import org.springframework.context.SmartLifecycle;
import top.egon.cola.component.common.cache.autoconfigure.EgonColaCacheProperties;
import top.egon.cola.component.common.cache.codec.EgonColaCacheCodecs;
import top.egon.cola.component.common.cache.core.EgonColaTwoLevelCacheManager;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 全模块唯一缓存事件订阅点（REQ-003/PC-001）：容器启动后订阅通用变更信封，
 * 按操作枚举经 {@link EventBus} 通用分发到本地失效原语，杜绝 per-cache listener。
 * 任何解析或分发异常一律吞并记 ERROR，绝不逃逸 Redisson 回调线程。
 */
@Slf4j
@RequiredArgsConstructor
public class EgonColaCacheChangedListener implements SmartLifecycle {

    private static final ObjectMapper EVENT_MAPPER = EgonColaCacheCodecs.eventMapper();
    private static final int PAYLOAD_LOG_LIMIT = 200;

    private final EgonColaTwoLevelCacheManager manager;
    private final EgonColaCacheProperties properties;

    private final AtomicBoolean running = new AtomicBoolean();
    private volatile int listenerId;

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            MessageListener<String> bridge = (channel, message) -> onMessage(message);
            listenerId = manager.topic().addListener(String.class, bridge);
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            manager.topic().removeListener(listenerId);
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    void onMessage(String body) {
        try {
            EgonColaCacheChangedEvent event = EVENT_MAPPER.readValue(body, EgonColaCacheChangedEvent.class);
            if (manager.originNodeId().equals(event.originNodeId())) {
                return;
            }
            EventBus.apply(event, manager);
        } catch (Exception ex) {
            log.error("CACHE_EVENT_DESERIALIZE_FAILED payloadPrefix={}",
                    payloadPrefix(body), ex);
        }
    }

    private static String payloadPrefix(String body) {
        if (body == null) {
            return "-";
        }
        return body.length() <= PAYLOAD_LOG_LIMIT
                ? body : body.substring(0, PAYLOAD_LOG_LIMIT) + "...";
    }

    /**
     * 操作枚举的穷尽分发（Observer + 枚举 switch，Rule 9）：新增操作必须在此显式表态。
     */
    static final class EventBus {

        private EventBus() {
        }

        static void apply(EgonColaCacheChangedEvent event, EgonColaTwoLevelCacheManager target) {
            switch (event.operation()) {
                case EVICT -> target.applyLocalEviction(event.cacheName(), event.keys());
                case PREFIX_EVICT -> event.keys()
                        .forEach(glob -> target.applyLocalPrefixEviction(event.cacheName(), glob));
                case PUT -> target.applyRemotePut(event.cacheName(), event.keys());
            }
        }
    }
}
