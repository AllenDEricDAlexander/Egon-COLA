package top.egon.cola.archetype.source.webopen.infrastructure.mq.impl;

import io.micrometer.core.instrument.Metrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.archetype.source.webopen.infrastructure.mq.MqMessageService;
import top.egon.cola.archetype.source.webopen.infrastructure.mq.MqRouteEnum;
import top.egon.cola.archetype.source.webopen.infrastructure.mq.OrganizationEventMessage;
import top.egon.cola.archetype.source.webopen.infrastructure.mq.OrganizationEventProducer;

/**
 * Rabbit publication of a declared route, deferred to the commit point of the caller's
 * transaction. A post-commit broker failure stays a logged, counted loss: it must never roll the
 * committed database state back, which is exactly what the previous publisher guaranteed.
 */
@Validated
@Component("rabbitMqMessageService")
@ConditionalOnProperty(prefix = "organization.integrations.rabbit", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class RabbitMqMessageServiceImpl implements MqMessageService {
    @Qualifier("organizationEventProducer")
    private final OrganizationEventProducer producer;

    @Override
    public void publish(MqRouteEnum route, Object payload) {
        if (!MqRouteEnum.SCHEMA_VERSION.equals(route.getSchemaVersion()) || !route.accepts(payload)) {
            throw new IllegalStateException("MQ_ROUTE_REJECTED: " + route.name()
                    + " accepts " + route.getPayloadType() + " on schema " + route.getSchemaVersion());
        }
        OrganizationEventMessage message = (OrganizationEventMessage) payload;
        afterCommit(() -> {
            try {
                producer.send(route.getExchange(), route.getRoutingKey(), message);
            } catch (RuntimeException failure) {
                Metrics.counter("organization.event.publish.failures",
                        "eventType", route.getEventType()).increment();
                log.error("organization event publication failed eventId={} eventType={}",
                        message.eventId(), route.getEventType(), failure);
            }
        });
    }

    /** A rolled back transaction must never reach the broker, so the send joins the commit point. */
    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
