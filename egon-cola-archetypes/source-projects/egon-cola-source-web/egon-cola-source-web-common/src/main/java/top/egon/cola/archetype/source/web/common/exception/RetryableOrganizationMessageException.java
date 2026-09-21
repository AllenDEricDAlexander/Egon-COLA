package top.egon.cola.archetype.source.web.common.exception;

import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;

import java.io.Serial;

/**
 * Message handling failed for a reason the broker may resolve later, so the delivery is retried
 * instead of being rejected. The retryable flag is the contract the consumer boundary relies on.
 */
public final class RetryableOrganizationMessageException extends CommonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String STATUS = "ORGANIZATION_MESSAGE_RETRYABLE";

    public RetryableOrganizationMessageException(String message, Throwable cause) {
        super(ResultCode.MIDDLEWARE_ERROR.getCode(), STATUS, message, true, cause);
    }
}
