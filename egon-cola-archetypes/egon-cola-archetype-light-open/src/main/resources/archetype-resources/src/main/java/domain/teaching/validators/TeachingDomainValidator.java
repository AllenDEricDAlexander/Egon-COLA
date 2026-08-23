package ${package}.domain.teaching.validators;

import ${package}.domain.teaching.entities.Course;
import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.teaching.enums.CourseStatus;
import ${package}.domain.teaching.enums.SchoolClassStatus;
import ${package}.domain.teaching.exceptions.TeachingDomainException;
import ${package}.domain.teaching.vos.CourseSchedule;

import java.util.Collection;

public final class TeachingDomainValidator {
    private TeachingDomainValidator() {
    }

    public static void requireSchedulable(SchoolClass schoolClass, Course course, CourseSchedule schedule) {
        if (schoolClass.status() != SchoolClassStatus.ACTIVE) {
            throw new TeachingDomainException("CLASS_NOT_ACTIVE", "School class must be active");
        }
        if (course.status() != CourseStatus.ACTIVE) {
            throw new TeachingDomainException("COURSE_NOT_ACTIVE", "Course must be active");
        }
        if (!course.code().equals(schedule.courseCode())) {
            throw new TeachingDomainException("COURSE_SCHEDULE_MISMATCH", "Schedule must reference the course");
        }
    }

    public static void requireNoOverlap(Collection<CourseSchedule> schedules, CourseSchedule candidate) {
        if (schedules.stream().anyMatch(candidate::overlaps)) {
            throw new TeachingDomainException("SCHEDULE_OVERLAP", "Course schedule overlaps an existing schedule");
        }
    }
}
