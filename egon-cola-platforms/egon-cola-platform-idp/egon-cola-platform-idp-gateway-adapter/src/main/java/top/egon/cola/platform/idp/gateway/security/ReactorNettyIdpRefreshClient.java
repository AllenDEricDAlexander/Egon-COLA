package top.egon.cola.platform.idp.gateway.security;

import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Bounded Reactor Netty client for the internal IdP refresh endpoint.
 */
public final class ReactorNettyIdpRefreshClient implements IdpRefreshClient {

    private final URI refreshUri;
    private final String refreshCookieName;
    private final String accessCookieName;
    private final Duration timeout;
    private final HttpClient client;

    public ReactorNettyIdpRefreshClient(
            String refreshUri,
            String refreshCookieName,
            String accessCookieName,
            Duration timeout) {
        this.refreshUri = URI.create(required(refreshUri, "refreshUri"));
        this.refreshCookieName = required(refreshCookieName, "refreshCookieName");
        this.accessCookieName = required(accessCookieName, "accessCookieName");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        this.client = HttpClient.create().responseTimeout(timeout);
    }

    @Override
    public Mono<Response> refresh(String refreshToken) {
        String cookie = refreshCookieName + "=" + required(refreshToken, "refreshToken");
        return client.headers(headers -> headers.add("content-type", "application/x-www-form-urlencoded")
                        .add("cookie", cookie))
                .post()
                .uri(refreshUri.toString())
                .sendForm((request, form) -> form.attr("grant_type", "refresh_token"))
                .responseSingle((response, body) -> body
                        .asString()
                        .map(ignored -> new IdpRefreshClient.Response(
                                response.status().code(),
                                collectHeaders(response.responseHeaders()))))
                .timeout(timeout)
                .cast(Response.class);
    }

    private Map<String, List<String>> collectHeaders(
            io.netty.handler.codec.http.HttpHeaders headers) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (String name : headers.names()) {
            if (!"set-cookie".equalsIgnoreCase(name)) {
                continue;
            }
            List<String> values = new ArrayList<>();
            headers.getAll(name).forEach(value -> {
                if (value.startsWith(accessCookieName + "=")) {
                    values.add(value);
                }
            });
            if (!values.isEmpty()) {
                result.put("set-cookie", List.copyOf(values));
            }
        }
        return Map.copyOf(result);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
