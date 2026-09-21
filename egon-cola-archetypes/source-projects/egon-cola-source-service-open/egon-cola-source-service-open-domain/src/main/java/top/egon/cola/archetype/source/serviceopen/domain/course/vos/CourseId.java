package top.egon.cola.archetype.source.serviceopen.domain.course.vos;

import top.egon.cola.archetype.source.serviceopen.common.enums.EvaluationDomainErrorCode;
import top.egon.cola.archetype.source.serviceopen.common.exception.EvaluationDomainException;

public record CourseId(Long value) {

    public CourseId {
        if (value == null || value <= 0) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.VALIDATION_FAILED, "course id must be positive");
        }
    }
}
