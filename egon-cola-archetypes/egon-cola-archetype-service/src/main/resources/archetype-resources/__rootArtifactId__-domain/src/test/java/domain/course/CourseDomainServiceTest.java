#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.course;

import ${package}.domain.common.EvaluationDomainException;
import ${package}.domain.course.entities.Course;
import ${package}.domain.course.entities.CourseSchedule;
import ${package}.domain.course.enums.CourseScheduleStatus;
import ${package}.domain.course.validators.CourseDomainValidator;
import ${package}.domain.course.vos.CourseCode;
import ${package}.domain.course.vos.CourseId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CourseDomainServiceTest {

    @Test
    void shouldNormalizeCourseCode() {
        Course course = Course.create(1001L, new CourseCode(" math-101 "), "Math", 3);

        assertEquals("MATH-101", course.getCode().value());
    }

    @Test
    void shouldRejectOverlappingSchedule() {
        Course course = Course.create(1001L, new CourseCode("MATH-101"), "Math", 3);
        CourseSchedule existing = new CourseSchedule(
                3001L, new CourseId(course.getId()), 2001L,
                Instant.parse("2026-09-01T01:00:00Z"),
                Instant.parse("2026-09-01T02:00:00Z"), CourseScheduleStatus.SCHEDULED);

        assertThrows(EvaluationDomainException.class, () -> new CourseDomainValidator().validateSchedule(
                course, 2001L,
                Instant.parse("2026-09-01T01:30:00Z"),
                Instant.parse("2026-09-01T02:30:00Z"), List.of(existing)));
    }
}
