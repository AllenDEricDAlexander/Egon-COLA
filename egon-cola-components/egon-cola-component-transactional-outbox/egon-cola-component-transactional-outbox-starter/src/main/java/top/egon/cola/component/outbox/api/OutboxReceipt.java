package top.egon.cola.component.outbox.api;

public record OutboxReceipt(
        String messageId,
        String idempotencyKey,
        boolean created
) {
}
