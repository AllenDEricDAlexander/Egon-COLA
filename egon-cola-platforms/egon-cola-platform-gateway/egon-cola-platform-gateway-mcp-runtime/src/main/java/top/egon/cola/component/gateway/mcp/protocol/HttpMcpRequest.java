package top.egon.cola.component.gateway.mcp.protocol;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public record HttpMcpRequest(
        String path,
        String method,
        String contentType,
        Map<String, String> headers,
        String body
) {

    public HttpMcpRequest {
        path = required(path, "path");
        method = required(method, "method").toUpperCase(Locale.ROOT);
        contentType = required(contentType, "contentType")
                .toLowerCase(Locale.ROOT);
        LinkedHashMap<String, String> normalizedHeaders = new LinkedHashMap<>();
        if (headers != null) {
            headers.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(
                            String.CASE_INSENSITIVE_ORDER
                    ))
                    .forEach(entry -> normalizedHeaders.put(
                            required(entry.getKey(), "header name")
                                    .toLowerCase(Locale.ROOT),
                            required(entry.getValue(), "header value")
                    ));
        }
        headers = Collections.unmodifiableMap(normalizedHeaders);
        body = required(body, "body");
    }

    public String header(String name) {
        if (name == null) {
            return null;
        }
        return headers.get(name.toLowerCase(Locale.ROOT));
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
