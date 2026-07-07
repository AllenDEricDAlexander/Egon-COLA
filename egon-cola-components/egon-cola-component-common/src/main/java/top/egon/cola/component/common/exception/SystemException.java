package top.egon.cola.component.common.exception;

import java.io.Serial;

/**
 * 系统不可预期异常，通常需要记录日志并排查基础设施或程序错误。
 */
public class SystemException extends CommonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public SystemException(String message) {
        this(ErrorCodes.SYSTEM_ERROR, message);
    }

    public SystemException(String code, String message) {
        super(code, message);
    }

    public SystemException(String message, Throwable cause) {
        this(ErrorCodes.SYSTEM_ERROR, message, cause);
    }

    public SystemException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
