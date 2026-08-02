package top.egon.cola.platform.idp.gateway.security;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.core.exchange.GatewayExchange;
import top.egon.cola.component.gateway.core.security.CredentialExtractionResult;
import top.egon.cola.component.gateway.core.security.GatewayCredential;
import top.egon.cola.component.gateway.core.security.GatewayCredentialExtractor;
import top.egon.cola.component.gateway.core.security.GatewaySecurityPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Accepts exactly one header Bearer and never reads credentials from query data.
 */
public final class IdpBearerCredentialExtractor
        implements GatewayCredentialExtractor {

    public static final String EXTRACTOR_ID = "idp-bearer";
    private static final int MAX_TOKEN_LENGTH = 8192;

    private final IdpReservedHeaderSanitizer sanitizer;

    public IdpBearerCredentialExtractor(IdpReservedHeaderSanitizer sanitizer) {
        this.sanitizer = Objects.requireNonNull(sanitizer, "sanitizer");
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
            GatewaySecurityPolicy policy
    ) {
        Objects.requireNonNull(exchange, "exchange");
        List<String> values = new ArrayList<>();
        exchange.request().headers().names().stream()
                .filter(name -> "authorization".equalsIgnoreCase(name))
                .forEach(name -> values.addAll(
                        exchange.request().headers().values(name)));
        if (values.isEmpty()) {
            return Mono.just(new CredentialExtractionResult(
                    List.of(), sanitizer.fieldsToRemove(), null));
        }
        if (values.size() != 1) {
            return Mono.just(invalid());
        }
        String value = values.getFirst();
        if (value == null || value.length() < "Bearer ".length()
                || !value.regionMatches(
                        true, 0, "Bearer ", 0, "Bearer ".length())) {
            return Mono.just(invalid());
        }
        String token = value.substring("Bearer ".length());
        if (token.isEmpty() || token.length() > MAX_TOKEN_LENGTH
                || token.chars().anyMatch(character ->
                Character.isWhitespace(character)
                        || Character.isISOControl(character)
                        || character == ',')) {
            return Mono.just(invalid());
        }
        return Mono.just(new CredentialExtractionResult(
                List.of(new GatewayCredential("bearer", token, Map.of())),
                sanitizer.fieldsToRemove(),
                null));
    }

    private CredentialExtractionResult invalid() {
        return new CredentialExtractionResult(
                List.of(),
                sanitizer.fieldsToRemove(),
                "GATEWAY_CREDENTIAL_INVALID");
    }
}
