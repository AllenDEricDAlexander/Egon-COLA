package top.egon.cola.component.outbox.store;

import java.time.Instant;

public record NewOutboxRecord(
        String messageId,
        String idempotencyKey,
        String messageFingerprint,
        String channel,
        String destination,
        String payload,
        String contentType,
        String schemaVersion,
        String headersJson,
        String traceId,
        Instant availableAt,
        int maxAttempts
) {
}
