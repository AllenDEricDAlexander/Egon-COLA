package ${package}.domain.teaching.aggregates;

import ${package}.domain.teaching.entities.Course;
import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.teaching.enums.CourseStatus;
import ${package}.domain.teaching.enums.SchoolClassStatus;
import ${package}.domain.teaching.exceptions.TeachingDomainException;
import ${package}.domain.teaching.vos.CourseCode;
import ${package}.domain.teaching.vos.CourseSchedule;
import ${package}.domain.teaching.vos.SchoolClassId;
import ${package}.domain.teaching.vos.Semester;
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
                new SchoolClassId("class-1"), "Class One", new Semester("2026-FALL"),
                SchoolClassStatus.ARCHIVED);
        SchoolClassAggregate aggregate = new SchoolClassAggregate(schoolClass);

        assertThrows(TeachingDomainException.class,
                () -> aggregate.schedule(activeCourse("math"), schedule("math", 9, 10)));
    }

    @Test
    void rejects_disabled_course() {
        SchoolClassAggregate aggregate = activeClass();
        Course course = new Course("course-math", new CourseCode("math"), "math", CourseStatus.DISABLED);

        assertThrows(TeachingDomainException.class,
                () -> aggregate.schedule(course, schedule("math", 9, 10)));
    }

    private static SchoolClassAggregate activeClass() {
        SchoolClass schoolClass = new SchoolClass(
                new SchoolClassId("class-1"), "Class One", new Semester("2026-FALL"),
                SchoolClassStatus.ACTIVE);
        return new SchoolClassAggregate(schoolClass);
    }

    private static Course activeCourse(String code) {
        return new Course("course-" + code, new CourseCode(code), code, CourseStatus.ACTIVE);
    }

    private static CourseSchedule schedule(String code, int startsAtHour, int endsAtHour) {
        LocalDate date = LocalDate.of(2026, 9, 1);
        return new CourseSchedule(
                new CourseCode(code),
                date.atTime(startsAtHour, 0),
                date.atTime(endsAtHour, 0));
    }
}
