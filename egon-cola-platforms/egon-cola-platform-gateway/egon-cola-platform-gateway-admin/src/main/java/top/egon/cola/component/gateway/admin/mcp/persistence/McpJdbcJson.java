package top.egon.cola.component.gateway.admin.mcp.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

final class McpJdbcJson {

    private static final TypeReference<Map<String, Object>> MAP =
            new TypeReference<>() {
            };

    private static final TypeReference<Set<String>> STRING_SET =
            new TypeReference<>() {
            };

    private final ObjectMapper objectMapper;

    McpJdbcJson(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy();
    }

    String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException(
                    "MCP persistence value cannot be serialized",
                    failure
            );
        }
    }

    Map<String, Object> map(String value) {
        try {
            return Map.copyOf(objectMapper.readValue(value, MAP));
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException(
                    "stored MCP persistence value is invalid",
                    failure
            );
        }
    }

    Set<String> stringSet(String value) {
        try {
            return Set.copyOf(objectMapper.readValue(value, STRING_SET));
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException(
                    "stored MCP string set is invalid",
                    failure
            );
        }
    }

    static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
