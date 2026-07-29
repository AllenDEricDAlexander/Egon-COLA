package top.egon.cola.component.common.core.exception;

import top.egon.cola.component.common.core.enums.ErrorStatus;

import java.io.Serial;

/**
 * Exception for unexpected system failures.
 */
public class SystemException extends CommonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public SystemException(ErrorStatus errorStatus) {
        super(errorStatus);
    }

    public SystemException(ErrorStatus errorStatus, Throwable cause) {
        super(errorStatus, cause);
    }

    public SystemException(int code, String status, String message) {
        super(code, status, message);
    }

    public SystemException(int code, String status, String message, Throwable cause) {
        super(code, status, message, cause);
    }
}
