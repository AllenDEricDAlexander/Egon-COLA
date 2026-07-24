package top.egon.cola.component.outbox.delivery;

import java.time.Instant;
import java.util.Map;

public record DeliveryContext(
        String messageId,
        String channel,
        String destination,
        String payload,
        String contentType,
        String schemaVersion,
        Map<String, String> headers,
        String traceId,
        int attempt,
        int maxAttempts,
        Instant deadline
) {

    public DeliveryContext {
        headers = Map.copyOf(headers);
    }
}
