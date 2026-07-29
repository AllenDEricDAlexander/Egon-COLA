package top.egon.cola.component.accessguard.core.plan;

import top.egon.cola.component.accessguard.execution.RejectionMode;
import top.egon.cola.component.accessguard.execution.TimeLimitMode;
import top.egon.cola.component.accessguard.execution.TimeLimiterType;

import java.time.Duration;
import java.util.Objects;

public record ExecutionConfig(
        TimeLimitConfig timeLimit,
        RejectionConfig rejection
) {

    public ExecutionConfig {
        timeLimit = Objects.requireNonNull(timeLimit, "timeLimit");
        rejection = Objects.requireNonNull(rejection, "rejection");
    }

    public record TimeLimitConfig(
            boolean enabled,
            TimeLimitMode mode,
            TimeLimiterType executor,
            Duration timeout,
            boolean cancelRunningTask
    ) {

        public TimeLimitConfig {
            mode = Objects.requireNonNull(mode, "mode");
            executor = Objects.requireNonNull(executor, "executor");
            timeout = Objects.requireNonNull(timeout, "timeout");
        }
    }

    public record RejectionConfig(
            RejectionMode mode,
            String fallbackMethod,
            String returnJson
    ) {

        public RejectionConfig {
            mode = Objects.requireNonNull(mode, "mode");
            fallbackMethod = fallbackMethod == null ? "" : fallbackMethod;
            returnJson = returnJson == null ? "" : returnJson;
        }
    }
}
