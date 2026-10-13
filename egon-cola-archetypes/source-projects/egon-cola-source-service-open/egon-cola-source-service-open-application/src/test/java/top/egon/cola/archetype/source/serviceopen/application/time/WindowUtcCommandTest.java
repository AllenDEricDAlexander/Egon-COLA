package top.egon.cola.archetype.source.serviceopen.application.time;

import jakarta.validation.Validation;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import top.egon.cola.archetype.source.serviceopen.application.course.pojo.command.ScheduleCourseCommand;
import top.egon.cola.archetype.source.serviceopen.application.exam.pojo.command.CreateExamCommand;
import top.egon.cola.archetype.source.serviceopen.domain.course.entities.Course;
import top.egon.cola.archetype.source.serviceopen.domain.course.vos.CourseCode;
import top.egon.cola.archetype.source.serviceopen.domain.course.validators.CourseDomainValidator;
import top.egon.cola.archetype.source.serviceopen.domain.exam.validators.ExamDomainValidator;
import top.egon.cola.archetype.source.serviceopen.common.exception.EvaluationDomainException;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import static org.junit.jupiter.api.Assertions.*;

class WindowUtcCommandTest {

    @Test
    void commandsCanonicalizeOffsetsAndNanoseconds() {
        Instant raw = OffsetDateTime.parse("2026-10-13T09:00:00.123456789+08:00").toInstant();
        Instant expected = Instant.parse("2026-10-13T01:00:00.123456Z");
        var schedule = new ScheduleCourseCommand(1001L, 2001L, raw, raw.plusSeconds(60));
        var exam = new CreateExamCommand(1001L, "Midterm", raw, raw.plusSeconds(60));
        assertEquals(expected, schedule.startsAt());
        assertEquals(expected.plusSeconds(60), schedule.endsAt());
        assertEquals(expected, exam.startsAt());
        assertEquals(expected.plusSeconds(60), exam.endsAt());
        Instant dstFirst = OffsetDateTime.parse("2026-11-01T01:30:00-04:00").toInstant();
        Instant dstSecond = OffsetDateTime.parse("2026-11-01T01:30:00-05:00").toInstant();
        assertEquals(3600, java.time.Duration.between(dstFirst, dstSecond).getSeconds());
        assertEquals(dstFirst, new ScheduleCourseCommand(1001L, 2001L, dstFirst, dstSecond).startsAt());
    }

    @Test
    void canonicalZeroLengthAndReversedWindowsReachExistingDomainGuards() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var utils = new ValidationUtils(factory.getValidator());
            var course = Course.create(1001L, new CourseCode("MATH-101"), "Math", 3);
            var schedules = new CourseDomainValidator(utils);
            var exams = new ExamDomainValidator(utils);
            Instant start = Instant.parse("2026-10-13T01:00:00.123456100Z");
            for (Instant end : List.of(start.plusNanos(100), start.minusSeconds(1))) {
                var schedule = new ScheduleCourseCommand(1001L, 2001L, start, end);
                var exam = new CreateExamCommand(1001L, "Midterm", start, end);
                assertThrows(EvaluationDomainException.class, () -> schedules.validateSchedule(
                        course, schedule.classId(), schedule.startsAt(), schedule.endsAt(), List.of()));
                assertThrows(EvaluationDomainException.class, () -> exams.validateExam(
                        course, exam.title(), exam.startsAt(), exam.endsAt()));
            }
        }
    }

    @Test
    void nullTimesRemainNativeConstraintViolations() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var schedule = new ScheduleCourseCommand(1001L, 2001L, null, null);
            var exam = new CreateExamCommand(1001L, "Midterm", null, null);
            assertNull(schedule.startsAt());
            assertNull(exam.endsAt());
            assertEquals(2, factory.getValidator().validate(schedule).size());
            assertEquals(2, factory.getValidator().validate(exam).size());
        }
    }
}
