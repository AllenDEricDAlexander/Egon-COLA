package top.egon.cola.component.gateway.mcp.transport;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Transport-neutral MCP response supporting direct JSON and streaming SSE.
 */
public record McpHttpResponse(
        int status,
        Map<String, List<String>> headers,
        Publisher<byte[]> body,
        boolean flushPerEvent
) {

    public McpHttpResponse {
        if (status < 100 || status > 599) {
            throw new IllegalArgumentException("invalid HTTP status");
        }
        headers = normalized(headers);
        body = Objects.requireNonNull(body, "body");
    }

    public String header(String name) {
        if (name == null) {
            return null;
        }
        List<String> values = headers.get(name.toLowerCase(Locale.ROOT));
        return values == null || values.isEmpty() ? null : values.getFirst();
    }

    public static McpHttpResponse json(
            int status,
            String body,
            Map<String, List<String>> extraHeaders) {
        LinkedHashMap<String, List<String>> headers = new LinkedHashMap<>();
        headers.put(
                "content-type",
                List.of("application/json; charset=UTF-8")
        );
        if (extraHeaders != null) {
            headers.putAll(extraHeaders);
        }
        return new McpHttpResponse(
                status,
                headers,
                Flux.just(body.getBytes(StandardCharsets.UTF_8)),
                false
        );
    }

    public static McpHttpResponse empty(int status) {
        return new McpHttpResponse(status, Map.of(), Flux.empty(), false);
    }

    private static Map<String, List<String>> normalized(
            Map<String, List<String>> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, List<String>> result = new LinkedHashMap<>();
        source.forEach((name, values) -> result.put(
                Objects.requireNonNull(name, "header name")
                        .toLowerCase(Locale.ROOT),
                List.copyOf(Objects.requireNonNull(values, "header values"))
        ));
        return Collections.unmodifiableMap(result);
    }
}
