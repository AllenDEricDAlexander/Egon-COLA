package top.egon.cola.archetype.source.service.infrastructure.course.repo;

import top.egon.cola.archetype.source.service.domain.course.entities.CourseSchedule;
import top.egon.cola.archetype.source.service.domain.course.enums.CourseScheduleStatus;
import top.egon.cola.archetype.source.service.domain.course.vos.CourseId;
import top.egon.cola.archetype.source.service.infrastructure.course.repo.converter.CourseScheduleConverter;
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
