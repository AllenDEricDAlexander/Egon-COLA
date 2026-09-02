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
