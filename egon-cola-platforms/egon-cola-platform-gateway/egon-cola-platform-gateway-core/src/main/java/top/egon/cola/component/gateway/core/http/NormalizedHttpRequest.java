package top.egon.cola.component.gateway.core.http;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record NormalizedHttpRequest(
        String method,
        String host,
        String rawPath,
        String normalizedPath,
        String rawQuery,
        Map<String, List<String>> headers
) {

    public NormalizedHttpRequest {
        method = required(method, "method");
        host = required(host, "host");
        rawPath = required(rawPath, "rawPath");
        normalizedPath = required(normalizedPath, "normalizedPath");
        rawQuery = rawQuery == null ? "" : rawQuery;
        headers = Map.copyOf(Objects.requireNonNull(headers, "headers"));
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
