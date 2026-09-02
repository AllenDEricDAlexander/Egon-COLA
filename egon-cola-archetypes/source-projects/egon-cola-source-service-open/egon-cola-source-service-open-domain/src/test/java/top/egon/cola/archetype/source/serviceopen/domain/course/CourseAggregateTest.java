package top.egon.cola.archetype.source.serviceopen.domain.course;

import top.egon.cola.archetype.source.serviceopen.domain.common.EvaluationDomainException;
import top.egon.cola.archetype.source.serviceopen.domain.course.entities.Course;
import top.egon.cola.archetype.source.serviceopen.domain.course.entities.CourseSchedule;
import top.egon.cola.archetype.source.serviceopen.domain.course.enums.CourseScheduleStatus;
import top.egon.cola.archetype.source.serviceopen.domain.course.validators.CourseDomainValidator;
import top.egon.cola.archetype.source.serviceopen.domain.course.vos.CourseCode;
import top.egon.cola.archetype.source.serviceopen.domain.course.vos.CourseId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CourseAggregateTest {

    private final CourseDomainValidator validator = new CourseDomainValidator();

    @Test
    void shouldRejectInvalidScheduleWindowAndBlankClass() {
        Course course = Course.create(1001L, new CourseCode("MATH-101"), "Math", 3);

        assertThrows(EvaluationDomainException.class, () -> validator.validateSchedule(
                course, 0L, Instant.EPOCH, Instant.EPOCH.plusSeconds(60), List.of()));
        assertThrows(EvaluationDomainException.class, () -> validator.validateSchedule(
                course, 2001L, Instant.EPOCH.plusSeconds(60), Instant.EPOCH, List.of()));
    }

    @Test
    void shouldAllowHalfOpenAdjacentSchedules() {
        Course course = Course.create(1001L, new CourseCode("MATH-101"), "Math", 3);
        var existing = new CourseSchedule(
                3001L, new CourseId(course.getId()), 2001L,
                Instant.EPOCH, Instant.EPOCH.plusSeconds(60), CourseScheduleStatus.SCHEDULED);

        assertDoesNotThrow(() -> validator.validateSchedule(
                course, 2001L, Instant.EPOCH.plusSeconds(60),
                Instant.EPOCH.plusSeconds(120), List.of(existing)));
    }
}
