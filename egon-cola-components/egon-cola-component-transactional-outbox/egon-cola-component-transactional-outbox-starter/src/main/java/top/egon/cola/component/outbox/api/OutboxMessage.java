package top.egon.cola.component.outbox.api;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OutboxMessage {

    private final String messageId;
    private final String idempotencyKey;
    private final String channel;
    private final String destination;
    private final Object payload;
    private final String contentType;
    private final String schemaVersion;
    private final Map<String, String> headers;
    private final Instant availableAt;
    private final String traceId;

    private OutboxMessage(Builder builder) {
        this.messageId = builder.messageId;
        this.idempotencyKey = builder.idempotencyKey;
        this.channel = builder.channel;
        this.destination = builder.destination;
        this.payload = builder.payload;
        this.contentType = builder.contentType;
        this.schemaVersion = builder.schemaVersion;
        this.headers = Map.copyOf(builder.headers);
        this.availableAt = builder.availableAt;
        this.traceId = builder.traceId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String messageId() {
        return messageId;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public String channel() {
        return channel;
    }

    public String destination() {
        return destination;
    }

    public Object payload() {
        return payload;
    }

    public String contentType() {
        return contentType;
    }

    public String schemaVersion() {
        return schemaVersion;
    }

    public Map<String, String> headers() {
        return headers;
    }

    public Instant availableAt() {
        return availableAt;
    }

    public String traceId() {
        return traceId;
    }

    public static final class Builder {

        private String messageId;
        private String idempotencyKey;
        private String channel;
        private String destination;
        private Object payload;
        private String contentType = "application/json";
        private String schemaVersion;
        private final Map<String, String> headers = new LinkedHashMap<>();
        private Instant availableAt;
        private String traceId;

        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public Builder channel(String channel) {
            this.channel = channel;
            return this;
        }

        public Builder destination(String destination) {
            this.destination = destination;
            return this;
        }

        public Builder payload(Object payload) {
            this.payload = payload;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder schemaVersion(String schemaVersion) {
            this.schemaVersion = schemaVersion;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers.clear();
            if (headers != null) {
                this.headers.putAll(headers);
            }
            return this;
        }

        public Builder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        public Builder availableAt(Instant availableAt) {
            this.availableAt = availableAt;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public OutboxMessage build() {
            return new OutboxMessage(this);
        }
    }
}
