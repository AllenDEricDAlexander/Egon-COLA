package top.egon.cola.component.outbox.exception;

public class OutboxValidationException extends OutboxException {

    public OutboxValidationException(String message) {
        super(message);
    }

    public OutboxValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
