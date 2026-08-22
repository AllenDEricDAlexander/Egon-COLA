package top.egon.cola.platform.idp.starter.client;

import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Narrow Spring Security OAuth2 Client facade for IdP SERVICE tokens.
 *
 * <p>It owns the complete authorization cache key and bounded per-key
 * single-flight behavior while leaving registration credentials in Spring's
 * configured {@code ClientRegistration}.</p>
 */
public class IdpServiceOAuth2Client {

    private final OAuth2AuthorizedClientManager manager;
    private final IdpClientCredentialsRequestEntityConverter converter;
    private final Clock clock;
    private final Duration renewalSkew;
    private final Map<ServiceAuthorizationKey, OAuth2AccessToken> cache =
            new ConcurrentHashMap<>();
    private final Map<ServiceAuthorizationKey, CompletableFuture<OAuth2AccessToken>>
            inFlight = new ConcurrentHashMap<>();

    /** Creates a facade with the default request converter. */
    public IdpServiceOAuth2Client(
            OAuth2AuthorizedClientManager manager,
            Clock clock,
            Duration renewalSkew
    ) {
        this(
                manager,
                new IdpClientCredentialsRequestEntityConverter(),
                clock,
                renewalSkew
        );
    }

    /** Creates a facade with the auto-configured request converter. */
    public IdpServiceOAuth2Client(
            OAuth2AuthorizedClientManager manager,
            IdpClientCredentialsRequestEntityConverter converter,
            Clock clock,
            Duration renewalSkew
    ) {
        this.manager = Objects.requireNonNull(manager, "manager");
        this.converter = Objects.requireNonNull(converter, "converter");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.renewalSkew = Objects.requireNonNull(
                renewalSkew,
                "renewalSkew"
        );
        if (renewalSkew.isNegative()) {
            throw new IllegalArgumentException("renewalSkew must be positive");
        }
    }

    /**
     * Obtains an access token for exactly one validated authorization key.
     *
     * @param request typed target/context/scope request
     * @return Spring OAuth2 access token for the requested key
     */
    public OAuth2AccessToken authorize(IdpServiceTokenRequest request) {
        ServiceAuthorizationKey key = ServiceAuthorizationKey.from(request);
        OAuth2AccessToken cached = cache.get(key);
        if (usable(cached)) {
            return cached;
        }

        CompletableFuture<OAuth2AccessToken> candidate =
                new CompletableFuture<>();
        CompletableFuture<OAuth2AccessToken> existing = inFlight.putIfAbsent(
                key,
                candidate
        );
        if (existing != null) {
            return join(existing);
        }

        try {
            converter.bind(
                    key.audience(),
                    key.context(),
                    key.tenantId(),
                    key.scopes()
            );
            OAuth2AuthorizeRequest authorizeRequest =
                    OAuth2AuthorizeRequest
                            .withClientRegistrationId(key.registrationId())
                            .principal(key.principalName())
                            .attribute("egon.idp.app_id", key.appId())
                            .attribute("egon.idp.resource", key.audience())
                            .attribute("egon.idp.scope_context", key.context())
                            .attribute("egon.idp.tenant_id", key.tenantId())
                            .attribute("egon.idp.scopes", key.scopes())
                            .build();
            OAuth2AuthorizedClient authorized = manager.authorize(
                    authorizeRequest
            );
            OAuth2AccessToken token = authorized == null
                    ? null
                    : authorized.getAccessToken();
            validateToken(token, key);
            cache.put(key, token);
            candidate.complete(token);
            return token;
        } catch (OAuth2AuthorizationException exception) {
            IdpServiceAuthorizationException failure =
                    new IdpServiceAuthorizationException(
                            exception.getError().getErrorCode(),
                            exception
                    );
            candidate.completeExceptionally(failure);
            throw failure;
        } catch (IdpServiceAuthorizationException exception) {
            candidate.completeExceptionally(exception);
            throw exception;
        } catch (RuntimeException exception) {
            IdpServiceAuthorizationException failure =
                    new IdpServiceAuthorizationException(
                            "authorization_failed",
                            exception
                    );
            candidate.completeExceptionally(failure);
            throw failure;
        } finally {
            converter.clear();
            inFlight.remove(key, candidate);
        }
    }

    private OAuth2AccessToken join(
            CompletableFuture<OAuth2AccessToken> future
    ) {
        try {
            return future.join();
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof IdpServiceAuthorizationException failure) {
                throw failure;
            }
            throw new IdpServiceAuthorizationException(
                    "authorization_failed",
                    cause == null ? exception : cause
            );
        }
    }

    private boolean usable(OAuth2AccessToken token) {
        return token != null
                && token.getExpiresAt() != null
                && token.getExpiresAt().isAfter(
                clock.instant().plus(renewalSkew)
        );
    }

    private void validateToken(
            OAuth2AccessToken token,
            ServiceAuthorizationKey key
    ) {
        if (token == null || !usable(token)
                || !token.getScopes().containsAll(key.scopes())) {
            throw new IdpServiceAuthorizationException(
                    "invalid_token_response",
                    null
            );
        }
    }

    /** Stable safe category for token endpoint/manager failures. */
    public static class IdpServiceAuthorizationException
            extends IllegalStateException {

        private final String errorCode;

        /** Creates an authorization failure without credential details. */
        public IdpServiceAuthorizationException(
                String errorCode,
                Throwable cause
        ) {
            super("IdP service token authorization failed: " + errorCode,
                    cause);
            this.errorCode = errorCode;
        }

        /** Returns the stable, non-secret OAuth/category error code. */
        public String errorCode() {
            return errorCode;
        }
    }
}
