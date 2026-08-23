#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.course;

import ${package}.domain.common.EvaluationDomainException;
import ${package}.domain.course.entities.Course;
import ${package}.domain.course.entities.CourseSchedule;
import ${package}.domain.course.service.impl.CourseDomainServiceImpl;
import ${package}.domain.course.vos.CourseCode;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CourseDomainServiceTest {

    private final CourseDomainServiceImpl service = new CourseDomainServiceImpl();

    @Test
    void shouldNormalizeCourseCode() {
        Course course = service.createCourse(
                "course-1", new CourseCode(" math-101 "), "Math", 3);

        assertEquals("MATH-101", course.getCode().value());
    }

    @Test
    void shouldRejectOverlappingSchedule() {
        Course course = service.createCourse(
                "course-1", new CourseCode("MATH-101"), "Math", 3);
        CourseSchedule existing = service.scheduleCourse(
                "schedule-1", course, "class-1",
                Instant.parse("2026-09-01T01:00:00Z"),
                Instant.parse("2026-09-01T02:00:00Z"), List.of());

        assertThrows(EvaluationDomainException.class, () -> service.scheduleCourse(
                "schedule-2", course, "class-1",
                Instant.parse("2026-09-01T01:30:00Z"),
                Instant.parse("2026-09-01T02:30:00Z"), List.of(existing)));
    }
}
