#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.course.repo;

import ${package}.domain.course.entities.CourseSchedule;
import ${package}.domain.course.enums.CourseScheduleStatus;
import ${package}.domain.course.vos.CourseId;
import ${package}.infrastructure.course.repo.converter.CourseScheduleConverter;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CourseScheduleRepositoryTest {
    @Test
    void shouldRoundTripCourseSchedulePersistenceModel() {
        CourseSchedule schedule = new CourseSchedule(
                3001L, new CourseId(1001L), 2001L,
                Instant.EPOCH, Instant.EPOCH.plusSeconds(60), CourseScheduleStatus.SCHEDULED);
        CourseScheduleConverter converter = Mappers.getMapper(CourseScheduleConverter.class);
        var target = converter.toTarget(schedule);
        target.setId(schedule.getId());
        CourseSchedule restored = converter.toSource(target);
        assertEquals(3001L, restored.getId());
        assertEquals(1001L, restored.getCourseId().value());
    }
}
