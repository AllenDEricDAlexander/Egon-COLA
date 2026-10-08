package top.egon.cola.platform.tianquan.shoubing.yuheng.security;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.yuheng.core.exchange.GatewayExchange;
import top.egon.cola.component.yuheng.core.security.AuthenticationFailure;
import top.egon.cola.component.yuheng.core.security.CredentialRecoveryResult;
import top.egon.cola.component.yuheng.core.security.GatewayAuthContext;
import top.egon.cola.component.yuheng.core.security.GatewayCredentialOnlineStateResult;
import top.egon.cola.component.yuheng.core.security.GatewayCredentialRecoveryProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Stops legacy USER refresh-token recovery while retaining authenticated online-state checks.
 */
public final class IdpUserCredentialRecoveryProvider
        implements GatewayCredentialRecoveryProvider {

    public static final String PROVIDER_ID = "tianquan-shoubing-user-refresh";

    private final IdpReservedHeaderSanitizer sanitizer;
    private final IdpUserOnlineStateProvider onlineStateProvider;
    private final String refreshCookieName;
    private final String accessCookieName;

    public IdpUserCredentialRecoveryProvider(
            IdpRefreshClient client,
            IdpGatewayJwtVerifier verifier,
            IdpReservedHeaderSanitizer sanitizer,
            String refreshCookieName,
            String accessCookieName) {
        this(client, verifier, sanitizer, refreshCookieName, accessCookieName, null);
    }

    public IdpUserCredentialRecoveryProvider(
            IdpRefreshClient client,
            IdpGatewayJwtVerifier verifier,
            IdpReservedHeaderSanitizer sanitizer,
            String refreshCookieName,
            String accessCookieName,
            IdpUserOnlineStateProvider onlineStateProvider) {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(verifier, "verifier");
        this.sanitizer = Objects.requireNonNull(sanitizer, "sanitizer");
        this.onlineStateProvider = onlineStateProvider;
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
        if (cookie(exchange, refreshCookieName) == null
                && cookie(exchange, accessCookieName) == null) {
            return Mono.just(CredentialRecoveryResult.notRecoverable());
        }
        return Mono.just(new CredentialRecoveryResult(
                CredentialRecoveryResult.Outcome.NOT_RECOVERABLE,
                null,
                sanitizer.fieldsToRemove(),
                expireCookies()));
    }

    @Override
    public Publisher<GatewayCredentialOnlineStateResult> validateAuthenticated(
            GatewayAuthContext context,
            GatewayExchange exchange) {
        if (onlineStateProvider == null) {
            return Mono.just(GatewayCredentialOnlineStateResult.active());
        }
        return onlineStateProvider.validateAuthenticated(context, exchange);
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
