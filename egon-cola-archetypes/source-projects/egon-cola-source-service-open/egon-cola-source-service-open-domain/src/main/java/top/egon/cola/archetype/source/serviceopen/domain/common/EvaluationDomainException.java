package top.egon.cola.archetype.source.serviceopen.domain.common;

import top.egon.cola.archetype.source.serviceopen.common.exceptions.EvaluationBizException;

public class EvaluationDomainException extends EvaluationBizException {

    public EvaluationDomainException(EvaluationDomainErrorCode code, String message) {
        super(code, message);
    }

    public EvaluationDomainException(
            EvaluationDomainErrorCode code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
