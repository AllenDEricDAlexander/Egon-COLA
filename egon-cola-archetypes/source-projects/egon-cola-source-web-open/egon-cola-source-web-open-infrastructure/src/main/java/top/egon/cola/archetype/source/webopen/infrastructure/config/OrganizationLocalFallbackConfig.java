package top.egon.cola.archetype.source.webopen.infrastructure.config;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.archetype.source.webopen.domain.service.CommandIdempotencyService;
import top.egon.cola.archetype.source.webopen.infrastructure.mq.MqMessageService;
import top.egon.cola.archetype.source.webopen.infrastructure.mq.MqRouteEnum;
import top.egon.cola.archetype.source.webopen.infrastructure.service.impl.InMemoryCommandIdempotencyServiceImpl;

/**
 * Fallback wiring for a profile without the matching integration: the application still boots on
 * the same Domain Service and MQ boundaries, only the external system is absent.
 */
@Configuration(proxyBeanMethods = false)
public class OrganizationLocalFallbackConfig {

    @Bean("commandIdempotencyService")
    @ConditionalOnProperty(prefix = "organization.integrations.redis", name = "enabled",
            havingValue = "false", matchIfMissing = true)
    CommandIdempotencyService commandIdempotencyService() {
        return new InMemoryCommandIdempotencyServiceImpl();
    }

    @Bean("mqMessageService")
    @ConditionalOnProperty(prefix = "organization.integrations.rabbit", name = "enabled",
            havingValue = "false", matchIfMissing = true)
    MqMessageService mqMessageService() {
        return new LocalMqMessageService();
    }

    /** Records instead of publishing, so a broker-free profile keeps the same call graph. */
    @Slf4j
    public static final class LocalMqMessageService implements MqMessageService {
        private final List<PublishedMessage> published = new CopyOnWriteArrayList<>();

        @Override
        public void publish(MqRouteEnum route, Object payload) {
            if (route.getPayloadType() == null || !route.accepts(payload)) {
                throw new IllegalStateException("MQ_ROUTE_REJECTED: " + route.name());
            }
            published.add(new PublishedMessage(route, payload));
            log.info("Local message {} on route {}", payload.getClass().getSimpleName(), route);
        }

        public List<PublishedMessage> publishedMessages() {
            return List.copyOf(published);
        }

        public void clear() {
            published.clear();
        }
    }

    /** One recorded publication, so a test can assert the route and the wire carrier together. */
    public record PublishedMessage(MqRouteEnum route, Object payload) {
    }
}
