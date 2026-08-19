package top.egon.cola.component.outbox.exception;

public class OutboxSerializationException extends OutboxException {

    public OutboxSerializationException(String message) {
        super(message);
    }

    public OutboxSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
