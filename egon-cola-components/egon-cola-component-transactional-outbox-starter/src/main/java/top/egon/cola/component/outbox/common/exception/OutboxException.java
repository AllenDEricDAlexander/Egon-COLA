package top.egon.cola.component.outbox.common.exception;

import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;

import java.io.Serial;

public class OutboxException extends CommonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public OutboxException(String message) {
        super(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getStatus(), message);
    }

    public OutboxException(String message, Throwable cause) {
        super(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getStatus(), message, cause);
    }
}
