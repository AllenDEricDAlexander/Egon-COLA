package top.egon.cola.component.common.core.exception;

import top.egon.cola.component.common.core.code.ErrorStatus;

import java.io.Serial;

/**
 * Exception for concurrent modification or duplicate submission conflicts.
 */
public class EgonConcurrencyException extends EgonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public EgonConcurrencyException(ErrorStatus errorStatus) {
        super(errorStatus);
    }

    public EgonConcurrencyException(ErrorStatus errorStatus, Throwable cause) {
        super(errorStatus, cause);
    }

    public EgonConcurrencyException(int code, String status, String message) {
        super(code, status, message);
    }

    public EgonConcurrencyException(int code, String status, String message, Throwable cause) {
        super(code, status, message, cause);
    }
}
