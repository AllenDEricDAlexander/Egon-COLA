package top.egon.cola.archetype.source.web.common.exception;

import top.egon.cola.archetype.source.web.common.enums.OrganizationDomainErrorCode;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;

import java.io.Serial;

/** Technical failure while reaching an organization capability behind a domain service. */
public class OrganizationPortException extends CommonException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final OrganizationDomainErrorCode error;

    public OrganizationPortException(OrganizationDomainErrorCode code, String message, Throwable cause) {
        super(ResultCode.REMOTE_CALL_ERROR.getCode(), code.getStatus(), message, cause);
        this.error = code;
    }

    public OrganizationDomainErrorCode code() {
        return error;
    }
}
