package top.egon.cola.component.outbox.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.exception.OutboxValidationException;
import top.egon.cola.component.outbox.serialization.SerializedOutboxPayload;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class OutboxMessageValidator {

    private static final Set<String> FORBIDDEN_HEADERS = Set.of(
            "authorization", "proxy-authorization", "cookie", "set-cookie",
            "host", "content-length", "transfer-encoding", "connection"
    );

    private final ObjectMapper objectMapper;
    private final int maximumPayloadBytes;
    private final int maximumHeaderCount;
    private final int maximumSerializedHeaderBytes;

    public OutboxMessageValidator(
            ObjectMapper objectMapper,
            int maximumPayloadBytes,
            int maximumHeaderCount,
            int maximumSerializedHeaderBytes
    ) {
        this.objectMapper = objectMapper;
        this.maximumPayloadBytes = maximumPayloadBytes;
        this.maximumHeaderCount = maximumHeaderCount;
        this.maximumSerializedHeaderBytes = maximumSerializedHeaderBytes;
    }

    public void validateEnvelope(OutboxMessage message) {
        if (message == null) {
            throw invalid("message");
        }
        validateOptional(message.messageId(), 64, "messageId");
        validateOptional(message.idempotencyKey(), 256, "idempotencyKey");
        validateRequired(message.channel(), 64, "channel");
        validateRequired(message.destination(), 256, "destination");
        if (message.payload() == null) {
            throw invalid("payload");
        }
        validateRequired(message.contentType(), 128, "contentType");
        validateOptional(message.schemaVersion(), 32, "schemaVersion");
        validateOptional(message.traceId(), 128, "traceId");
        validateHeaders(message.headers());
    }

    public void validateSerialized(SerializedOutboxPayload payload, Map<String, String> headers) {
        if (payload == null || payload.text() == null || payload.utf8Bytes() < 0) {
            throw invalid("payload");
        }
        if (payload.utf8Bytes() > maximumPayloadBytes) {
            throw new OutboxValidationException("Outbox payload exceeds configured byte limit");
        }
        validateHeaders(headers);
        try {
            int headerBytes = objectMapper.writeValueAsBytes(new TreeMap<>(headers)).length;
            if (headerBytes > maximumSerializedHeaderBytes) {
                throw new OutboxValidationException("Outbox headers exceed configured byte limit");
            }
        } catch (JsonProcessingException exception) {
            throw new OutboxValidationException("Outbox headers could not be serialized", exception);
        }
    }

    public static boolean isForbiddenHeader(String name) {
        return name != null && FORBIDDEN_HEADERS.contains(name.toLowerCase(java.util.Locale.ROOT));
    }

    private void validateHeaders(Map<String, String> headers) {
        if (headers == null) {
            throw invalid("headers");
        }
        if (headers.size() > maximumHeaderCount) {
            throw new OutboxValidationException("Outbox headers exceed configured count limit");
        }
        headers.forEach((name, value) -> {
            if (name == null || name.isBlank()) {
                throw invalid("header name");
            }
            if (value == null) {
                throw invalid("header value");
            }
            if (isForbiddenHeader(name)) {
                throw new OutboxValidationException(
                        "Outbox header '" + name.toLowerCase(java.util.Locale.ROOT) + "' is forbidden"
                );
            }
        });
    }

    private void validateRequired(String value, int maximumLength, String field) {
        if (value == null || value.isBlank() || value.length() > maximumLength) {
            throw invalid(field);
        }
    }

    private void validateOptional(String value, int maximumLength, String field) {
        if (value != null && (value.isBlank() || value.length() > maximumLength)) {
            throw invalid(field);
        }
    }

    private OutboxValidationException invalid(String field) {
        return new OutboxValidationException("Invalid outbox " + field);
    }
}
