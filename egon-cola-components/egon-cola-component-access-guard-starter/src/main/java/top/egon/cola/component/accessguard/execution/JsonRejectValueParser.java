package top.egon.cola.component.accessguard.execution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Objects;
import java.util.function.Supplier;

public final class JsonRejectValueParser {

    private final Supplier<ObjectMapper> objectMapper;

    public JsonRejectValueParser(ObjectMapper objectMapper) {
        this(() -> Objects.requireNonNull(objectMapper, "objectMapper"));
    }

    public JsonRejectValueParser(Supplier<ObjectMapper> objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public Object parse(String json, Class<?> returnType) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("returnJson must not be blank");
        }
        try {
            return objectMapper.get().readValue(json, Objects.requireNonNull(returnType, "returnType"));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("returnJson is invalid for " + returnType.getName(), exception);
        }
    }
}
