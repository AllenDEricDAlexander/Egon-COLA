package top.egon.cola.archetype.source.serviceopen.common.exceptions;

public final class EvaluationNotFoundException extends EvaluationBizException {

    public EvaluationNotFoundException(EvaluationError code, String message) {
        super(code, message);
    }
}
