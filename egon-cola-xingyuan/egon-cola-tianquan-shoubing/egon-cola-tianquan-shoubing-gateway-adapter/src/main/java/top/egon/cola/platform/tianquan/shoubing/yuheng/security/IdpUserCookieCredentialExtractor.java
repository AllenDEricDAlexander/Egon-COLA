package top.egon.cola.platform.tianquan.shoubing.yuheng.security;

import org.reactivestreams.Publisher;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import reactor.core.publisher.Mono;
import top.egon.cola.component.yuheng.core.exchange.GatewayExchange;
import top.egon.cola.component.yuheng.core.security.CredentialExtractionResult;
import top.egon.cola.component.yuheng.core.security.GatewayAuthContext;
import top.egon.cola.component.yuheng.core.security.GatewayCredential;
import top.egon.cola.component.yuheng.core.security.GatewayCredentialExtractor;
import top.egon.cola.component.yuheng.core.security.GatewaySecurityPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Extracts only explicit Bearer credentials; the former capability identifier remains compatible.
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class IdpUserCookieCredentialExtractor
        implements GatewayCredentialExtractor {

    public static final String EXTRACTOR_ID = "tianquan-shoubing-user-cookie";
    private static final int MAX_TOKEN_LENGTH = 8192;

    @Qualifier("idpReservedHeaderSanitizer")
    private final IdpReservedHeaderSanitizer sanitizer;

    public IdpUserCookieCredentialExtractor(
            IdpReservedHeaderSanitizer sanitizer,
            String accessTokenCookieName,
            Set<String> trustedOrigins) {
        this(Objects.requireNonNull(sanitizer, "sanitizer"));
        required(accessTokenCookieName, "accessTokenCookieName");
    }

    public static IdpUserCookieCredentialExtractor createConfigured(IdpReservedHeaderSanitizer sanitizer) {
        return new IdpUserCookieCredentialExtractor(Objects.requireNonNull(sanitizer, "sanitizer"));
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
        String token = bearer;
        if (token == null) {
            return Mono.just(new CredentialExtractionResult(
                    List.of(),
                    sanitizer.fieldsToRemove(),
                    null));
        }
        return Mono.just(new CredentialExtractionResult(
                List.of(new GatewayCredential("bearer", token, Map.of(
                        "source", "bearer"))),
                sanitizer.fieldsToRemove(),
                null));
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
                "YUHENG_CREDENTIAL_INVALID");
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
