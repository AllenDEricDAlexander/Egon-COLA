#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.course.mq;

import ${package}.domain.course.entities.CourseSchedule;
import ${package}.domain.course.enums.CourseScheduleStatus;
import ${package}.domain.course.vos.CourseId;
import ${package}.infrastructure.course.mq.RabbitCourseEventPublisher;
import ${package}.infrastructure.course.mq.message.CourseScheduledMessage;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitCourseEventPublisherTest {

    @Test
    void shouldPublishCourseScheduledMessage() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        RabbitCourseEventPublisher publisher = new RabbitCourseEventPublisher(
                template, "evaluation.events", "course.scheduled");
        CourseSchedule schedule = new CourseSchedule(
                3001L, new CourseId(1001L), 2001L,
                Instant.EPOCH, Instant.EPOCH.plusSeconds(60), CourseScheduleStatus.SCHEDULED);

        publisher.courseScheduled(schedule);

        verify(template).convertAndSend(
                eq("evaluation.events"), eq("course.scheduled"),
                argThat((Object message) ->
                        ((CourseScheduledMessage) message).scheduleId() == 3001L));
    }
}
