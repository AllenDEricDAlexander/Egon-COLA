package top.egon.cola.archetype.source.serviceopen.common.exception;

import top.egon.cola.archetype.source.serviceopen.common.enums.EvaluationDomainErrorCode;

import java.io.Serial;

/** A domain port invocation failed; {@link #operation()} names the affected port operation. */
public final class EvaluationPortException extends EvaluationDomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String operation;

    public EvaluationPortException(String operation, String message, Throwable cause) {
        super(EvaluationDomainErrorCode.PORT_FAILURE, message, cause);
        this.operation = operation;
    }

    public String operation() {
        return operation;
    }
}
