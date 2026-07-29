package top.egon.cola.component.common.core.exception;

import top.egon.cola.component.common.core.enums.ErrorStatus;

import java.io.Serial;

/**
 * Exception for concurrent modification or duplicate submission conflicts.
 */
public class ConcurrencyException extends CommonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ConcurrencyException(ErrorStatus errorStatus) {
        super(errorStatus);
    }

    public ConcurrencyException(ErrorStatus errorStatus, Throwable cause) {
        super(errorStatus, cause);
    }

    public ConcurrencyException(int code, String status, String message) {
        super(code, status, message);
    }

    public ConcurrencyException(int code, String status, String message, Throwable cause) {
        super(code, status, message, cause);
    }
}
