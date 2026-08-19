package top.egon.cola.component.outbox.exception;

public class OutboxTransactionMismatchException extends OutboxException {

    public OutboxTransactionMismatchException(String message) {
        super(message);
    }
}
