package top.egon.cola.archetype.source.light.infrastructure.user.mq;

import top.egon.cola.archetype.source.light.domain.user.event.UserEventPublisher;
import top.egon.cola.archetype.source.light.domain.user.vos.UserEvent;
import top.egon.cola.archetype.source.light.infrastructure.config.TransactionCompletionExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("userEventPublisher")
@ConditionalOnProperty(name = "app.integrations.rabbitmq.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class RabbitUserEventPublisher implements UserEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final TransactionCompletionExecutor transactionCompletionExecutor;
    @Value("${app.integrations.rabbitmq.exchange}")
    private final String exchange;

    @Override
    public void publish(UserEvent event) {
        transactionCompletionExecutor.executeAfterCommit(
                () -> rabbitTemplate.convertAndSend(exchange, routingKey(event), event));
    }

    private String routingKey(UserEvent event) {
        return event.type().startsWith("authorization.")
                ? "authorization.changed"
                : "user.changed";
    }
}
