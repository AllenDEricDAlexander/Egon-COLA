package top.egon.cola.component.common.core.exception;

import top.egon.cola.component.common.core.code.ErrorStatus;

import java.io.Serial;

/**
 * Exception for invalid component or domain state transitions.
 */
public class EgonIllegalStateException extends EgonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public EgonIllegalStateException(ErrorStatus errorStatus) {
        super(errorStatus);
    }

    public EgonIllegalStateException(ErrorStatus errorStatus, Throwable cause) {
        super(errorStatus, cause);
    }

    public EgonIllegalStateException(int code, String status, String message) {
        super(code, status, message);
    }
}
