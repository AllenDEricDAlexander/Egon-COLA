package top.egon.cola.archetype.source.light.domain.teaching.aggregates;

import top.egon.cola.archetype.source.light.domain.teaching.entities.Course;
import top.egon.cola.archetype.source.light.domain.teaching.entities.SchoolClass;
import top.egon.cola.archetype.source.light.domain.teaching.enums.CourseStatus;
import top.egon.cola.archetype.source.light.domain.teaching.enums.SchoolClassStatus;
import top.egon.cola.archetype.source.light.common.exception.TeachingDomainException;
import top.egon.cola.archetype.source.light.domain.teaching.vos.CourseCode;
import top.egon.cola.archetype.source.light.domain.teaching.vos.CourseScheduleBO;
import top.egon.cola.archetype.source.light.domain.teaching.vos.SchoolClassId;
import top.egon.cola.archetype.source.light.domain.teaching.vos.Semester;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SchoolClassAggregateTest {
    @Test
    void windows_normalize_microseconds_before_overlap() {
        CourseScheduleBO first = new CourseScheduleBO(new CourseCode("MATH"),
                Instant.parse("2026-10-13T06:07:08.123456789Z"), Instant.parse("2026-10-13T06:07:08.123457100Z"));
        CourseScheduleBO adjacent = new CourseScheduleBO(new CourseCode("MATH"),
                Instant.parse("2026-10-13T06:07:08.123457999Z"), Instant.parse("2026-10-13T06:07:08.123458999Z"));
        assertEquals(Instant.parse("2026-10-13T06:07:08.123456Z"), first.startsAt());
        org.junit.jupiter.api.Assertions.assertFalse(first.overlaps(adjacent));
        assertEquals(first, new CourseScheduleBO(first.courseCode(), first.startsAt(), first.endsAt()));
    }

    @Test
    void folded_micro_window_is_rejected_and_offsets_identify_the_same_instant() {
        TeachingDomainException error = assertThrows(TeachingDomainException.class, () -> new CourseScheduleBO(new CourseCode("MATH"),
                Instant.parse("2026-10-13T06:07:08.123456100Z"), Instant.parse("2026-10-13T06:07:08.123456900Z")));
        assertEquals("INVALID_SCHEDULE", error.getStatus());
        assertEquals(Instant.parse("2026-10-13T06:07:08Z"),
                java.time.OffsetDateTime.parse("2026-10-13T14:07:08+08:00").toInstant());
        assertEquals(Instant.parse("2026-11-01T05:30:00Z"),
                java.time.OffsetDateTime.parse("2026-11-01T01:30:00-04:00").toInstant());
    }

    @Test
    void schedules_non_overlapping_courses() {
        SchoolClassAggregate aggregate = activeClass();

        aggregate.schedule(activeCourse("math"), schedule("math", 9, 10));
        aggregate.schedule(activeCourse("english"), schedule("english", 10, 11));

        assertEquals(2, aggregate.schedules().size());
    }

    @Test
    void rejects_overlapping_course_times() {
        SchoolClassAggregate aggregate = activeClass();
        aggregate.schedule(activeCourse("math"), schedule("math", 9, 11));

        assertThrows(TeachingDomainException.class,
                () -> aggregate.schedule(activeCourse("english"), schedule("english", 10, 12)));
    }

    @Test
    void rejects_scheduling_for_inactive_class() {
        SchoolClass schoolClass = new SchoolClass(
                new SchoolClassId(1003L), "Class One", new Semester("2026-FALL"),
                SchoolClassStatus.ARCHIVED);
        SchoolClassAggregate aggregate = new SchoolClassAggregate(schoolClass);

        assertThrows(TeachingDomainException.class,
                () -> aggregate.schedule(activeCourse("math"), schedule("math", 9, 10)));
    }

    @Test
    void rejects_disabled_course() {
        SchoolClassAggregate aggregate = activeClass();
        Course course = new Course(1002L, new CourseCode("math"), "math", CourseStatus.DISABLED);

        assertThrows(TeachingDomainException.class,
                () -> aggregate.schedule(course, schedule("math", 9, 10)));
    }

    private static SchoolClassAggregate activeClass() {
        SchoolClass schoolClass = new SchoolClass(
                new SchoolClassId(1003L), "Class One", new Semester("2026-FALL"),
                SchoolClassStatus.ACTIVE);
        return new SchoolClassAggregate(schoolClass);
    }

    private static Course activeCourse(String code) {
        return new Course(1002L, new CourseCode(code), code, CourseStatus.ACTIVE);
    }

    private static CourseScheduleBO schedule(String code, int startsAtHour, int endsAtHour) {
        LocalDate date = LocalDate.of(2026, 9, 1);
        return new CourseScheduleBO(
                new CourseCode(code),
                date.atTime(startsAtHour, 0).toInstant(ZoneOffset.UTC),
                date.atTime(endsAtHour, 0).toInstant(ZoneOffset.UTC));
    }
}
