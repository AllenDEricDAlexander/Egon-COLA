#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.mq;

import ${package}.domain.course.entities.CourseSchedule;
import ${package}.domain.course.enums.CourseScheduleStatus;
import ${package}.domain.course.vos.CourseId;
import ${package}.infrastructure.mq.course.RabbitCourseEventPublisher;
import ${package}.infrastructure.mq.message.CourseScheduledMessage;
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
                "schedule-1", new CourseId("course-1"), "class-1",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(60), CourseScheduleStatus.SCHEDULED);

        publisher.courseScheduled(schedule);

        verify(template).convertAndSend(
                eq("evaluation.events"), eq("course.scheduled"),
                argThat((Object message) ->
                        ((CourseScheduledMessage) message).scheduleId().equals("schedule-1")));
    }
}
