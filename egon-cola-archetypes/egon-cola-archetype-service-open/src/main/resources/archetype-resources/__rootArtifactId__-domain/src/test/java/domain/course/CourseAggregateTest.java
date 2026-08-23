#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.course;

import ${package}.domain.common.EvaluationDomainException;
import ${package}.domain.course.entities.Course;
import ${package}.domain.course.service.impl.CourseDomainServiceImpl;
import ${package}.domain.course.vos.CourseCode;
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
                1L, new CourseCode("MATH-101"), "Math", 3);

        assertThrows(EvaluationDomainException.class, () -> service.scheduleCourse(
                1L, course, 0L, Instant.EPOCH, Instant.EPOCH.plusSeconds(60), List.of()));
        assertThrows(EvaluationDomainException.class, () -> service.scheduleCourse(
                1L, course, 1L, Instant.EPOCH.plusSeconds(60), Instant.EPOCH, List.of()));
    }

    @Test
    void shouldAllowHalfOpenAdjacentSchedules() {
        Course course = service.createCourse(
                1L, new CourseCode("MATH-101"), "Math", 3);
        var existing = service.scheduleCourse(
                1L, course, 1L, Instant.EPOCH, Instant.EPOCH.plusSeconds(60), List.of());

        assertDoesNotThrow(() -> service.scheduleCourse(
                2L, course, 1L,
                Instant.EPOCH.plusSeconds(60), Instant.EPOCH.plusSeconds(120), List.of(existing)));
    }
}
