package top.egon.cola.component.outbox.store;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    RETRY_WAIT,
    SUCCEEDED,
    DEAD
}
