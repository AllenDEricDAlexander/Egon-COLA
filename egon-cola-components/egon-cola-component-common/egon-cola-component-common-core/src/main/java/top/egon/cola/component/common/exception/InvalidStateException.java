package top.egon.cola.component.common.exception;

import top.egon.cola.component.common.enums.ErrorStatus;

import java.io.Serial;

/**
 * Exception for invalid component or domain state transitions.
 */
public class InvalidStateException extends CommonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidStateException(ErrorStatus errorStatus) {
        super(errorStatus);
    }

    public InvalidStateException(ErrorStatus errorStatus, Throwable cause) {
        super(errorStatus, cause);
    }

    public InvalidStateException(int code, String status, String message) {
        super(code, status, message);
    }

    public InvalidStateException(int code, String status, String message, Throwable cause) {
        super(code, status, message, cause);
    }
}
