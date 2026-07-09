package ${package}.infrastructure.user.mq;

import ${package}.domain.user.service.UserEventPublisher;
import ${package}.domain.user.vos.UserEvent;
import ${package}.infrastructure.config.TransactionCompletionExecutor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("userEventPublisher")
@ConditionalOnProperty(name = "app.integrations.rabbitmq.enabled", havingValue = "true")
public class RabbitUserEventPublisher implements UserEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final TransactionCompletionExecutor transactionCompletionExecutor;
    private final String exchange;

    public RabbitUserEventPublisher(
            RabbitTemplate rabbitTemplate,
            TransactionCompletionExecutor transactionCompletionExecutor,
            @Value("${symbol_dollar}{app.integrations.rabbitmq.exchange}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.transactionCompletionExecutor = transactionCompletionExecutor;
        this.exchange = exchange;
    }

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
