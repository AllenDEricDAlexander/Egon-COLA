package top.egon.cola.component.outbox.common.exception;

public class OutboxTransactionRequiredException extends OutboxException {

    public OutboxTransactionRequiredException(String message) {
        super(message);
    }
}
