package top.egon.cola.component.accessguard.reject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.component.accessguard.exception.AccessGuardRejectResponseException;

public class JsonRejectResponseParser {

    private final ObjectMapper objectMapper;

    public JsonRejectResponseParser() {
        this(new ObjectMapper());
    }

    public JsonRejectResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Object parse(String returnJson, Class<?> returnType) {
        if (returnType == String.class) {
            return returnJson;
        }
        try {
            return objectMapper.readValue(returnJson, returnType);
        } catch (JsonProcessingException e) {
            throw new AccessGuardRejectResponseException("Failed to parse access guard returnJson", e);
        }
    }
}
