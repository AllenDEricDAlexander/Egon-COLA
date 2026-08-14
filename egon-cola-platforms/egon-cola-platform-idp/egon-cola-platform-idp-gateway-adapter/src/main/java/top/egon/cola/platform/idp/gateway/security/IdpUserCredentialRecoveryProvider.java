package top.egon.cola.platform.idp.gateway.security;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.core.exchange.GatewayExchange;
import top.egon.cola.component.gateway.core.security.AuthenticationFailure;
import top.egon.cola.component.gateway.core.security.CredentialRecoveryResult;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.GatewayCredential;
import top.egon.cola.component.gateway.core.security.GatewayCredentialRecoveryProvider;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Recovers one USER AT from the RT cookie, then verifies it before retrying the request.
 */
public final class IdpUserCredentialRecoveryProvider
        implements GatewayCredentialRecoveryProvider {

    public static final String PROVIDER_ID = "idp-user-refresh";

    private final IdpRefreshClient client;
    private final IdpGatewayJwtVerifier verifier;
    private final IdpReservedHeaderSanitizer sanitizer;
    private final String refreshCookieName;
    private final String accessCookieName;

    public IdpUserCredentialRecoveryProvider(
            IdpRefreshClient client,
            IdpGatewayJwtVerifier verifier,
            IdpReservedHeaderSanitizer sanitizer,
            String refreshCookieName,
            String accessCookieName) {
        this.client = Objects.requireNonNull(client, "client");
        this.verifier = Objects.requireNonNull(verifier, "verifier");
        this.sanitizer = Objects.requireNonNull(sanitizer, "sanitizer");
        this.refreshCookieName = required(refreshCookieName, "refreshCookieName");
        this.accessCookieName = required(accessCookieName, "accessCookieName");
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public Publisher<CredentialRecoveryResult> recover(
            GatewayAuthContext context,
            GatewayExchange exchange,
            AuthenticationFailure failure) {
        if (failure != AuthenticationFailure.MISSING
                && failure != AuthenticationFailure.EXPIRED) {
            return Mono.just(CredentialRecoveryResult.notRecoverable());
        }
        String refreshToken = cookie(exchange, refreshCookieName);
        if (refreshToken == null) {
            return Mono.just(CredentialRecoveryResult.notRecoverable());
        }
        return client.refresh(refreshToken)
                .map(response -> response(response, context))
                .onErrorReturn(CredentialRecoveryResult.failed());
    }

    private CredentialRecoveryResult response(
            IdpRefreshClient.Response response,
            GatewayAuthContext context) {
        if (response.status() == 401 || response.status() == 400) {
            return new CredentialRecoveryResult(
                    CredentialRecoveryResult.Outcome.NOT_RECOVERABLE,
                    null,
                    sanitizer.fieldsToRemove(),
                    expireCookies());
        }
        if (response.status() >= 500) {
            return CredentialRecoveryResult.failed();
        }
        String accessToken = accessToken(response.headers());
        if (accessToken == null) {
            return CredentialRecoveryResult.failed();
        }
        IdpGatewayJwtVerifier.Verification verified = verifier.verify(
                context,
                accessToken);
        if (verified.failure() != top.egon.cola.component.gateway.core.security.AuthenticationFailure.NONE
                || !(verified.principal() instanceof IdentityPrincipal)) {
            return CredentialRecoveryResult.notRecoverable();
        }
        return CredentialRecoveryResult.recovered(
                new GatewayCredential("bearer", accessToken, Map.of(
                        "source", "refresh")),
                sanitizer.fieldsToRemove(),
                Map.of("set-cookie", List.of(
                        response.headers().getOrDefault("set-cookie", List.of()).getFirst())));
    }

    private String accessToken(Map<String, List<String>> headers) {
        for (String value : headers.getOrDefault("set-cookie", List.of())) {
            if (!value.startsWith(accessCookieName + "=")) {
                continue;
            }
            int end = value.indexOf(';');
            String token = value.substring(accessCookieName.length() + 1,
                    end < 0 ? value.length() : end);
            return token.isBlank() ? null : token;
        }
        return null;
    }

    private Map<String, List<String>> expireCookies() {
        String expiredAt = accessCookieName
                + "=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax";
        String expiredRt = refreshCookieName
                + "=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax";
        return Map.of("set-cookie", List.of(expiredAt, expiredRt));
    }

    private String cookie(GatewayExchange exchange, String name) {
        List<String> headers = new ArrayList<>();
        exchange.request().headers().names().stream()
                .filter(value -> "cookie".equalsIgnoreCase(value))
                .forEach(value -> headers.addAll(
                        exchange.request().headers().values(value)));
        String result = null;
        for (String header : headers) {
            for (String pair : header.split(";")) {
                int equals = pair.indexOf('=');
                if (equals <= 0 || !name.equals(pair.substring(0, equals).trim())) {
                    continue;
                }
                String value = pair.substring(equals + 1).trim();
                if (result != null && !result.equals(value)) {
                    return null;
                }
                result = value;
            }
        }
        return result == null || result.isBlank() ? null : result;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
