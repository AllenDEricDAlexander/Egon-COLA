package top.egon.cola.component.common.core.exception;

import top.egon.cola.component.common.core.enums.ErrorStatus;
import top.egon.cola.component.common.core.enums.ExceptionCategory;

import java.io.Serial;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.IllegalFormatException;
import java.util.Objects;

/**
 * Base runtime exception carrying stable enterprise error status fields.
 */
public class CommonException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int code;

    private final String status;

    private final boolean retryable;

    private final ExceptionCategory exceptionCategory;

    public CommonException(ErrorStatus errorStatus) {
        this(errorStatus, false, null, new Object[0]);
    }

    public CommonException(ErrorStatus errorStatus, Throwable cause) {
        this(errorStatus, false, cause, new Object[0]);
    }

    public CommonException(ErrorStatus errorStatus, Object... details) {
        this(errorStatus, false, null, details);
    }

    public CommonException(ErrorStatus errorStatus, Throwable cause, Object... details) {
        this(errorStatus, false, cause, details);
    }

    public CommonException(ErrorStatus errorStatus, boolean retryable, Throwable cause) {
        this(errorStatus, retryable, cause, new Object[0]);
    }

    public CommonException(ErrorStatus errorStatus,
                           boolean retryable,
                           Throwable cause,
                           Object... details) {
        this(
                Objects.requireNonNull(errorStatus, "errorStatus").getCode(),
                errorStatus.getStatus(),
                formatMessage(errorStatus.getMessage(), details),
                retryable,
                cause
        );
    }

    public CommonException(int code, String status, String message) {
        this(code, status, message, false, null);
    }

    public CommonException(int code, String status, String message, Throwable cause) {
        this(code, status, message, false, cause);
    }

    public CommonException(int code, String status, String message, boolean retryable, Throwable cause) {
        this(code, status, message, retryable, cause, ExceptionCategory.SYSTEM);
    }

    protected CommonException(int code, String status, String message, boolean retryable,
                              Throwable cause, ExceptionCategory exceptionCategory) {
        super(message, cause);
        this.exceptionCategory = Objects.requireNonNull(exceptionCategory, "exceptionCategory");
        this.code = code;
        this.status = status;
        this.retryable = retryable;
    }

    public int getCode() {
        return code;
    }

    public String getStatus() {
        return status;
    }

    public boolean isRetryable() {
        return retryable;
    }

    /** Legacy serialized exceptions have no category field; keep their original family. */
    public ExceptionCategory getExceptionCategory() {
        ExceptionCategory category = exceptionCategory == null
                ? (this instanceof BusinessException business ? business.legacyCategory() : ExceptionCategory.SYSTEM)
                : exceptionCategory;
        return category == ExceptionCategory.BUSINESS && hasAlertCause(getCause())
                ? ExceptionCategory.BUSINESS_ALERT : category;
    }

    public boolean isAlertRequired() {
        return getExceptionCategory() != ExceptionCategory.BUSINESS;
    }

    private static boolean hasAlertCause(Throwable cause) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable current = cause; current != null && seen.add(current); current = current.getCause()) {
            if (current instanceof CommonException common) {
                ExceptionCategory category = common.exceptionCategory == null
                        ? (common instanceof BusinessException business ? business.legacyCategory() : ExceptionCategory.SYSTEM)
                        : common.exceptionCategory;
                if (category == ExceptionCategory.SYSTEM || category == ExceptionCategory.BUSINESS_ALERT) {
                    return true;
                }
            }
        }
        return false;
    }

    protected static String formatMessage(String message, Object[] details) {
        if (message == null || details == null || details.length == 0 || message.indexOf('%') < 0) {
            return message;
        }
        try {
            return String.format(message, details);
        } catch (IllegalFormatException ignored) {
            return message;
        }
    }
}
