package top.egon.cola.archetype.source.web.common.exception;

import top.egon.cola.component.common.core.enums.ExceptionCategory;
import top.egon.cola.archetype.source.web.common.enums.OrganizationFailureType;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.BusinessException;

import java.io.Serial;

/**
 * Application use-case rejection. The caller supplied stable String code stays published through
 * {@link #getStatus()}; {@link #failureType()} keeps the classification used by the wire mapping.
 */
public class OrganizationApplicationException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final OrganizationFailureType failureType;

    public OrganizationApplicationException(OrganizationFailureType failureType, String code, String message) {
        this(failureType, code, message, null);
    }

    public OrganizationApplicationException(
            OrganizationFailureType failureType, String code, String message, Throwable cause) {
        super(ResultCode.BUSINESS_ERROR.getCode(), code, message, cause,
                failureType == OrganizationFailureType.DEPENDENCY_UNAVAILABLE || failureType == OrganizationFailureType.INTERNAL
                        ? ExceptionCategory.BUSINESS_ALERT : ExceptionCategory.BUSINESS);
        this.failureType = failureType;
    }

    public OrganizationFailureType failureType() {
        return failureType;
    }

    public String code() {
        return getStatus();
    }
}
