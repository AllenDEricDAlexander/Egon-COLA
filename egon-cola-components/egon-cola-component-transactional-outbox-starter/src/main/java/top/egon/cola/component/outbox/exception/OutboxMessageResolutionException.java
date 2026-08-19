package top.egon.cola.component.outbox.exception;

public class OutboxMessageResolutionException extends OutboxException {

    public OutboxMessageResolutionException(String message) {
        super(message);
    }

    public OutboxMessageResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
