package top.egon.cola.component.outbox.persistence.converter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.outbox.common.exception.OutboxValidationException;
import top.egon.cola.component.outbox.serialization.SerializedOutboxPayload;
import top.egon.cola.component.outbox.validation.OutboxMessageValidator;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Parses stored header JSON for the one-way message projection without rewriting stored bytes. */
@Slf4j
@Validated
@RequiredArgsConstructor
public class OutboxHeadersConverter {

    @Qualifier("outboxStateMachineObjectMapper")
    private final ObjectMapper objectMapper;

    @Qualifier("outboxMessageValidator")
    private final OutboxMessageValidator messageValidator;

    @Named("parseHeaders")
    public Map<String, String> parseHeaders(@NotBlank String headersJson) {
        if (headersJson == null || headersJson.isBlank()) {
            throw new OutboxValidationException("Outbox headers JSON must not be blank");
        }
        try {
            JsonNode root = objectMapper.readerFor(JsonNode.class)
                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readValue(headersJson);
            if (root == null || !root.isObject()) {
                throw new OutboxValidationException("Outbox headers JSON must be one object");
            }
            Map<String, String> headers = new LinkedHashMap<>();
            root.properties().forEach(entry -> {
                JsonNode value = entry.getValue();
                if (!value.isTextual()) {
                    throw new OutboxValidationException("Outbox header values must be strings");
                }
                headers.put(entry.getKey(), value.textValue());
            });
            messageValidator.validateSerialized(new SerializedOutboxPayload("", 0), headers);
            return Collections.unmodifiableMap(headers);
        } catch (IOException failure) {
            throw new OutboxValidationException("Outbox headers JSON is invalid", failure);
        }
    }
}
