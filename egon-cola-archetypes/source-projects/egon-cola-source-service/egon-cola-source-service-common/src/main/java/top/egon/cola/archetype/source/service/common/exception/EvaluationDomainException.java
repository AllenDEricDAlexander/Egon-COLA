package top.egon.cola.archetype.source.service.common.exception;

import top.egon.cola.archetype.source.service.common.enums.EvaluationDomainErrorCode;

import java.io.Serial;

/** Domain invariant violation raised inside the evaluation aggregates. */
public class EvaluationDomainException extends EvaluationBizException {

    @Serial
    private static final long serialVersionUID = 1L;

    public EvaluationDomainException(EvaluationDomainErrorCode code, String message) {
        super(code, message);
    }

    public EvaluationDomainException(EvaluationDomainErrorCode code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
