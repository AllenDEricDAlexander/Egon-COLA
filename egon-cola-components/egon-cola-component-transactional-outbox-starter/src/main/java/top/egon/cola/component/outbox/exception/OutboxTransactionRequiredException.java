package top.egon.cola.component.outbox.exception;

public class OutboxTransactionRequiredException extends OutboxException {

    public OutboxTransactionRequiredException(String message) {
        super(message);
    }
}
