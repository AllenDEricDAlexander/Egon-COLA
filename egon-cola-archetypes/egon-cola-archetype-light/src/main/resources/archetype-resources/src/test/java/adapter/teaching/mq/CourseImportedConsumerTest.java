package ${package}.adapter.teaching.mq;

import ${package}.adapter.teaching.validators.TeachingRequestValidator;
import ${package}.application.teaching.command.CreateCourseCommand;
import ${package}.application.teaching.manage.CourseManage;
import ${package}.facade.teaching.dto.CreateCourseDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CourseImportedConsumerTest {
    @Test
    void maps_actor_and_message_id() {
        CourseManage manage = mock(CourseManage.class);
        CourseImportedConsumer consumer = new CourseImportedConsumer(manage, new TeachingRequestValidator());
        consumer.consume(new CreateCourseDTO("MATH", "Math", "actor-1", "message-1"));
        ArgumentCaptor<CreateCourseCommand> captor = ArgumentCaptor.forClass(CreateCourseCommand.class);
        verify(manage).create(captor.capture());
        assertThat(captor.getValue().operatorId()).isEqualTo("actor-1");
        assertThat(captor.getValue().idempotencyKey()).isEqualTo("message-1");
    }
}
