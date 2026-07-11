#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.course;

import ${package}.domain.entities.course.CourseSchedule;
import ${package}.domain.enums.course.CourseScheduleStatus;
import ${package}.domain.vos.course.CourseId;
import ${package}.infrastructure.repo.course.converter.CourseScheduleConverter;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CourseScheduleRepositoryTest {

    @Test
    void shouldRoundTripCourseSchedulePersistenceModel() {
        CourseSchedule schedule = new CourseSchedule(
                "schedule-1", new CourseId("course-1"), "class-1",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(60), CourseScheduleStatus.SCHEDULED);
        CourseScheduleConverter converter = new CourseScheduleConverter();

        CourseSchedule restored = converter.toDomain(converter.toPo(schedule));

        assertEquals("schedule-1", restored.getId());
        assertEquals(new CourseId("course-1"), restored.getCourseId());
        assertEquals(CourseScheduleStatus.SCHEDULED, restored.getStatus());
    }
}
