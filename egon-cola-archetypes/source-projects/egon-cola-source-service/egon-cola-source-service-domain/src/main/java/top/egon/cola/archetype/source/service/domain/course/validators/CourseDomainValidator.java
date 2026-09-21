package top.egon.cola.archetype.source.service.domain.course.validators;

import top.egon.cola.archetype.source.service.common.enums.EvaluationDomainErrorCode;
import top.egon.cola.archetype.source.service.common.exception.EvaluationDomainException;
import top.egon.cola.archetype.source.service.domain.course.entities.Course;
import top.egon.cola.archetype.source.service.domain.course.entities.CourseSchedule;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import java.time.Instant;
import java.util.List;

/** Course relation rules; native constraints run first through the common validation facade. */
public class CourseDomainValidator extends BaseValidator {

    private final ValidationUtils validationUtils;

    public CourseDomainValidator(ValidationUtils validationUtils) {
        this.validationUtils = validationUtils;
    }

    @Override
    protected ValidationUtils getValidationUtils() {
        return validationUtils;
    }

    public void validateSchedule(
            Course course,
            Long classId,
            Instant startsAt,
            Instant endsAt,
            List<CourseSchedule> overlaps) {
        if (course == null || !course.isActive()) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.COURSE_INACTIVE, "only active courses can be scheduled");
        }
        if (classId == null || classId <= 0 || startsAt == null || endsAt == null
                || !startsAt.isBefore(endsAt)) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.VALIDATION_FAILED, "invalid course schedule");
        }
        if (overlaps != null && overlaps.stream().anyMatch(it -> it.overlaps(startsAt, endsAt))) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.SCHEDULE_CONFLICT, "course schedule overlaps");
        }
    }
}
