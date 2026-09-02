package top.egon.cola.archetype.source.serviceopen.infrastructure.course.mq;

import top.egon.cola.archetype.source.serviceopen.domain.common.EvaluationPortException;
import top.egon.cola.archetype.source.serviceopen.domain.course.entities.CourseSchedule;
import top.egon.cola.archetype.source.serviceopen.domain.course.event.CourseEventPublisher;
import top.egon.cola.archetype.source.serviceopen.infrastructure.course.mq.message.CourseScheduledMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.integrations.rabbitmq", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class RabbitCourseEventPublisher implements CourseEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    @Value("${app.integrations.rabbitmq.exchange}")
    private final String exchange;
    @Value("${app.integrations.rabbitmq.course-scheduled-routing-key}")
    private final String routingKey;

    @Override
    public void courseScheduled(CourseSchedule schedule) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, new CourseScheduledMessage(
                    schedule.getId(), schedule.getCourseId().value(), schedule.getClassId(),
                    schedule.getStartsAt(), schedule.getEndsAt()));
        } catch (AmqpException failure) {
            throw new EvaluationPortException("publish course scheduled", "rabbitmq publish failed", failure);
        }
    }
}
