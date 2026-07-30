package top.egon.cola.component.ddc.admin.common;

import top.egon.cola.component.common.core.enums.ErrorStatus;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.ddc.common.DdcErrorStatus;

import java.io.Serial;

public class DdcAdminException extends CommonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public DdcAdminException(ErrorStatus errorStatus) {
        super(errorStatus);
    }

    public DdcAdminException(ErrorStatus errorStatus, Throwable cause) {
        super(errorStatus, cause);
    }

    public DdcAdminException(String message) {
        super(
                DdcErrorStatus.INVALID_REQUEST.getCode(),
                DdcErrorStatus.INVALID_REQUEST.getStatus(),
                message
        );
    }

    public DdcAdminException(String message, Throwable cause) {
        super(
                DdcErrorStatus.INTERNAL_FAILURE.getCode(),
                DdcErrorStatus.INTERNAL_FAILURE.getStatus(),
                message,
                cause
        );
    }
}
