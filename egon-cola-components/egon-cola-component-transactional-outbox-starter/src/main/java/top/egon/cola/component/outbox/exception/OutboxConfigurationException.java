package top.egon.cola.component.outbox.exception;

public class OutboxConfigurationException extends OutboxException {

    public OutboxConfigurationException(String message) {
        super(message);
    }

    public OutboxConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
