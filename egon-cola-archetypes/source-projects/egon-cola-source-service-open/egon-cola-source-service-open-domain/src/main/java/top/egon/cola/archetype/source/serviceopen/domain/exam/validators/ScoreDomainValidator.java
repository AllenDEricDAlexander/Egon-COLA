package top.egon.cola.archetype.source.serviceopen.domain.exam.validators;

import top.egon.cola.archetype.source.serviceopen.common.enums.EvaluationDomainErrorCode;
import top.egon.cola.archetype.source.serviceopen.common.exception.EvaluationDomainException;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.Exam;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.ExamPaper;
import top.egon.cola.archetype.source.serviceopen.domain.exam.enums.ExamStatus;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

/** Score range and duplication rules; native constraints run first through the common facade. */
public class ScoreDomainValidator extends BaseValidator {

    private final ValidationUtils validationUtils;

    public ScoreDomainValidator(ValidationUtils validationUtils) {
        this.validationUtils = validationUtils;
    }

    @Override
    protected ValidationUtils getValidationUtils() {
        return validationUtils;
    }

    public void validate(
            Exam exam, ExamPaper paper, Long studentId, int points, boolean duplicate) {
        if (exam == null || paper == null || exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.EXAM_NOT_PUBLISHABLE, "score requires a published exam");
        }
        if (studentId == null || studentId <= 0 || points < 0
                || points > paper.getTotalPoints()) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.SCORE_OUT_OF_RANGE, "score is outside paper range");
        }
        if (duplicate) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.SCORE_DUPLICATED, "student score already exists");
        }
    }
}
