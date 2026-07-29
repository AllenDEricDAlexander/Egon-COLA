package top.egon.cola.component.common.core.exception;

import top.egon.cola.component.common.core.enums.ErrorStatus;

import java.io.Serial;

/**
 * Exception for unauthenticated access.
 */
public class UnauthorizedException extends CommonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public UnauthorizedException(ErrorStatus errorStatus) {
        super(errorStatus);
    }

    public UnauthorizedException(ErrorStatus errorStatus, Throwable cause) {
        super(errorStatus, cause);
    }

    public UnauthorizedException(int code, String status, String message) {
        super(code, status, message);
    }

    public UnauthorizedException(int code, String status, String message, Throwable cause) {
        super(code, status, message, cause);
    }
}
