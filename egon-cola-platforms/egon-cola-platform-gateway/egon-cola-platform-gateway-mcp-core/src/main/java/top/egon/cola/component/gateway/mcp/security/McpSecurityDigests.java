package top.egon.cola.component.gateway.mcp.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public final class McpSecurityDigests {

    private McpSecurityDigests() {
    }

    public static String token(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("approval token is required");
        }
        return sha256(token.getBytes(StandardCharsets.UTF_8));
    }

    public static String arguments(
            ObjectMapper objectMapper,
            Map<String, Object> arguments) {
        Objects.requireNonNull(objectMapper, "objectMapper");
        Objects.requireNonNull(arguments, "arguments");
        try {
            JsonNode tree = objectMapper.valueToTree(arguments);
            return sha256(objectMapper.writeValueAsBytes(canonical(tree)));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "MCP arguments cannot be canonicalized",
                    exception
            );
        }
    }

    private static Object canonical(JsonNode value) {
        if (value.isObject()) {
            TreeMap<String, Object> sorted = new TreeMap<>();
            value.properties().forEach(entry -> sorted.put(
                    entry.getKey(),
                    canonical(entry.getValue())
            ));
            return sorted;
        }
        if (value.isArray()) {
            ArrayList<Object> items = new ArrayList<>(value.size());
            value.forEach(item -> items.add(canonical(item)));
            return items;
        }
        if (value.isNull()) {
            return null;
        }
        if (value.isTextual()) {
            return value.textValue();
        }
        if (value.isBoolean()) {
            return value.booleanValue();
        }
        if (value.isIntegralNumber()) {
            return value.bigIntegerValue();
        }
        if (value.isFloatingPointNumber()) {
            return value.decimalValue();
        }
        if (value.isBinary()) {
            try {
                return value.binaryValue();
            } catch (java.io.IOException exception) {
                throw new IllegalArgumentException(
                        "MCP binary argument cannot be canonicalized",
                        exception
                );
            }
        }
        return value.asText();
    }

    private static String sha256(byte[] value) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value)
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
