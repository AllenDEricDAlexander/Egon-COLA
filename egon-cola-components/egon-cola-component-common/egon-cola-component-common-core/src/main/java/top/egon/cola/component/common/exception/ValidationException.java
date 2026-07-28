package top.egon.cola.component.common.exception;

import top.egon.cola.component.common.enums.ErrorStatus;

import java.io.Serial;

/**
 * Exception for validation failures.
 */
public class ValidationException extends CommonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ValidationException(ErrorStatus errorStatus) {
        super(errorStatus);
    }

    public ValidationException(ErrorStatus errorStatus, Throwable cause) {
        super(errorStatus, cause);
    }

    public ValidationException(int code, String status, String message) {
        super(code, status, message);
    }

    public ValidationException(int code, String status, String message, Throwable cause) {
        super(code, status, message, cause);
    }
}
