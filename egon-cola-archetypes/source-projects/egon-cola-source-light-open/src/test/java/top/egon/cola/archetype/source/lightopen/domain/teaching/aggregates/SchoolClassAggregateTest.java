package top.egon.cola.archetype.source.lightopen.domain.teaching.aggregates;

import top.egon.cola.archetype.source.lightopen.domain.teaching.entities.Course;
import top.egon.cola.archetype.source.lightopen.domain.teaching.entities.SchoolClass;
import top.egon.cola.archetype.source.lightopen.domain.teaching.enums.CourseStatus;
import top.egon.cola.archetype.source.lightopen.domain.teaching.enums.SchoolClassStatus;
import top.egon.cola.archetype.source.lightopen.domain.teaching.exceptions.TeachingDomainException;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.CourseCode;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.CourseSchedule;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.SchoolClassId;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.Semester;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SchoolClassAggregateTest {
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

    private static CourseSchedule schedule(String code, int startsAtHour, int endsAtHour) {
        LocalDate date = LocalDate.of(2026, 9, 1);
        return new CourseSchedule(
                new CourseCode(code),
                date.atTime(startsAtHour, 0),
                date.atTime(endsAtHour, 0));
    }
}
