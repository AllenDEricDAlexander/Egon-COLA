package top.egon.cola.archetype.source.web.common.exception;

import top.egon.cola.archetype.source.web.common.enums.OrganizationDomainErrorCode;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.BusinessException;

import java.io.Serial;

/**
 * Domain invariant violation raised inside the organization aggregates and value objects. The
 * stable wire code stays published through {@link #getStatus()}; {@link #code()} keeps the enum.
 */
public class OrganizationDomainException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final OrganizationDomainErrorCode error;

    public OrganizationDomainException(OrganizationDomainErrorCode error, String message) {
        this(error, message, null);
    }

    public OrganizationDomainException(OrganizationDomainErrorCode error, String message, Throwable cause) {
        super(ResultCode.BUSINESS_ERROR.getCode(), error.getStatus(), message, cause);
        this.error = error;
    }

    public OrganizationDomainErrorCode code() {
        return error;
    }
}
