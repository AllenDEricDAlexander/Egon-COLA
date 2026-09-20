package top.egon.cola.component.outbox.common.exception;

public class OutboxTransactionSynchronizationException extends OutboxException {

    public OutboxTransactionSynchronizationException(String message) {
        super(message);
    }
}
