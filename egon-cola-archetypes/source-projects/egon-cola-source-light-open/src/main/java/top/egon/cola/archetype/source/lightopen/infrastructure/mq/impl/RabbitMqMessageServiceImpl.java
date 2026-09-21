package top.egon.cola.archetype.source.lightopen.infrastructure.mq.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.archetype.source.lightopen.infrastructure.config.TransactionCompletionExecutor;
import top.egon.cola.archetype.source.lightopen.infrastructure.mq.MqMessageService;
import top.egon.cola.archetype.source.lightopen.infrastructure.mq.MqRouteEnum;

/** Rabbit publication of a declared route, deferred to the commit point of the caller's transaction. */
@Validated
@Component("rabbitMqMessageService")
@ConditionalOnProperty(name = "app.integrations.rabbitmq.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class RabbitMqMessageServiceImpl implements MqMessageService {
    @Qualifier("rabbitTemplate")
    private final RabbitTemplate rabbitTemplate;
    @Qualifier("transactionCompletionExecutor")
    private final TransactionCompletionExecutor transactionCompletionExecutor;
    @Qualifier("environment")
    private final Environment environment;

    @Override
    public void publish(MqRouteEnum route, Object payload) {
        if (!MqRouteEnum.SCHEMA_VERSION.equals(route.getSchemaVersion()) || !route.accepts(payload)) {
            throw new IllegalStateException("MQ_ROUTE_REJECTED: " + route.name()
                    + " accepts " + route.getPayloadType().getSimpleName()
                    + " on schema " + route.getSchemaVersion());
        }
        String exchange = environment.getProperty(route.getExchangeProperty());
        transactionCompletionExecutor.executeAfterCommit(
                () -> rabbitTemplate.convertAndSend(exchange, route.getRoutingKey(), payload));
    }
}
