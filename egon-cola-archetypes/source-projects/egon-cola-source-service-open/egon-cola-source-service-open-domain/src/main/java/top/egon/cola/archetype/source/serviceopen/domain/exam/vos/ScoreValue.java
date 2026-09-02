package top.egon.cola.archetype.source.serviceopen.domain.exam.vos;

import top.egon.cola.archetype.source.serviceopen.domain.common.EvaluationDomainErrorCode;
import top.egon.cola.archetype.source.serviceopen.domain.common.EvaluationDomainException;

public record ScoreValue(int value) {

    public ScoreValue {
        if (value < 0) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.SCORE_OUT_OF_RANGE, "score must not be negative");
        }
    }
}
