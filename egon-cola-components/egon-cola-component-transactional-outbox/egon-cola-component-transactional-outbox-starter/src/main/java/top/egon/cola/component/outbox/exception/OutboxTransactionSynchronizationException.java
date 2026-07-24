package top.egon.cola.component.outbox.exception;

public class OutboxTransactionSynchronizationException extends OutboxException {

    public OutboxTransactionSynchronizationException(String message) {
        super(message);
    }
}
