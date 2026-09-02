package top.egon.cola.archetype.source.service.domain.exam.validators;

import top.egon.cola.archetype.source.service.domain.common.EvaluationDomainErrorCode;
import top.egon.cola.archetype.source.service.domain.common.EvaluationDomainException;
import top.egon.cola.archetype.source.service.domain.course.entities.Course;
import java.time.Instant;

public final class ExamDomainValidator {

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
