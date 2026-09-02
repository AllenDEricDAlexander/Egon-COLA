package top.egon.cola.archetype.source.light.infrastructure.teaching.mq;

import top.egon.cola.archetype.source.light.domain.teaching.event.TeachingEventPublisher;
import top.egon.cola.archetype.source.light.domain.teaching.vos.TeachingEvent;
import top.egon.cola.archetype.source.light.infrastructure.config.TransactionCompletionExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("teachingEventPublisher")
@ConditionalOnProperty(name = "app.integrations.rabbitmq.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class RabbitTeachingEventPublisher implements TeachingEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final TransactionCompletionExecutor transactionCompletionExecutor;
    @Value("${app.integrations.rabbitmq.exchange}")
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
