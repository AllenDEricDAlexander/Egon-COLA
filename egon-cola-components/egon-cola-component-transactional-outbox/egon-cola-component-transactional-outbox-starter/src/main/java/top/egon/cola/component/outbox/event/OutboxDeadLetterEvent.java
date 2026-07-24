package top.egon.cola.component.outbox.event;

public record OutboxDeadLetterEvent(
        String messageId,
        String channel,
        String destination,
        int attempt,
        String errorCode,
        String errorMessage,
        String traceId
) {
}
