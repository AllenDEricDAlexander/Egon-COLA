package top.egon.cola.archetype.source.serviceopen.domain.exam.vos;

import top.egon.cola.archetype.source.serviceopen.common.enums.EvaluationDomainErrorCode;
import top.egon.cola.archetype.source.serviceopen.common.exception.EvaluationDomainException;

public record ExamId(Long value) {

    public ExamId {
        if (value == null || value <= 0) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.VALIDATION_FAILED, "exam id must be positive");
        }
    }
}
