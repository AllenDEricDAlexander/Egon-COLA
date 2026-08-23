package ${package}.infrastructure.teaching.mq;

import ${package}.domain.teaching.service.TeachingEventPublisher;
import ${package}.domain.teaching.vos.TeachingEvent;
import ${package}.infrastructure.config.TransactionCompletionExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("teachingEventPublisher")
@ConditionalOnProperty(name = "app.integrations.rabbitmq.enabled", havingValue = "true")
@RequiredArgsConstructor
public class RabbitTeachingEventPublisher implements TeachingEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final TransactionCompletionExecutor transactionCompletionExecutor;
    @Value("${symbol_dollar}{app.integrations.rabbitmq.exchange}")
    private final String exchange;

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
