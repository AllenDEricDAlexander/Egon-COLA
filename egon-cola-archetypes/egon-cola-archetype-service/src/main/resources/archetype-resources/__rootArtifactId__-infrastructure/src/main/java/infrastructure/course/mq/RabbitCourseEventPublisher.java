#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.course.mq;

import ${package}.domain.common.EvaluationPortException;
import ${package}.domain.course.entities.CourseSchedule;
import ${package}.domain.course.event.CourseEventPublisher;
import ${package}.infrastructure.course.mq.message.CourseScheduledMessage;
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
    @Value("${symbol_dollar}{app.integrations.rabbitmq.exchange}")
    private final String exchange;
    @Value("${symbol_dollar}{app.integrations.rabbitmq.course-scheduled-routing-key}")
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
