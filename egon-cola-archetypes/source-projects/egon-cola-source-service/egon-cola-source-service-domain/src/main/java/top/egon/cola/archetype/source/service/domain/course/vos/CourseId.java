package top.egon.cola.archetype.source.service.domain.course.vos;

import top.egon.cola.archetype.source.service.domain.common.EvaluationDomainErrorCode;
import top.egon.cola.archetype.source.service.domain.common.EvaluationDomainException;

public record CourseId(Long value) {

    public CourseId {
        if (value == null || value <= 0) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.VALIDATION_FAILED, "course id must be positive");
        }
    }
}
