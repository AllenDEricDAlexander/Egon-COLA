package top.egon.cola.component.outbox.exception;

public class OutboxException extends RuntimeException {

    public OutboxException(String message) {
        super(message);
    }

    public OutboxException(String message, Throwable cause) {
        super(message, cause);
    }
}
