package top.egon.cola.component.outbox.common.exception;

public class OutboxMessageResolutionException extends OutboxException {

    public OutboxMessageResolutionException(String message) {
        super(message);
    }

    public OutboxMessageResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
