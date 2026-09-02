package top.egon.cola.archetype.source.serviceopen.common.exceptions;

public class EvaluationBizException extends RuntimeException {

    private final EvaluationError code;

    public EvaluationBizException(EvaluationError code, String message) {
        this(code, message, null);
    }

    public EvaluationBizException(EvaluationError code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public EvaluationError getCode() {
        return code;
    }
}
