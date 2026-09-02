package top.egon.cola.archetype.source.service.domain.exam.vos;

import top.egon.cola.archetype.source.service.domain.common.EvaluationDomainErrorCode;
import top.egon.cola.archetype.source.service.domain.common.EvaluationDomainException;

public record ScoreValue(int value) {

    public ScoreValue {
        if (value < 0) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.SCORE_OUT_OF_RANGE, "score must not be negative");
        }
    }
}
