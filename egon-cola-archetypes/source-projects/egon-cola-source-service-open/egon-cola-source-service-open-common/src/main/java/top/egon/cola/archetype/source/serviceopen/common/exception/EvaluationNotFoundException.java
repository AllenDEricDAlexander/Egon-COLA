package top.egon.cola.archetype.source.serviceopen.common.exception;

import top.egon.cola.archetype.source.serviceopen.common.enums.EvaluationError;

import java.io.Serial;

/** Evaluation resource the caller referenced does not exist. */
public final class EvaluationNotFoundException extends EvaluationBizException {

    @Serial
    private static final long serialVersionUID = 1L;

    public EvaluationNotFoundException(EvaluationError code, String message) {
        super(code, message);
    }
}
