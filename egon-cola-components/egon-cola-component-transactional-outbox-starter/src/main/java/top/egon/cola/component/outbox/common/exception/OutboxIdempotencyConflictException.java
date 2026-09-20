package top.egon.cola.component.outbox.common.exception;

public class OutboxIdempotencyConflictException extends OutboxException {

    public OutboxIdempotencyConflictException(String message) {
        super(message);
    }

    public OutboxIdempotencyConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
