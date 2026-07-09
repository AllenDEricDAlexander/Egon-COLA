package ${package}.infrastructure.teaching.mq;

import ${package}.domain.teaching.service.TeachingEventPublisher;
import ${package}.domain.teaching.vos.TeachingEvent;
import ${package}.infrastructure.config.TransactionCompletionExecutor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("teachingEventPublisher")
@ConditionalOnProperty(name = "app.integrations.rabbitmq.enabled", havingValue = "true")
public class RabbitTeachingEventPublisher implements TeachingEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final TransactionCompletionExecutor transactionCompletionExecutor;
    private final String exchange;

    public RabbitTeachingEventPublisher(
            RabbitTemplate rabbitTemplate,
            TransactionCompletionExecutor transactionCompletionExecutor,
            @Value("${symbol_dollar}{app.integrations.rabbitmq.exchange}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.transactionCompletionExecutor = transactionCompletionExecutor;
        this.exchange = exchange;
    }

    @Override
    public void publish(TeachingEvent event) {
        transactionCompletionExecutor.executeAfterCommit(
                () -> rabbitTemplate.convertAndSend(exchange, routingKey(event), event));
    }

    private String routingKey(TeachingEvent event) {
        if (event.type().startsWith("class.")) {
            return "class.changed";
        }
        if (event.type().startsWith("course.")) {
            return "course.changed";
        }
        return "schedule.changed";
    }
}
