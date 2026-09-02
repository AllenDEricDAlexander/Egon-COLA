package top.egon.cola.archetype.source.service.domain.common;

public final class EvaluationPortException extends EvaluationDomainException {

    private final String operation;

    public EvaluationPortException(String operation, String message, Throwable cause) {
        super(EvaluationDomainErrorCode.PORT_FAILURE, message, cause);
        this.operation = operation;
    }

    public String operation() {
        return operation;
    }
}
