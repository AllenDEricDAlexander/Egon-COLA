package top.egon.cola.evaluation.facade.exceptions;

import top.egon.cola.evaluation.facade.enums.EvaluationFacadeErrorCode;

public final class EvaluationFacadeException extends RuntimeException {

    private final EvaluationFacadeErrorCode code;

    public EvaluationFacadeException(EvaluationFacadeErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public EvaluationFacadeErrorCode code() {
        return code;
    }
}
