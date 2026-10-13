package top.egon.cola.component.common.core.exception;

import top.egon.cola.component.common.core.enums.BusinessExceptionEnum;
import top.egon.cola.component.common.core.enums.ErrorStatus;
import top.egon.cola.component.common.core.enums.ExceptionLevelEnum;
import top.egon.cola.component.common.core.enums.ExceptionCategory;
import top.egon.cola.component.common.core.enums.ResultCode;

import java.io.Serial;
import java.util.Objects;

/**
 * Exception for expected business rule failures.
 */
public class BusinessException extends CommonException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final BusinessExceptionEnum businessExceptionEnum;

    private final ExceptionLevelEnum level;

    private long messageId;

    private Object[] details;

    public BusinessException(ErrorStatus errorStatus) {
        this(errorStatus, ExceptionLevelEnum.WARN, null, new Object[0]);
    }

    public BusinessException(ErrorStatus errorStatus, Throwable cause) {
        this(errorStatus, ExceptionLevelEnum.WARN, cause, new Object[0]);
    }

    public BusinessException(ErrorStatus errorStatus, Object... details) {
        this(errorStatus, ExceptionLevelEnum.WARN, null, details);
    }

    public BusinessException(ErrorStatus errorStatus, Throwable cause, Object... details) {
        this(errorStatus, ExceptionLevelEnum.WARN, cause, details);
    }

    public BusinessException(BusinessExceptionEnum businessExceptionEnum) {
        this(businessExceptionEnum, ExceptionLevelEnum.WARN, null, new Object[0]);
    }

    public BusinessException(BusinessExceptionEnum businessExceptionEnum, Throwable cause) {
        this(businessExceptionEnum, ExceptionLevelEnum.WARN, cause, new Object[0]);
    }

    public BusinessException(BusinessExceptionEnum businessExceptionEnum, Object... details) {
        this(businessExceptionEnum, ExceptionLevelEnum.WARN, null, details);
    }

    public BusinessException(BusinessExceptionEnum businessExceptionEnum,
                             Throwable cause,
                             Object... details) {
        this(businessExceptionEnum, ExceptionLevelEnum.WARN, cause, details);
    }

    public BusinessException(BusinessExceptionEnum businessExceptionEnum,
                             ExceptionLevelEnum level,
                             Object... details) {
        this(businessExceptionEnum, level, null, details);
    }

    public BusinessException(BusinessExceptionEnum businessExceptionEnum,
                             ExceptionLevelEnum level,
                             Throwable cause,
                             Object... details) {
        this(businessExceptionEnum, level, cause, details, category(businessExceptionEnum));
    }

    public BusinessException(int code, String status, String message) {
        this(code, status, message, null);
    }

    public BusinessException(int code, String status, String message, Throwable cause) {
        this(code, status, message, cause, category(code, status));
    }

    /** Allows a typed application failure to mark a dependency/internal rejection explicitly. */
    protected BusinessException(int code, String status, String message, Throwable cause,
                                ExceptionCategory category) {
        super(code, status, message, false, cause, category);
        this.businessExceptionEnum = null;
        this.level = category == ExceptionCategory.BUSINESS ? ExceptionLevelEnum.WARN : ExceptionLevelEnum.ERROR;
        this.details = new Object[0];
    }

    private BusinessException(ErrorStatus errorStatus,
                              ExceptionLevelEnum level,
                              Throwable cause,
                              Object[] details) {
        this(errorStatus, level, cause, details, category(errorStatus));
    }

    private BusinessException(ErrorStatus errorStatus, ExceptionLevelEnum level,
                              Throwable cause, Object[] details, ExceptionCategory category) {
        super(Objects.requireNonNull(errorStatus, "errorStatus").getCode(), errorStatus.getStatus(),
                formatMessage(errorStatus.getMessage(), details), false, cause, category);
        this.businessExceptionEnum = errorStatus instanceof BusinessExceptionEnum business
                ? business
                : null;
        this.level = Objects.requireNonNull(level, "level");
        this.details = copyDetails(details);
    }

    /** Marks a business response that also needs operational attention, without ambiguous overloads. */
    public static BusinessException alert(ErrorStatus errorStatus, Throwable cause, Object... details) {
        return new BusinessException(errorStatus, ExceptionLevelEnum.ERROR, cause, details, ExceptionCategory.BUSINESS_ALERT);
    }

    private static ExceptionCategory category(int code, String status) {
        for (ResultCode known : ResultCode.values()) {
            if (known.getCode() == code && known.getStatus().equals(status)) {
                return category(known);
            }
        }
        for (BusinessExceptionEnum known : BusinessExceptionEnum.values()) {
            if (known.getCode() == code && known.getStatus().equals(status)) {
                return category(known);
            }
        }
        return ExceptionCategory.BUSINESS;
    }

    private static ExceptionCategory category(ErrorStatus status) {
        Objects.requireNonNull(status, "errorStatus");
        boolean system = status == BusinessExceptionEnum.SYSTEM_ERROR
                || status == BusinessExceptionEnum.INTERFACE_CALL_ERROR
                || status == BusinessExceptionEnum.TIMEOUT
                || status == BusinessExceptionEnum.OPEN_REQUEST_EXCEPTION
                || status == ResultCode.SYSTEM_ERROR || status == ResultCode.REMOTE_CALL_ERROR
                || status == ResultCode.MIDDLEWARE_ERROR || status == ResultCode.UNKNOWN_ERROR;
        return system ? ExceptionCategory.BUSINESS_ALERT : ExceptionCategory.BUSINESS;
    }

    public static BusinessException get(BusinessExceptionEnum businessExceptionEnum,
                                        Object... details) {
        return new BusinessException(businessExceptionEnum, details);
    }

    public static BusinessException get(BusinessExceptionEnum businessExceptionEnum,
                                        ExceptionLevelEnum level,
                                        Object... details) {
        return new BusinessException(businessExceptionEnum, level, details);
    }

    public static BusinessException getWarn(BusinessExceptionEnum businessExceptionEnum,
                                            Object... details) {
        return get(businessExceptionEnum, ExceptionLevelEnum.WARN, details);
    }

    public static BusinessException get(Throwable cause,
                                        BusinessExceptionEnum businessExceptionEnum,
                                        Object... details) {
        if (cause instanceof BusinessException businessException) {
            return businessException;
        }
        return new BusinessException(businessExceptionEnum, cause, details);
    }

    public static BusinessException get(Throwable cause,
                                        BusinessExceptionEnum businessExceptionEnum,
                                        ExceptionLevelEnum level,
                                        Object... details) {
        if (cause instanceof BusinessException businessException) {
            return businessException;
        }
        return new BusinessException(businessExceptionEnum, level, cause, details);
    }

    public static BusinessException get(Exception cause) {
        if (cause instanceof BusinessException businessException) {
            return businessException;
        }
        return alert(BusinessExceptionEnum.SYSTEM_ERROR, cause);
    }

    ExceptionCategory legacyCategory() {
        return businessExceptionEnum == null ? category(getCode(), getStatus()) : category(businessExceptionEnum);
    }

    public BusinessExceptionEnum getBusinessExceptionEnum() {
        return businessExceptionEnum;
    }

    public ExceptionLevelEnum getLevel() {
        return isAlertRequired() && level != ExceptionLevelEnum.FATAL ? ExceptionLevelEnum.ERROR : level;
    }

    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    public Object[] getDetails() {
        return details.clone();
    }

    public void setDetails(Object[] details) {
        this.details = copyDetails(details);
    }

    @Override
    public String toString() {
        return super.toString() + "-[" + getCode() + "]";
    }

    private static Object[] copyDetails(Object[] details) {
        return details == null ? new Object[0] : details.clone();
    }
}
