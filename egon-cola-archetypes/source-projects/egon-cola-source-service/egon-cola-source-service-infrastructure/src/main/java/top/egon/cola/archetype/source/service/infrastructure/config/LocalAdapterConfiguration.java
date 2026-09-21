package top.egon.cola.archetype.source.service.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.archetype.source.service.infrastructure.mq.MqMessageService;
import top.egon.cola.archetype.source.service.infrastructure.mq.MqRouteEnum;

/**
 * Fallback wiring for a profile without the matching integration: the application still boots on
 * the same MQ boundary, only the external system is absent.
 */
@Configuration(proxyBeanMethods = false)
public class LocalAdapterConfiguration {

    @Bean("mqMessageService")
    @ConditionalOnProperty(
            name = "app.integrations.rabbitmq.enabled", havingValue = "false", matchIfMissing = true)
    MqMessageService mqMessageService() {
        return new LocalMqMessageService();
    }

    /** Records instead of publishing, so a broker-free profile keeps the same call graph. */
    @Slf4j
    static final class LocalMqMessageService implements MqMessageService {
        @Override
        public void publish(MqRouteEnum route, Object payload) {
            if (route.getPayloadType() == null || !route.accepts(payload)) {
                throw new IllegalStateException("MQ_ROUTE_REJECTED: " + route.name());
            }
            log.info("Local message {} on route {}", payload.getClass().getSimpleName(), route);
        }
    }
}
