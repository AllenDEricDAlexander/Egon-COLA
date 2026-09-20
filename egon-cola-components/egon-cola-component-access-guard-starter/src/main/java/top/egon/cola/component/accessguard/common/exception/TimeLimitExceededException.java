package top.egon.cola.component.accessguard.common.exception;

import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;

import java.io.Serial;
import java.time.Duration;
import java.util.Objects;

/**
 * Thrown when a guarded call exceeds its configured time limit.
 */
public final class TimeLimitExceededException extends CommonException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Duration timeout;

    public TimeLimitExceededException(Duration timeout) {
        super(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getStatus(),
                "Access Guard execution exceeded " + Objects.requireNonNull(timeout, "timeout"));
        this.timeout = timeout;
    }

    /** @return the configured timeout that was exceeded */
    public Duration timeout() {
        return timeout;
    }
}
