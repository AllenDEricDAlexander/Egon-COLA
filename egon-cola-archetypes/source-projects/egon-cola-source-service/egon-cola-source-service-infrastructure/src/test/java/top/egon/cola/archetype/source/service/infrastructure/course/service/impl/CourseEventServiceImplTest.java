package top.egon.cola.archetype.source.service.infrastructure.course.service.impl;

import org.junit.jupiter.api.Test;
import top.egon.cola.archetype.source.service.domain.course.entities.CourseSchedule;
import top.egon.cola.archetype.source.service.domain.course.enums.CourseScheduleStatus;
import top.egon.cola.archetype.source.service.domain.course.vos.CourseId;
import top.egon.cola.archetype.source.service.infrastructure.course.mq.message.CourseScheduledMessage;
import top.egon.cola.archetype.source.service.infrastructure.mq.MqRouteEnum;
import top.egon.cola.archetype.source.service.infrastructure.support.RecordingMqMessageService;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CourseEventServiceImplTest {

    private final RecordingMqMessageService messages = new RecordingMqMessageService();
    private final CourseEventServiceImpl service = new CourseEventServiceImpl(messages);

    @Test
    void shouldPublishCourseScheduledMessageOnTheDeclaredRoute() {
        service.courseScheduled(new CourseSchedule(
                3001L, new CourseId(1001L), 2001L,
                Instant.EPOCH, Instant.EPOCH.plusSeconds(60), CourseScheduleStatus.SCHEDULED));

        assertThat(messages.publications()).singleElement().satisfies(publication -> {
            assertThat(publication.route()).isEqualTo(MqRouteEnum.COURSE_SCHEDULED);
            assertThat(publication.payload()).isInstanceOf(CourseScheduledMessage.class);
            assertThat(((CourseScheduledMessage) publication.payload()).scheduleId()).isEqualTo(3001L);
        });
    }
}
