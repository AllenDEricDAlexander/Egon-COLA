package top.egon.cola.component.common.exception;

import top.egon.cola.component.common.enums.ErrorStatus;

import java.io.Serial;

/**
 * Exception for expected business rule failures.
 */
public class BusinessException extends CommonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BusinessException(ErrorStatus errorStatus) {
        super(errorStatus);
    }

    public BusinessException(ErrorStatus errorStatus, Throwable cause) {
        super(errorStatus, cause);
    }

    public BusinessException(int code, String status, String message) {
        super(code, status, message);
    }

    public BusinessException(int code, String status, String message, Throwable cause) {
        super(code, status, message, cause);
    }
}
