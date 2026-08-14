package top.egon.cola.platform.idp.gateway.security;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.core.exchange.GatewayExchange;
import top.egon.cola.component.gateway.core.security.CredentialExtractionResult;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.GatewayCredential;
import top.egon.cola.component.gateway.core.security.GatewayCredentialExtractor;
import top.egon.cola.component.gateway.core.security.GatewaySecurityPolicy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Extracts the USER AT from the explicit Bearer header or the configured AT cookie.
 */
public final class IdpUserCookieCredentialExtractor
        implements GatewayCredentialExtractor {

    public static final String EXTRACTOR_ID = "idp-user-cookie";
    private static final int MAX_TOKEN_LENGTH = 8192;
    private static final Set<String> UNSAFE = Set.of(
            "POST", "PUT", "PATCH", "DELETE");

    private final IdpReservedHeaderSanitizer sanitizer;
    private final String accessTokenCookieName;
    private final Set<String> trustedOrigins;

    public IdpUserCookieCredentialExtractor(
            IdpReservedHeaderSanitizer sanitizer,
            String accessTokenCookieName,
            Set<String> trustedOrigins) {
        this.sanitizer = Objects.requireNonNull(sanitizer, "sanitizer");
        this.accessTokenCookieName = required(
                accessTokenCookieName,
                "accessTokenCookieName");
        this.trustedOrigins = trustedOrigins == null
                ? Set.of()
                : trustedOrigins.stream()
                .filter(Objects::nonNull)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override
    public String extractorId() {
        return EXTRACTOR_ID;
    }

    @Override
    public String credentialType() {
        return "bearer";
    }

    @Override
    public Publisher<CredentialExtractionResult> extract(
            GatewayExchange exchange,
            GatewaySecurityPolicy policy) {
        return extract(exchange, null, policy);
    }

    @Override
    public Publisher<CredentialExtractionResult> extract(
            GatewayExchange exchange,
            GatewayAuthContext context,
            GatewaySecurityPolicy policy) {
        Objects.requireNonNull(exchange, "exchange");
        List<String> authorization = headerValues(
                exchange,
                "authorization");
        String bearer = parseBearer(authorization);
        if ("".equals(bearer)) {
            return Mono.just(invalid());
        }
        Map<String, String> cookies = cookies(exchange);
        String cookieToken = cookies.get(accessTokenCookieName);
        if (cookieToken != null && !safeCookieRequest(exchange, context)) {
            return Mono.just(invalid());
        }
        if (bearer != null && cookieToken != null && !bearer.equals(cookieToken)) {
            return Mono.just(invalid());
        }
        String token = bearer == null ? cookieToken : bearer;
        if (token == null) {
            return Mono.just(new CredentialExtractionResult(
                    List.of(),
                    sanitizer.fieldsToRemove(),
                    null));
        }
        return Mono.just(new CredentialExtractionResult(
                List.of(new GatewayCredential("bearer", token, Map.of(
                        "source", bearer == null ? "cookie" : "bearer"))),
                sanitizer.fieldsToRemove(),
                null));
    }

    private boolean safeCookieRequest(
            GatewayExchange exchange,
            GatewayAuthContext context) {
        if (context == null || context.method() == null
                || !UNSAFE.contains(context.method().toUpperCase(Locale.ROOT))) {
            return true;
        }
        String origin = exchange.request().headers()
                .firstValue("origin")
                .orElseGet(() -> exchange.request().headers()
                        .firstValue("referer")
                        .map(this::originOf)
                        .orElse(null));
        return origin != null
                && trustedOrigins.contains(origin.toLowerCase(Locale.ROOT));
    }

    private String originOf(String referer) {
        try {
            java.net.URI uri = java.net.URI.create(referer);
            return uri.getScheme() + "://" + uri.getAuthority();
        } catch (RuntimeException invalid) {
            return null;
        }
    }

    private Map<String, String> cookies(GatewayExchange exchange) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String header : headerValues(exchange, "cookie")) {
            if (header == null || header.length() > 16384) {
                return Map.of();
            }
            for (String pair : header.split(";")) {
                int equals = pair.indexOf('=');
                if (equals <= 0) {
                    continue;
                }
                String name = pair.substring(0, equals).trim();
                String value = pair.substring(equals + 1).trim();
                if (name.isBlank() || value.length() > MAX_TOKEN_LENGTH) {
                    continue;
                }
                if (result.putIfAbsent(name, value) != null) {
                    return Map.of();
                }
            }
        }
        return Map.copyOf(result);
    }

    private List<String> headerValues(
            GatewayExchange exchange,
            String name) {
        List<String> result = new ArrayList<>();
        exchange.request().headers().names().stream()
                .filter(value -> name.equalsIgnoreCase(value))
                .forEach(value -> result.addAll(
                        exchange.request().headers().values(value)));
        return List.copyOf(result);
    }

    private String parseBearer(List<String> values) {
        if (values.isEmpty()) {
            return null;
        }
        if (values.size() != 1) {
            return "";
        }
        String value = values.getFirst();
        if (value == null || !value.regionMatches(
                true, 0, "Bearer ", 0, 7)) {
            return "";
        }
        String token = value.substring(7);
        if (token.isBlank() || token.length() > MAX_TOKEN_LENGTH
                || token.chars().anyMatch(Character::isWhitespace)) {
            return "";
        }
        return token;
    }

    private CredentialExtractionResult invalid() {
        return new CredentialExtractionResult(
                List.of(),
                sanitizer.fieldsToRemove(),
                "GATEWAY_CREDENTIAL_INVALID");
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
