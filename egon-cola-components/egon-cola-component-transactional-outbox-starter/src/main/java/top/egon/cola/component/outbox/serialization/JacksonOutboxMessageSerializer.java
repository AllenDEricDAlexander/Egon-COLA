package top.egon.cola.component.outbox.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.component.outbox.exception.OutboxSerializationException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class JacksonOutboxMessageSerializer implements OutboxMessageSerializer {

    private final ObjectMapper objectMapper;

    public JacksonOutboxMessageSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public SerializedOutboxPayload serialize(Object payload, String contentType) {
        try {
            String text = payload instanceof String stringPayload && supportsRawString(contentType)
                    ? stringPayload
                    : objectMapper.writeValueAsString(payload);
            return new SerializedOutboxPayload(text, text.getBytes(StandardCharsets.UTF_8).length);
        } catch (JsonProcessingException exception) {
            throw new OutboxSerializationException("Failed to serialize outbox payload", exception);
        }
    }

    private boolean supportsRawString(String contentType) {
        if (contentType == null) {
            return false;
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        return normalized.startsWith("text/")
                || normalized.equals("application/json")
                || normalized.startsWith("application/") && normalized.endsWith("+json")
                || normalized.equals("application/xml")
                || normalized.startsWith("application/") && normalized.endsWith("+xml");
    }
}
