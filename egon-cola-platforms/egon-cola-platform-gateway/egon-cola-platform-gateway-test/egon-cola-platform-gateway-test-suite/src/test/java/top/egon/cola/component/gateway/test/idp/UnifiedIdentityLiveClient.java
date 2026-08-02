package top.egon.cola.component.gateway.test.idp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;

final class UnifiedIdentityLiveClient {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    static UnifiedIdentityLiveClient enabled() {
        Assumptions.assumeTrue(Boolean.parseBoolean(
                System.getenv().getOrDefault("UNIFIED_IDENTITY_LIVE", "false")));
        return new UnifiedIdentityLiveClient();
    }

    int get(String baseUrl, String path, String accessToken)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(5))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        return http.send(request, HttpResponse.BodyHandlers.discarding())
                .statusCode();
    }

    String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }

    String token(String envName) throws IOException {
        return Files.readString(Path.of(requiredEnv(envName)), StandardCharsets.UTF_8)
                .trim();
    }

    JsonNode claims(String token) throws IOException {
        String[] segments = token.split("\\.");
        if (segments.length != 3) {
            throw new IllegalArgumentException("JWT must have three segments");
        }
        byte[] payload = Base64.getUrlDecoder().decode(segments[1]);
        return JSON.readTree(payload);
    }
}
