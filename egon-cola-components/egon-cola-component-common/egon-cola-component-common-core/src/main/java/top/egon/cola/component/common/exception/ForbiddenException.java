package top.egon.cola.component.common.exception;

import top.egon.cola.component.common.enums.ErrorStatus;

import java.io.Serial;

/**
 * Exception for forbidden access.
 */
public class ForbiddenException extends CommonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ForbiddenException(ErrorStatus errorStatus) {
        super(errorStatus);
    }

    public ForbiddenException(ErrorStatus errorStatus, Throwable cause) {
        super(errorStatus, cause);
    }

    public ForbiddenException(int code, String status, String message) {
        super(code, status, message);
    }

    public ForbiddenException(int code, String status, String message, Throwable cause) {
        super(code, status, message, cause);
    }
}
