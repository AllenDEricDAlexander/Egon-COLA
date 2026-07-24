package top.egon.cola.component.outbox.exception;

public class OutboxIdempotencyConflictException extends OutboxException {

    public OutboxIdempotencyConflictException(String message) {
        super(message);
    }

    public OutboxIdempotencyConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
