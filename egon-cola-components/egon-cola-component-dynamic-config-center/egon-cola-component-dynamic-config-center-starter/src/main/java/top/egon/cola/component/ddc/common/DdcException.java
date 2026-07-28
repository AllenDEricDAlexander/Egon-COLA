package top.egon.cola.component.ddc.common;

import top.egon.cola.component.common.enums.ErrorStatus;
import top.egon.cola.component.common.exception.CommonException;

import java.io.Serial;

public class DdcException extends CommonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public DdcException(ErrorStatus errorStatus) {
        super(errorStatus);
    }

    public DdcException(ErrorStatus errorStatus, Throwable cause) {
        super(errorStatus, cause);
    }

    public DdcException(int code, String status, String message) {
        super(code, status, message);
    }

    public DdcException(String message) {
        super(
                DdcErrorStatus.INVALID_REQUEST.getCode(),
                DdcErrorStatus.INVALID_REQUEST.getStatus(),
                message
        );
    }

    public DdcException(String message, Throwable cause) {
        super(
                DdcErrorStatus.INTERNAL_FAILURE.getCode(),
                DdcErrorStatus.INTERNAL_FAILURE.getStatus(),
                message,
                cause
        );
    }
}
