package top.egon.cola.platform.tianquan.shoubing.starter.client;

import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.util.StringUtils;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext;
import top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder;
import top.egon.cola.platform.tianquan.shoubing.core.observability.SecurityMetricObservationBO;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry.RealmType;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry.TrustedIssuerVO;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry;

/**
 * Narrow Spring Security OAuth2 Client facade for Tianquan-Shoubing SERVICE tokens.
 *
 * <p>It owns the complete trusted authorization key and bounded per-key single-flight behavior
 * while leaving registration credentials in Spring's configured {@code ClientRegistration}.</p>
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class IdpServiceOAuth2Client implements AutoCloseable {

    private static final int MAX_CACHE_ENTRIES = 10_000;
    private static final Duration ACTIVE_KEY_WINDOW = Duration.ofMinutes(2);
    private static final long MAX_REFRESH_JITTER_MILLIS = 5_000L;

    @Qualifier("idpOAuth2AuthorizedClientManager")
    private final OAuth2AuthorizedClientManager manager;
    @Qualifier("idpClientCredentialsRequestEntityConverter")
    private final IdpClientCredentialsRequestEntityConverter converter;
    @Qualifier("idpClock")
    private final Clock clock;
    private final Duration renewalSkew;
    @Qualifier("clientRegistrationRepository")
    private final ClientRegistrationRepository registrations;
    @Qualifier("idpOAuth2AuthorizedClientService")
    private final OAuth2AuthorizedClientService authorizedClients;
    @Qualifier("trustedIssuerRegistry")
    private final TrustedIssuerRegistry trustedIssuerRegistry;
    @Qualifier("idpStarterProperties")
    private final IdpStarterProperties properties;
    @Qualifier("idpSecurityMetricsRecorder")
    private final IdpSecurityMetricsRecorder metrics;

    private final Map<ServiceAuthorizationKey, OAuth2AccessToken> cache =
            new ConcurrentHashMap<>();
    private final Map<ServiceAuthorizationKey, CompletableFuture<OAuth2AccessToken>>
            inFlight = new ConcurrentHashMap<>();
    private final Map<ServiceAuthorizationKey, IdpServiceTokenRequest> requests =
            new ConcurrentHashMap<>();
    private final Map<ServiceAuthorizationKey, AtomicReference<Instant>> lastUsed =
            new ConcurrentHashMap<>();
    private final Map<ServiceAuthorizationKey, ScheduledFuture<?>> refreshTasks =
            new ConcurrentHashMap<>();
    private final Map<ServiceAuthorizationKey, Long> refreshGenerations =
            new ConcurrentHashMap<>();
    private final Object cacheLock = new Object();
    private final ScheduledThreadPoolExecutor refreshScheduler = newRefreshScheduler();
    private final AtomicLong refreshGeneration = new AtomicLong();
    private volatile boolean closed;
    private final java.util.concurrent.atomic.AtomicBoolean metricFailureLogged = new java.util.concurrent.atomic.AtomicBoolean();

    /** Keeps the former constructor source-compatible; authorization fails closed without identity config. */
    @Deprecated
    public IdpServiceOAuth2Client(
            OAuth2AuthorizedClientManager manager,
            Clock clock,
            Duration renewalSkew
    ) {
        this(manager, new IdpClientCredentialsRequestEntityConverter(), clock,
                renewalSkew, null, null, null, null, IdpSecurityMetricsRecorder.NOOP);
    }

    /** Keeps the former constructor source-compatible; authorization fails closed without identity config. */
    @Deprecated
    public IdpServiceOAuth2Client(
            OAuth2AuthorizedClientManager manager,
            IdpClientCredentialsRequestEntityConverter converter,
            Clock clock,
            Duration renewalSkew
    ) {
        this(manager, converter, clock, renewalSkew, null, null, null, null, IdpSecurityMetricsRecorder.NOOP);
    }

    /** Validates and creates a facade with exact trusted registration identity sources. */
    public static IdpServiceOAuth2Client createConfigured(
            OAuth2AuthorizedClientManager manager,
            IdpClientCredentialsRequestEntityConverter converter,
            Clock clock,
            Duration renewalSkew,
            ClientRegistrationRepository registrations,
            OAuth2AuthorizedClientService authorizedClients,
            TrustedIssuerRegistry trustedIssuerRegistry,
            IdpStarterProperties properties) {
        return createConfigured(manager, converter, clock, renewalSkew, registrations, authorizedClients,
                trustedIssuerRegistry, properties, IdpSecurityMetricsRecorder.NOOP);
    }

    public static IdpServiceOAuth2Client createConfigured(
            OAuth2AuthorizedClientManager manager, IdpClientCredentialsRequestEntityConverter converter,
            Clock clock, Duration renewalSkew, ClientRegistrationRepository registrations,
            OAuth2AuthorizedClientService authorizedClients, TrustedIssuerRegistry trustedIssuerRegistry,
            IdpStarterProperties properties, IdpSecurityMetricsRecorder metrics) {
        Objects.requireNonNull(manager, "manager");
        Objects.requireNonNull(converter, "converter");
        Objects.requireNonNull(clock, "clock");
        if (renewalSkew == null || renewalSkew.isNegative()) {
            throw new IllegalArgumentException("renewalSkew must be non-negative");
        }
        Objects.requireNonNull(registrations, "registrations");
        Objects.requireNonNull(authorizedClients, "authorizedClients");
        Objects.requireNonNull(trustedIssuerRegistry, "trustedIssuerRegistry");
        Objects.requireNonNull(properties, "properties");
        properties.getServiceClient().validate();
        return new IdpServiceOAuth2Client(manager, converter, clock, renewalSkew,
                registrations, authorizedClients, trustedIssuerRegistry, properties, Objects.requireNonNull(metrics, "metrics"));
    }

    /** Obtains an access token for exactly one validated authorization key. */
    public OAuth2AccessToken authorize(IdpServiceTokenRequest request) {
        ServiceAuthorizationKey key = key(request);
        ensureOpen();
        OAuth2AccessToken cached = cache.get(key);
        boolean usable = usable(cached);
        recordMetric("token.cache.access", cached == null ? "MISS" : usable ? "HIT" : "EXPIRED", null);
        if (usable) {
            touch(key, request, cached);
            return cached;
        }
        if (cached != null) {
            evict(key);
        }
        return authorize(key, request);
    }

    /** Removes both the local token and Spring's underlying authorized-client entry. */
    public void invalidate(IdpServiceTokenRequest request) {
        evict(key(request));
    }

    int cacheSize() {
        return cache.size();
    }

    private OAuth2AccessToken authorize(
            ServiceAuthorizationKey key,
            IdpServiceTokenRequest request) {
        CompletableFuture<OAuth2AccessToken> candidate = new CompletableFuture<>();
        CompletableFuture<OAuth2AccessToken> existing = inFlight.putIfAbsent(key, candidate);
        if (existing != null) {
            return join(existing);
        }

        try {
            converter.bind(key.audience(), key.context(), key.tenantId(), key.scopes());
            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId(key.registrationId())
                    .principal(key.principalName())
                    .attribute("egon.tianquan-shoubing.app_id", key.appId())
                    .attribute("egon.tianquan-shoubing.resource", key.audience())
                    .attribute("egon.tianquan-shoubing.scope_context", key.context())
                    .attribute("egon.tianquan-shoubing.tenant_id", key.tenantId())
                    .attribute("egon.tianquan-shoubing.scopes", key.scopes())
                    .build();
            OAuth2AuthorizedClient authorized = acquire(authorizeRequest);
            OAuth2AccessToken token = authorized == null ? null : authorized.getAccessToken();
            validateToken(authorized, token, key);
            store(key, request, token);
            candidate.complete(token);
            return token;
        } catch (OAuth2AuthorizationException exception) {
            IdpServiceAuthorizationException failure = new IdpServiceAuthorizationException(
                    exception.getError().getErrorCode(), exception);
            completeFailure(candidate, key, failure);
            throw failure;
        } catch (IdpServiceAuthorizationException exception) {
            completeFailure(candidate, key, exception);
            throw exception;
        } catch (RuntimeException exception) {
            IdpServiceAuthorizationException failure = new IdpServiceAuthorizationException(
                    "authorization_failed", exception);
            completeFailure(candidate, key, failure);
            throw failure;
        } finally {
            converter.clear();
            inFlight.remove(key, candidate);
        }
    }

    private OAuth2AuthorizedClient acquire(OAuth2AuthorizeRequest request) {
        long started = System.nanoTime();
        String result = "FAILURE";
        try {
            OAuth2AuthorizedClient authorized = manager.authorize(request);
            if (authorized != null && authorized.getAccessToken() != null) { result = "SUCCESS"; }
            return authorized;
        } finally {
            recordMetric("token.acquire.duration", result,
                    Duration.ofNanos(Math.max(0, System.nanoTime() - started)));
        }
    }

    private void recordMetric(String signal, String result, Duration elapsed) {
        if (metrics == IdpSecurityMetricsRecorder.NOOP) { return; }
        try {
            metrics.record(SecurityMetricObservationBO.builder().signal(signal).result(result).duration(elapsed).build());
        } catch (RuntimeException failure) {
            if (metricFailureLogged.compareAndSet(false, true)) {
                log.warn("SERVICE token observation unavailable; failureType={}", failure.getClass().getSimpleName());
            }
        }
    }

    private ServiceAuthorizationKey key(IdpServiceTokenRequest request) {
        if (request == null) {
            throw new IdpServiceAuthorizationException("request_invalid", null);
        }
        validateAudience(request.audience());
        if (registrations == null || authorizedClients == null
                || trustedIssuerRegistry == null || properties == null) {
            throw new IdpServiceAuthorizationException(
                    "registration_identity_unavailable", null);
        }
        IdpStarterProperties.ServiceClient serviceClient = properties.getServiceClient();
        if (!Objects.equals(request.appId(), serviceClient.getAppId())) {
            throw new IdpServiceAuthorizationException("app_id_mismatch", null);
        }
        ClientRegistration registration = registrations.findByRegistrationId(
                request.registrationId());
        if (registration == null
                || !AuthorizationGrantType.CLIENT_CREDENTIALS.equals(
                registration.getAuthorizationGrantType())
                || !ClientAuthenticationMethod.CLIENT_SECRET_BASIC.equals(
                registration.getClientAuthenticationMethod())
                || !StringUtils.hasText(registration.getClientSecret())) {
            throw new IdpServiceAuthorizationException("registration_invalid", null);
        }
        String issuer = configuredIssuer(registration);
        URI issuerUri = secureUri(issuer, "issuer");
        validateTokenEndpoint(registration, issuerUri);
        validateTrustedIssuer(issuer, request);
        long credentialVersion;
        try {
            credentialVersion = serviceClient.credentialVersion(
                    request.registrationId());
        } catch (IllegalStateException invalid) {
            throw new IdpServiceAuthorizationException(
                    "credential_version_unconfigured", invalid);
        }
        return ServiceAuthorizationKey.from(
                request, issuerUri.toASCIIString(), registration.getClientId(),
                credentialVersion);
    }

    private String configuredIssuer(ClientRegistration registration) {
        String providerIssuer = registration.getProviderDetails().getIssuerUri();
        if (trustedIssuerRegistry.isEmpty()) {
            String legacyIssuer = properties.getIssuer();
            if (!StringUtils.hasText(legacyIssuer)
                    || (StringUtils.hasText(providerIssuer)
                    && !legacyIssuer.equals(providerIssuer))) {
                throw new IdpServiceAuthorizationException("issuer_untrusted", null);
            }
            return legacyIssuer;
        }
        if (!StringUtils.hasText(providerIssuer)) {
            throw new IdpServiceAuthorizationException("issuer_unconfigured", null);
        }
        return providerIssuer;
    }

    private void validateTrustedIssuer(
            String issuer,
            IdpServiceTokenRequest request) {
        if (trustedIssuerRegistry.isEmpty()) {
            return;
        }
        TrustedIssuerVO trusted = trustedIssuerRegistry.findByIssuer(issuer)
                .orElseThrow(() -> new IdpServiceAuthorizationException(
                        "issuer_untrusted", null));
        boolean tenantMatches = request.context() == ServiceTokenContext.TENANT
                && trusted.realmType() == RealmType.TENANT
                && Objects.equals(trusted.tenantId(), request.tenantId());
        boolean platformMatches = request.context() == ServiceTokenContext.PLATFORM
                && trusted.realmType() == RealmType.PLATFORM
                && request.tenantId() == null;
        if (!tenantMatches && !platformMatches) {
            throw new IdpServiceAuthorizationException("issuer_context_mismatch", null);
        }
    }

    private static void validateTokenEndpoint(
            ClientRegistration registration,
            URI issuer) {
        String tokenEndpoint = registration.getProviderDetails().getTokenUri();
        if (!StringUtils.hasText(tokenEndpoint)) {
            throw new IdpServiceAuthorizationException("token_endpoint_invalid", null);
        }
        URI tokenUri = secureUri(tokenEndpoint, "token endpoint");
        if (!sameOrigin(issuer, tokenUri)) {
            throw new IdpServiceAuthorizationException(
                    "token_endpoint_origin_mismatch", null);
        }
    }

    private static URI secureUri(String value, String field) {
        URI uri;
        try {
            uri = URI.create(Objects.requireNonNull(value, field));
        } catch (IllegalArgumentException malformed) {
            throw new IdpServiceAuthorizationException("registration_uri_invalid", malformed);
        }
        String scheme = uri.getScheme();
        boolean secure = "https".equalsIgnoreCase(scheme);
        boolean loopback = "http".equalsIgnoreCase(scheme) && isLoopback(uri.getHost());
        if (!uri.isAbsolute() || uri.isOpaque() || uri.getHost() == null
                || (!secure && !loopback) || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null
                || !uri.normalize().equals(uri) || !uri.toASCIIString().equals(value)) {
            throw new IdpServiceAuthorizationException("registration_uri_invalid", null);
        }
        return uri;
    }

    private static boolean isLoopback(String host) {
        return "127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host)
                || "::1".equals(host) || "[::1]".equals(host);
    }

    private static boolean sameOrigin(URI left, URI right) {
        return left.getScheme().equalsIgnoreCase(right.getScheme())
                && left.getHost().equalsIgnoreCase(right.getHost())
                && effectivePort(left) == effectivePort(right);
    }

    private static void validateAudience(URI audience) {
        URI uri = Objects.requireNonNull(audience, "audience");
        String scheme = uri.getScheme();
        boolean secure = "https".equalsIgnoreCase(scheme);
        boolean loopback = "http".equalsIgnoreCase(scheme) && isLoopback(uri.getHost());
        if (!uri.isAbsolute() || uri.isOpaque() || uri.getHost() == null
                || (!secure && !loopback) || uri.getUserInfo() != null
                || uri.getFragment() != null || !uri.normalize().equals(uri)
                || !uri.toASCIIString().equals(audience.toString())) {
            throw new IdpServiceAuthorizationException("audience_invalid", null);
        }
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private boolean usable(OAuth2AccessToken token) {
        return token != null && token.getExpiresAt() != null
                && token.getExpiresAt().isAfter(clock.instant().plus(renewalSkew));
    }

    private void validateToken(
            OAuth2AuthorizedClient authorized,
            OAuth2AccessToken token,
            ServiceAuthorizationKey key) {
        if (authorized == null || token == null || !usable(token)
                || !key.registrationId().equals(
                authorized.getClientRegistration().getRegistrationId())
                || !key.clientId().equals(authorized.getClientRegistration().getClientId())
                || !key.principalName().equals(authorized.getPrincipalName())
                || authorized.getRefreshToken() != null
                || !token.getTokenType().equals(TokenType.BEARER)
                || !token.getScopes().equals(key.scopes())) {
            throw new IdpServiceAuthorizationException("invalid_token_response", null);
        }
    }

    private void store(
            ServiceAuthorizationKey key,
            IdpServiceTokenRequest request,
            OAuth2AccessToken token) {
        synchronized (cacheLock) {
            ensureOpen();
            evictExpiredLocked();
            while (!cache.containsKey(key) && cache.size() >= MAX_CACHE_ENTRIES) {
                ServiceAuthorizationKey oldest = cache.entrySet().stream()
                        .min((left, right) -> left.getValue().getExpiresAt()
                                .compareTo(right.getValue().getExpiresAt()))
                        .map(Map.Entry::getKey)
                        .orElseThrow(() -> new IllegalStateException(
                                "service token cache capacity invariant failed"));
                evictLocked(oldest);
            }
            cancelRefreshLocked(key);
            cache.put(key, token);
            requests.put(key, request);
            lastUsed.computeIfAbsent(key,
                    ignored -> new AtomicReference<>(clock.instant()))
                    .set(clock.instant());
            try {
                scheduleRefreshLocked(key, request, token);
            } catch (RuntimeException failure) {
                evictLocked(key);
                throw failure;
            }
        }
    }

    private void touch(ServiceAuthorizationKey key,
                       IdpServiceTokenRequest request,
                       OAuth2AccessToken token) {
        synchronized (cacheLock) {
            AtomicReference<Instant> last = lastUsed.computeIfAbsent(key,
                    ignored -> new AtomicReference<>(clock.instant()));
            last.set(clock.instant());
            requests.put(key, request);
            ScheduledFuture<?> scheduled = refreshTasks.get(key);
            if (scheduled == null || scheduled.isDone()) {
                scheduleRefreshLocked(key, request, token);
            }
        }
    }

    private void scheduleRefreshLocked(
            ServiceAuthorizationKey key,
            IdpServiceTokenRequest request,
            OAuth2AccessToken token) {
        Instant now = clock.instant();
        long baseDelay = Duration.between(
                now, token.getExpiresAt().minus(renewalSkew)).toMillis();
        long jitterBound = Math.min(MAX_REFRESH_JITTER_MILLIS,
                Math.max(0L, baseDelay / 10L));
        long jitter = jitterBound == 0L ? 0L
                : ThreadLocalRandom.current().nextLong(jitterBound + 1L);
        long delay = Math.max(0L, baseDelay + jitter);
        requests.put(key, request);
        long generation = refreshGeneration.incrementAndGet();
        refreshGenerations.put(key, generation);
        refreshTasks.put(key, refreshScheduler.schedule(
                () -> refreshIfActive(key, generation), delay, TimeUnit.MILLISECONDS));
    }

    private void refreshIfActive(ServiceAuthorizationKey key, long generation) {
        synchronized (cacheLock) {
            if (!refreshGenerations.remove(key, generation)) {
                return;
            }
            refreshTasks.remove(key);
        }
        if (closed) {
            return;
        }
        AtomicReference<Instant> last = lastUsed.get(key);
        IdpServiceTokenRequest request = requests.get(key);
        if (last == null || request == null
                || last.get().plus(ACTIVE_KEY_WINDOW).isBefore(clock.instant())) {
            evict(key);
            return;
        }
        try {
            authorize(request);
        } catch (IdpServiceAuthorizationException failure) {
            log.warn("Tianquan-Shoubing SERVICE token refresh failed; errorCode={}",
                    failure.errorCode());
        }
    }

    private void evictExpiredLocked() {
        cache.entrySet().stream()
                .filter(entry -> !usable(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList()
                .forEach(this::evictLocked);
    }

    private void evict(ServiceAuthorizationKey key) {
        synchronized (cacheLock) {
            evictLocked(key);
        }
    }

    private void evictLocked(ServiceAuthorizationKey key) {
        cache.remove(key);
        requests.remove(key);
        lastUsed.remove(key);
        cancelRefreshLocked(key);
        removeAuthorizedClient(key);
    }

    private void cancelRefreshLocked(ServiceAuthorizationKey key) {
        ScheduledFuture<?> task = refreshTasks.remove(key);
        refreshGenerations.remove(key);
        if (task != null) {
            task.cancel(false);
        }
    }

    private void completeFailure(
            CompletableFuture<OAuth2AccessToken> candidate,
            ServiceAuthorizationKey key,
            IdpServiceAuthorizationException failure) {
        candidate.completeExceptionally(failure);
        try {
            removeAuthorizedClient(key);
        } catch (RuntimeException storageFailure) {
            failure.addSuppressed(storageFailure);
        }
    }

    private void removeAuthorizedClient(ServiceAuthorizationKey key) {
        if (authorizedClients != null && key != null) {
            authorizedClients.removeAuthorizedClient(
                    key.registrationId(), key.principalName());
        }
    }

    private OAuth2AccessToken join(CompletableFuture<OAuth2AccessToken> future) {
        try {
            return future.join();
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof IdpServiceAuthorizationException failure) {
                throw failure;
            }
            throw new IdpServiceAuthorizationException(
                    "authorization_failed", cause == null ? exception : cause);
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IdpServiceAuthorizationException("client_closed", null);
        }
    }

    @PreDestroy
    @Override
    public void close() {
        closed = true;
        synchronized (cacheLock) {
            refreshTasks.values().forEach(task -> task.cancel(false));
            refreshTasks.clear();
            refreshGenerations.clear();
            requests.clear();
            lastUsed.clear();
            cache.clear();
        }
        refreshScheduler.shutdownNow();
    }

    private static ScheduledThreadPoolExecutor newRefreshScheduler() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(4, task -> {
            Thread thread = new Thread(task, "tianquan-service-token-refresh");
            thread.setDaemon(true);
            return thread;
        });
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }

    /** Stable safe category for token endpoint/manager failures. */
    public static class IdpServiceAuthorizationException extends IllegalStateException {

        private final String errorCode;

        /** Creates an authorization failure without credential details in its message. */
        public IdpServiceAuthorizationException(String errorCode, Throwable cause) {
            super("Tianquan-Shoubing service token authorization failed: " + errorCode, cause);
            this.errorCode = errorCode;
        }

        /** Returns the stable, non-secret OAuth/category error code. */
        public String errorCode() {
            return errorCode;
        }
    }
}
