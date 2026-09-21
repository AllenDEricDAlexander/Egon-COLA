package top.egon.cola.archetype.source.service.domain.exam.validators;

import top.egon.cola.archetype.source.service.common.enums.EvaluationDomainErrorCode;
import top.egon.cola.archetype.source.service.common.exception.EvaluationDomainException;
import top.egon.cola.archetype.source.service.domain.course.entities.Course;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import java.time.Instant;

/** Exam and paper relation rules; native constraints run first through the common validation facade. */
public class ExamDomainValidator extends BaseValidator {

    private final ValidationUtils validationUtils;

    public ExamDomainValidator(ValidationUtils validationUtils) {
        this.validationUtils = validationUtils;
    }

    @Override
    protected ValidationUtils getValidationUtils() {
        return validationUtils;
    }

    public void validateExam(Course course, String title, Instant startsAt, Instant endsAt) {
        if (course == null || !course.isActive()) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.COURSE_INACTIVE, "exam requires an active course");
        }
        if (title == null || title.isBlank() || startsAt == null || endsAt == null
                || !startsAt.isBefore(endsAt)) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.VALIDATION_FAILED, "invalid exam definition");
        }
    }

    public void validatePaper(String title, int totalPoints) {
        if (title == null || title.isBlank() || totalPoints <= 0) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.VALIDATION_FAILED, "exam paper requires positive points");
        }
    }
}
