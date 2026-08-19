package top.egon.cola.component.outbox.api;

public interface TransactionalOutbox {

    OutboxReceipt enqueue(OutboxMessage message);
}
