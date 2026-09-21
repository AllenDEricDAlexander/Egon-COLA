package top.egon.cola.archetype.source.serviceopen.infrastructure.mq.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.archetype.source.serviceopen.common.exception.EvaluationPortException;
import top.egon.cola.archetype.source.serviceopen.infrastructure.mq.MqMessageService;
import top.egon.cola.archetype.source.serviceopen.infrastructure.mq.MqRouteEnum;

/** Rabbit publication of a declared route, deferred to the commit point of the caller's transaction. */
@Validated
@Component("rabbitMqMessageService")
@ConditionalOnProperty(prefix = "app.integrations.rabbitmq", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class RabbitMqMessageServiceImpl implements MqMessageService {
    @Qualifier("rabbitTemplate")
    private final RabbitTemplate rabbitTemplate;
    @Qualifier("environment")
    private final Environment environment;

    @Override
    public void publish(MqRouteEnum route, Object payload) {
        if (!MqRouteEnum.SCHEMA_VERSION.equals(route.getSchemaVersion()) || !route.accepts(payload)) {
            throw new IllegalStateException("MQ_ROUTE_REJECTED: " + route.name()
                    + " accepts " + route.getPayloadType() + " on schema " + route.getSchemaVersion());
        }
        String exchange = environment.getProperty(route.getExchangeProperty());
        String routingKey = environment.getProperty(route.getRoutingKeyProperty());
        afterCommit(() -> {
            try {
                rabbitTemplate.convertAndSend(exchange, routingKey, payload);
            } catch (AmqpException failure) {
                throw new EvaluationPortException(
                        "publish " + route.getMessage(), "rabbitmq publish failed", failure);
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
