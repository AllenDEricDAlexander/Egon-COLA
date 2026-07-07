package top.egon.cola.component.common.exception;

import java.io.Serial;

/**
 * 业务可预期异常，调用方可根据错误码进行业务处理。
 */
public class BusinessException extends CommonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        this(ErrorCodes.BUSINESS_ERROR, message);
    }

    public BusinessException(String code, String message) {
        super(code, message);
    }

    public BusinessException(String message, Throwable cause) {
        this(ErrorCodes.BUSINESS_ERROR, message, cause);
    }

    public BusinessException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
