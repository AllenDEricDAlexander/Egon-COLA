package top.egon.cola.archetype.source.service.common.exceptions;

public final class EvaluationNotFoundException extends EvaluationBizException {

    public EvaluationNotFoundException(EvaluationError code, String message) {
        super(code, message);
    }
}
