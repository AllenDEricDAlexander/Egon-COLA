package top.egon.cola.component.gateway.mcp.transport;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Transport-neutral MCP HTTP request passed from a concrete HTTP listener.
 */
public record McpHttpRequest(
        String method,
        String path,
        Map<String, String> headers,
        String body,
        Map<String, Object> attributes
) {

    public McpHttpRequest {
        method = required(method, "method").toUpperCase(Locale.ROOT);
        path = required(path, "path");
        headers = normalized(headers);
        body = body == null ? "" : body;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public String header(String name) {
        return name == null ? null : headers.get(
                name.toLowerCase(Locale.ROOT)
        );
    }

    private static Map<String, String> normalized(
            Map<String, String> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        source.forEach((name, value) -> result.put(
                required(name, "header name").toLowerCase(Locale.ROOT),
                required(value, "header value")
        ));
        return Collections.unmodifiableMap(result);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
