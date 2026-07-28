package top.egon.cola.component.common.exception;

import top.egon.cola.component.common.enums.ErrorStatus;

import java.io.Serial;

/**
 * Exception for missing resources.
 */
public class NotFoundException extends CommonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public NotFoundException(ErrorStatus errorStatus) {
        super(errorStatus);
    }

    public NotFoundException(ErrorStatus errorStatus, Throwable cause) {
        super(errorStatus, cause);
    }

    public NotFoundException(int code, String status, String message) {
        super(code, status, message);
    }

    public NotFoundException(int code, String status, String message, Throwable cause) {
        super(code, status, message, cause);
    }
}
