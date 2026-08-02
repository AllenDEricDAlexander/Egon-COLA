package top.egon.cola.component.gateway.test.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

final class McpTestHttpClient {

    private static final TypeReference<Map<String, Object>> MAP =
            new TypeReference<>() {
            };

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    Response post(
            URI endpoint,
            String token,
            Map<String, String> requestHeaders,
            Map<String, Object> body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(10))
                .header("content-type", "application/json")
                .header("accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(body)
                ));
        if (token != null && !token.isBlank()) {
            request.header("authorization", "Bearer " + token);
        }
        requestHeaders.forEach(request::header);
        HttpResponse<String> response = http.send(
                request.build(),
                HttpResponse.BodyHandlers.ofString()
        );
        Map<String, Object> decoded = response.body().isBlank()
                ? Map.of()
                : objectMapper.readValue(response.body(), MAP);
        return new Response(
                response.statusCode(),
                decoded,
                response.headers().map().entrySet().stream()
                        .collect(java.util.stream.Collectors.toUnmodifiableMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().getFirst()
                        ))
        );
    }

    Session openSse(URI endpoint, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(20))
                .header("accept", "text/event-stream")
                .GET();
        if (token != null && !token.isBlank()) {
            request.header("authorization", "Bearer " + token);
        }
        HttpResponse<java.util.stream.Stream<String>> response = http.sendAsync(
                        request.build(),
                        HttpResponse.BodyHandlers.ofLines()
                )
                .get();
        String data;
        try (var lines = response.body()) {
            data = lines.filter(line -> line.startsWith("data:"))
                    .map(line -> line.substring("data:".length()))
                    .findFirst()
                    .orElse("");
        }
        return new Session(
                response.statusCode(),
                response.headers().firstValue("mcp-session-id")
                        .orElseThrow(),
                data
        );
    }

    Map<String, Object> readSseEvent(
            URI endpoint,
            String token,
            String sessionId) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(20))
                .header("accept", "text/event-stream")
                .header("mcp-session-id", sessionId)
                .GET();
        if (token != null && !token.isBlank()) {
            request.header("authorization", "Bearer " + token);
        }
        HttpResponse<java.util.stream.Stream<String>> response = http.sendAsync(
                        request.build(),
                        HttpResponse.BodyHandlers.ofLines()
                )
                .get();
        String data;
        try (var lines = response.body()) {
            data = lines.filter(line -> line.startsWith("data:"))
                    .map(line -> line.substring("data:".length()))
                    .findFirst()
                    .orElseThrow();
        }
        return objectMapper.readValue(data, MAP);
    }

    static Map<String, Object> request(
            String id,
            String method,
            Map<String, Object> params) {
        LinkedHashMap<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", id);
        request.put("method", method);
        request.put("params", params);
        return Map.copyOf(request);
    }

    record Response(
            int status,
            Map<String, Object> body,
            Map<String, String> headers) {
    }

    record Session(int status, String sessionId, String endpoint) {
    }
}
