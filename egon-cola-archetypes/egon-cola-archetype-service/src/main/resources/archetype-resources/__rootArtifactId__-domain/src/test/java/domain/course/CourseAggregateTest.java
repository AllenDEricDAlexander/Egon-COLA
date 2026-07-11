#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.course;

import ${package}.domain.common.EvaluationDomainException;
import ${package}.domain.entities.course.Course;
import ${package}.domain.service.course.impl.CourseDomainServiceImpl;
import ${package}.domain.vos.course.CourseCode;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CourseAggregateTest {

    private final CourseDomainServiceImpl service = new CourseDomainServiceImpl();

    @Test
    void shouldRejectInvalidScheduleWindowAndBlankClass() {
        Course course = service.createCourse(
                "course-1", new CourseCode("MATH-101"), "Math", 3);

        assertThrows(EvaluationDomainException.class, () -> service.scheduleCourse(
                "schedule-1", course, " ", Instant.EPOCH, Instant.EPOCH.plusSeconds(60), List.of()));
        assertThrows(EvaluationDomainException.class, () -> service.scheduleCourse(
                "schedule-1", course, "class-1", Instant.EPOCH.plusSeconds(60), Instant.EPOCH, List.of()));
    }

    @Test
    void shouldAllowHalfOpenAdjacentSchedules() {
        Course course = service.createCourse(
                "course-1", new CourseCode("MATH-101"), "Math", 3);
        var existing = service.scheduleCourse(
                "schedule-1", course, "class-1", Instant.EPOCH, Instant.EPOCH.plusSeconds(60), List.of());

        assertDoesNotThrow(() -> service.scheduleCourse(
                "schedule-2", course, "class-1",
                Instant.EPOCH.plusSeconds(60), Instant.EPOCH.plusSeconds(120), List.of(existing)));
    }
}
