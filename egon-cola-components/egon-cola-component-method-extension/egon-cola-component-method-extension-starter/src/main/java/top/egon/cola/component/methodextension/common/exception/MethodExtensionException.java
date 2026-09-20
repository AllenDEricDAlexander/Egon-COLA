package top.egon.cola.component.methodextension.common.exception;

import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;

import java.io.Serial;

public class MethodExtensionException extends CommonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public MethodExtensionException(String message) {
        super(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getStatus(), message);
    }

    public MethodExtensionException(String message, Throwable cause) {
        super(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getStatus(), message, cause);
    }
}
