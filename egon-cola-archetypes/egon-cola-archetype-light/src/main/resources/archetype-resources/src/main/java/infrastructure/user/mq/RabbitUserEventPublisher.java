package ${package}.infrastructure.user.mq;

import ${package}.domain.user.service.UserEventPublisher;
import ${package}.domain.user.vos.UserEvent;
import ${package}.infrastructure.config.TransactionCompletionExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("userEventPublisher")
@ConditionalOnProperty(name = "app.integrations.rabbitmq.enabled", havingValue = "true")
@RequiredArgsConstructor
public class RabbitUserEventPublisher implements UserEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final TransactionCompletionExecutor transactionCompletionExecutor;
    @Value("${symbol_dollar}{app.integrations.rabbitmq.exchange}")
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
