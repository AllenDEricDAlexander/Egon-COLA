package top.egon.cola.component.outbox.common.exception;

public class OutboxTransactionMismatchException extends OutboxException {

    public OutboxTransactionMismatchException(String message) {
        super(message);
    }
}
