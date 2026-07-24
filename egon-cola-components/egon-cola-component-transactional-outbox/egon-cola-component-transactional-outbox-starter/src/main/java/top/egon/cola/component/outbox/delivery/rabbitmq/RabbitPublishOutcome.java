package top.egon.cola.component.outbox.delivery.rabbitmq;

import java.util.Objects;

public record RabbitPublishOutcome(Kind kind, String reason, Integer replyCode) {

    public enum Kind {
        ACK,
        NACK,
        TIMEOUT,
        RETURNED
    }

    public RabbitPublishOutcome {
        Objects.requireNonNull(kind, "kind");
        reason = sanitize(reason);
    }

    public static RabbitPublishOutcome ack() {
        return new RabbitPublishOutcome(Kind.ACK, null, null);
    }

    public static RabbitPublishOutcome nack(String reason) {
        return new RabbitPublishOutcome(Kind.NACK, reason, null);
    }

    public static RabbitPublishOutcome timeout() {
        return new RabbitPublishOutcome(Kind.TIMEOUT, null, null);
    }

    public static RabbitPublishOutcome returned(int replyCode, String reason) {
        return new RabbitPublishOutcome(Kind.RETURNED, reason, replyCode);
    }

    private static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder sanitized = new StringBuilder(Math.min(value.length(), 256));
        value.codePoints().forEach(codePoint -> {
            if (sanitized.length() < 256) {
                sanitized.appendCodePoint(Character.isISOControl(codePoint) ? ' ' : codePoint);
            }
        });
        return sanitized.length() > 256 ? sanitized.substring(0, 256) : sanitized.toString();
    }
}
