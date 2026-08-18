package top.egon.cola.platform.idp.gateway.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import top.egon.cola.platform.idp.core.token.RefreshTokenStatus;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Bounded Reactor Netty client for the IdP internal refresh-token status endpoint.
 */
public final class ReactorNettyIdpRefreshTokenStatusClient
        implements IdpRefreshTokenStatusClient {

    private final URI statusUri;
    private final Supplier<String> serviceAccessToken;
    private final ObjectMapper objectMapper;
    private final Duration timeout;
    private final HttpClient client;

    public ReactorNettyIdpRefreshTokenStatusClient(
            String statusUri,
            Supplier<String> serviceAccessToken,
            ObjectMapper objectMapper,
            Duration timeout) {
        this.statusUri = URI.create(required(statusUri, "statusUri"));
        this.serviceAccessToken = Objects.requireNonNull(
                serviceAccessToken, "serviceAccessToken");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        this.client = HttpClient.create().responseTimeout(timeout);
    }

    @Override
    public Mono<Response> validate(String refreshToken) {
        String serviceToken = required(serviceAccessToken.get(), "serviceAccessToken");
        String token = required(refreshToken, "refreshToken");
        return client.headers(headers -> headers
                        .add("content-type", "application/x-www-form-urlencoded")
                        .add("authorization", "Bearer " + serviceToken))
                .post()
                .uri(statusUri.toString())
                .sendForm((request, form) -> form.attr("token", token))
                .responseSingle((response, body) -> body
                        .asString()
                        .map(value -> parse(response.status().code(), value)))
                .timeout(timeout)
                .cast(Response.class);
    }

    private Response parse(int status, String body) {
        if (status != 200) {
            return new Response(status, null);
        }
        try {
            JsonNode data = objectMapper.readTree(body).path("data");
            RefreshTokenStatus tokenStatus = new RefreshTokenStatus(
                    data.path("subject").asText(null),
                    data.path("tenantId").asText(null),
                    Instant.parse(data.path("expiresAt").asText()));
            return new Response(status, tokenStatus);
        } catch (Exception exception) {
            throw new IllegalStateException("invalid refresh status response", exception);
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
