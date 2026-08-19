package top.egon.cola.component.outbox.exception;

public class OutboxStorageException extends OutboxException {

    public OutboxStorageException(String message) {
        super(message);
    }

    public OutboxStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
