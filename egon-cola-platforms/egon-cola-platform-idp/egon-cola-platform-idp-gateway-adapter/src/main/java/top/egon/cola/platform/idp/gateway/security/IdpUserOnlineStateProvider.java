package top.egon.cola.platform.idp.gateway.security;

import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.core.exchange.GatewayExchange;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.GatewayCredentialOnlineStateResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves IdP USER online state for an already authenticated Gateway principal.
 */
public final class IdpUserOnlineStateProvider {

    private final IdpRefreshTokenStatusClient client;
    private final String refreshCookieName;
    private final String accessCookieName;

    public IdpUserOnlineStateProvider(
            IdpRefreshTokenStatusClient client,
            String refreshCookieName,
            String accessCookieName) {
        this.client = Objects.requireNonNull(client, "client");
        this.refreshCookieName = required(refreshCookieName, "refreshCookieName");
        this.accessCookieName = required(accessCookieName, "accessCookieName");
    }

    public Mono<GatewayCredentialOnlineStateResult> validateAuthenticated(
            GatewayAuthContext context,
            GatewayExchange exchange) {
        String refreshToken = cookie(exchange, refreshCookieName);
        if (refreshToken == null) {
            return Mono.just(inactive());
        }
        return client.validate(refreshToken)
                .map(response -> evaluate(response, context))
                .onErrorReturn(GatewayCredentialOnlineStateResult.unavailable());
    }

    private GatewayCredentialOnlineStateResult evaluate(
            IdpRefreshTokenStatusClient.Response response,
            GatewayAuthContext context) {
        if (response.status() == 401 || response.status() == 400) {
            return inactive();
        }
        if (response.status() >= 500) {
            return GatewayCredentialOnlineStateResult.unavailable();
        }
        if (response.status() != 200 || response.tokenStatus() == null) {
            return GatewayCredentialOnlineStateResult.unavailable();
        }
        var status = response.tokenStatus();
        if (!context.principal().principalId().equals(status.subject())
                || !Objects.equals(context.principal().tenantId(), status.tenantId())
                || !status.expiresAt().isAfter(java.time.Instant.now())) {
            return inactive();
        }
        return GatewayCredentialOnlineStateResult.active();
    }

    private GatewayCredentialOnlineStateResult inactive() {
        String expiredAt = accessCookieName
                + "=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax";
        String expiredRt = refreshCookieName
                + "=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax";
        return GatewayCredentialOnlineStateResult.inactive(
                Map.of("set-cookie", List.of(expiredAt, expiredRt)));
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
