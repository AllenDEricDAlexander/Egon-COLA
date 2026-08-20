package top.egon.cola.component.common.core.exception;

import top.egon.cola.component.common.core.enums.BusinessExceptionEnum;
import top.egon.cola.component.common.core.enums.ErrorStatus;
import top.egon.cola.component.common.core.enums.ExceptionLevelEnum;

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
        this(errorStatus, ExceptionLevelEnum.ERROR, null, new Object[0]);
    }

    public BusinessException(ErrorStatus errorStatus, Throwable cause) {
        this(errorStatus, ExceptionLevelEnum.ERROR, cause, new Object[0]);
    }

    public BusinessException(ErrorStatus errorStatus, Object... details) {
        this(errorStatus, ExceptionLevelEnum.ERROR, null, details);
    }

    public BusinessException(ErrorStatus errorStatus, Throwable cause, Object... details) {
        this(errorStatus, ExceptionLevelEnum.ERROR, cause, details);
    }

    public BusinessException(BusinessExceptionEnum businessExceptionEnum) {
        this(businessExceptionEnum, ExceptionLevelEnum.ERROR, null, new Object[0]);
    }

    public BusinessException(BusinessExceptionEnum businessExceptionEnum, Throwable cause) {
        this(businessExceptionEnum, ExceptionLevelEnum.ERROR, cause, new Object[0]);
    }

    public BusinessException(BusinessExceptionEnum businessExceptionEnum, Object... details) {
        this(businessExceptionEnum, ExceptionLevelEnum.ERROR, null, details);
    }

    public BusinessException(BusinessExceptionEnum businessExceptionEnum,
                             Throwable cause,
                             Object... details) {
        this(businessExceptionEnum, ExceptionLevelEnum.ERROR, cause, details);
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
        super(
                Objects.requireNonNull(businessExceptionEnum, "businessExceptionEnum").getCode(),
                businessExceptionEnum.getStatus(),
                formatMessage(businessExceptionEnum.getMessage(), details),
                cause
        );
        this.businessExceptionEnum = businessExceptionEnum;
        this.level = Objects.requireNonNull(level, "level");
        this.details = copyDetails(details);
    }

    public BusinessException(int code, String status, String message) {
        super(code, status, message);
        this.businessExceptionEnum = null;
        this.level = ExceptionLevelEnum.ERROR;
        this.details = new Object[0];
    }

    public BusinessException(int code, String status, String message, Throwable cause) {
        super(code, status, message, cause);
        this.businessExceptionEnum = null;
        this.level = ExceptionLevelEnum.ERROR;
        this.details = new Object[0];
    }

    private BusinessException(ErrorStatus errorStatus,
                              ExceptionLevelEnum level,
                              Throwable cause,
                              Object[] details) {
        super(Objects.requireNonNull(errorStatus, "errorStatus"), cause, details);
        this.businessExceptionEnum = errorStatus instanceof BusinessExceptionEnum business
                ? business
                : null;
        this.level = Objects.requireNonNull(level, "level");
        this.details = copyDetails(details);
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
        return new BusinessException(BusinessExceptionEnum.SYSTEM_ERROR, cause);
    }

    public BusinessExceptionEnum getBusinessExceptionEnum() {
        return businessExceptionEnum;
    }

    public ExceptionLevelEnum getLevel() {
        return level;
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
