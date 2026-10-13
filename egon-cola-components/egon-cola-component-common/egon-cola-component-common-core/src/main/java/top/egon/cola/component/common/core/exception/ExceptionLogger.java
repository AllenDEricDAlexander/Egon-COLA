package top.egon.cola.component.common.core.exception;

import org.slf4j.Logger;
import org.slf4j.event.Level;
import top.egon.cola.component.common.core.enums.ExceptionCategory;
import top.egon.cola.component.common.core.enums.ExceptionLevelEnum;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.trace.TraceContext;

import java.io.Serial;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/** One structured event at the owning handling boundary; exception creation never logs. */
public final class ExceptionLogger {

    private ExceptionLogger() {
    }

    public static void log(Logger logger, Throwable failure) {
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(failure, "failure");
        Throwable classified = unwrap(failure);
        CommonException common = classified instanceof CommonException typed ? typed : null;
        ExceptionCategory category = common == null ? ExceptionCategory.SYSTEM : common.getExceptionCategory();
        ExceptionLevelEnum severity = common instanceof BusinessException business
                ? business.getLevel() : ExceptionLevelEnum.ERROR;
        TraceContext trace = TraceContext.capture();
        var event = logger.atLevel(level(severity))
                .addKeyValue("exceptionCategory", category.name())
                .addKeyValue("alertRequired", category != ExceptionCategory.BUSINESS)
                .addKeyValue("code", common == null ? ResultCode.SYSTEM_ERROR.getCode() : common.getCode())
                .addKeyValue("status", safe(common == null ? ResultCode.SYSTEM_ERROR.getStatus() : common.getStatus()))
                .addKeyValue("retryable", common != null && common.isRetryable())
                .addKeyValue("exceptionClass", classified.getClass().getName())
                .addKeyValue("exceptionLevel", severity.name())
                .addKeyValue("traceId", safe(trace.traceId()))
                .addKeyValue("requestId", safe(trace.requestId()));
        if (category != ExceptionCategory.BUSINESS) {
            event.setCause(sanitized(failure, Collections.newSetFromMap(new IdentityHashMap<>()), 0));
        }
        event.log("exception handled");
    }

    private static Throwable unwrap(Throwable failure) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        while ((failure instanceof CompletionException || failure instanceof ExecutionException)
                && failure.getCause() != null && seen.add(failure)) {
            failure = failure.getCause();
        }
        return failure;
    }

    private static Level level(ExceptionLevelEnum severity) {
        return switch (severity) {
            case TRACE -> Level.TRACE;
            case DEBUG -> Level.DEBUG;
            case INFO -> Level.INFO;
            case WARN -> Level.WARN;
            case ERROR, FATAL -> Level.ERROR;
        };
    }

    private static String safe(String value) {
        return value != null && value.matches("[A-Za-z0-9._:-]{1,128}") ? value : "unknown";
    }

    /** Suppress all free-form messages, retaining exception types, frames and bounded cause structure. */
    private static Throwable sanitized(Throwable original, Set<Throwable> seen, int depth) {
        if (original == null || depth >= 16 || !seen.add(original)) {
            return null;
        }
        RuntimeException safe = new RedactedException(original.getClass().getName());
        safe.setStackTrace(original.getStackTrace());
        Throwable cause = sanitized(original.getCause(), seen, depth + 1);
        if (cause != null) {
            safe.initCause(cause);
        }
        Throwable[] suppressed = original.getSuppressed();
        for (int index = 0; index < Math.min(suppressed.length, 8); index++) {
            Throwable item = sanitized(suppressed[index], seen, depth + 1);
            if (item != null) {
                safe.addSuppressed(item);
            }
        }
        return safe;
    }

    private static final class RedactedException extends RuntimeException {
        @Serial
        private static final long serialVersionUID = 1L;

        private RedactedException(String originalClass) {
            super(originalClass + ": [message redacted]");
        }
    }
}
