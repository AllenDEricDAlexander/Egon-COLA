package top.egon.cola.archetype.source.lightopen.domain.teaching.validators;

import jakarta.validation.Validation;
import top.egon.cola.archetype.source.lightopen.common.exception.TeachingDomainException;
import top.egon.cola.archetype.source.lightopen.domain.teaching.entities.Course;
import top.egon.cola.archetype.source.lightopen.domain.teaching.entities.SchoolClass;
import top.egon.cola.archetype.source.lightopen.domain.teaching.enums.CourseStatus;
import top.egon.cola.archetype.source.lightopen.domain.teaching.enums.SchoolClassStatus;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.CourseSchedule;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import java.util.Collection;

/** Teaching invariants; the native-constraint facade is inherited from the common base. */
public class TeachingDomainValidator extends BaseValidator {

    @Override
    protected ValidationUtils getValidationUtils() {
        return JakartaValidation.UTILS;
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

    /** The Domain layer stays framework-free, so the Jakarta bootstrap is created on first use only. */
    private static final class JakartaValidation {
        private static final ValidationUtils UTILS =
                new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator());
    }
}
