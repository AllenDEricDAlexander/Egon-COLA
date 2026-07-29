package top.egon.cola.component.accessguard.execution;

import java.time.Duration;
import java.util.Objects;

public final class TimeLimitExceededException extends RuntimeException {

    private final Duration timeout;

    public TimeLimitExceededException(Duration timeout) {
        super("Access Guard execution exceeded " + Objects.requireNonNull(timeout, "timeout"));
        this.timeout = timeout;
    }

    public Duration timeout() {
        return timeout;
    }
}
