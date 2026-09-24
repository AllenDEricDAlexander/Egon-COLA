package top.egon.cola.component.outbox.common.exception;

import lombok.Getter;

import java.io.Serial;

/** Describes an explicit outbox state-machine rejection or execution failure. */
public class OutboxStateMachineException extends OutboxException {

    @Serial
    private static final long serialVersionUID = 1L;

    @Getter
    private final String reason;

    private final boolean retryable;

    public OutboxStateMachineException(String reason, boolean retryable, String message) {
        super(message);
        this.reason = requireReason(reason);
        this.retryable = retryable;
    }

    public OutboxStateMachineException(
            String reason,
            boolean retryable,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.reason = requireReason(reason);
        this.retryable = retryable;
    }

    @Override
    public boolean isRetryable() {
        return retryable;
    }

    private static String requireReason(String reason) {
        if (reason == null || !reason.matches("[A-Z][A-Z0-9_]{0,63}")) {
            throw new IllegalArgumentException("Invalid outbox state-machine reason");
        }
        return reason;
    }
}
