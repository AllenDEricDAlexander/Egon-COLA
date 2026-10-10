package top.egon.cola.platform.tianquan.shoubing.starter.security;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import top.egon.cola.platform.tianquan.shoubing.contract.IdpClaimNames;
import top.egon.cola.platform.tianquan.shoubing.contract.OAuthSecurityVersionBO;
import top.egon.cola.platform.tianquan.shoubing.contract.PrincipalType;
import top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder;
import top.egon.cola.platform.tianquan.shoubing.core.observability.SecurityMetricObservationBO;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry.RealmType;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry.TrustedIssuerVO;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.VerifiedJwtBO;

/** Resolves each token only to a local manager created from the exact configured issuer entry. */
@Slf4j
@RequiredArgsConstructor
public final class TenantIssuerAuthenticationManagerComponent {

    @Qualifier("trustedIssuerRegistry")
    private final TrustedIssuerRegistry trustedIssuerRegistry;
    @Qualifier("idpStarterProperties")
    private final IdpStarterProperties properties;
    @Qualifier("idpClock")
    private final Clock clock;
    private final java.util.concurrent.atomic.AtomicBoolean metricFailureLogged = new java.util.concurrent.atomic.AtomicBoolean();
    @Qualifier("idpSecurityMetricsRecorder")
    private final IdpSecurityMetricsRecorder metrics;
    private final ConcurrentMap<String, CachedIssuerManager> managers = new ConcurrentHashMap<>();
    private volatile BoundedJwkSetCache jwksCache;
    private volatile RestOperations jwksRestOperations;
    private volatile AuthenticationManagerResolver<String> trustedManagerResolver;
    private volatile JwtIssuerAuthenticationManagerResolver servletResolver;

    public TenantIssuerAuthenticationManagerComponent(
            TrustedIssuerRegistry trustedIssuerRegistry,
            IdpStarterProperties properties) {
        this(trustedIssuerRegistry, properties, Clock.systemUTC());
    }

    public TenantIssuerAuthenticationManagerComponent(TrustedIssuerRegistry registry, IdpStarterProperties properties, Clock clock) {
        this(registry, properties, clock, IdpSecurityMetricsRecorder.NOOP);
    }

    public synchronized void initialize() {
        if (trustedManagerResolver != null) {
            return;
        }
        if (trustedIssuerRegistry.isEmpty()) {
            this.jwksCache = new BoundedJwkSetCache(1, Duration.ofSeconds(300).toNanos(), metrics);
            this.jwksRestOperations = jwksRestOperations(properties.getJwksCache());
            this.trustedManagerResolver = this::resolveTrustedManager;
            this.servletResolver = new JwtIssuerAuthenticationManagerResolver(trustedManagerResolver);
            return;
        }
        properties.validate();
        if (trustedIssuerRegistry.activeIssuers().size() > properties.getJwksCache().getMaxIssuers()) {
            throw new IllegalStateException("trusted issuer count exceeds jwks-cache.max-issuers");
        }
        this.jwksCache = new BoundedJwkSetCache(
                properties.getJwksCache().getMaxIssuers(),
                properties.getJwksCache().getTtl().toNanos(), metrics);
        this.jwksRestOperations = jwksRestOperations(properties.getJwksCache());
        this.trustedManagerResolver = this::resolveTrustedManager;
        this.servletResolver = new JwtIssuerAuthenticationManagerResolver(trustedManagerResolver);
    }

    public boolean isConfigured() {
        return !trustedIssuerRegistry.isEmpty();
    }

    /** Resolves an HTTP request through Spring's issuer resolver and the exact local manager map. */
    public AuthenticationManagerResolver<jakarta.servlet.http.HttpServletRequest> servletResolver() {
        ensureInitialized();
        return servletResolver;
    }

    /** Parses an unverified issuer only to select a configured manager, then authenticates locally. */
    public VerifiedJwtBO authenticate(String compactToken) {
        ensureInitialized();
        String token = required(compactToken, "token");
        SignedJWT parsed = parseSignedToken(token);
        rejectUntrustedHeaders(parsed);
        String unverifiedIssuer = unverifiedIssuer(parsed);
        TrustedIssuerVO trustedIssuer = trustedIssuerRegistry.findByIssuer(unverifiedIssuer)
                .orElseThrow(() -> new InvalidIssuerException("JWT_ISSUER_UNTRUSTED"));
        AuthenticationManager manager = trustedManagerResolver.resolve(unverifiedIssuer);
        if (manager == null) {
            throw new InvalidIssuerException("JWT_ISSUER_UNTRUSTED");
        }
        var authentication = manager.authenticate(new BearerTokenAuthenticationToken(token));
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new InvalidIssuerException("JWT_AUTHENTICATION_RESULT_INVALID");
        }
        Jwt jwt = jwtAuthentication.getToken();
        validateIssuerBinding(jwt, trustedIssuer);
        return verifiedJwt(jwt, trustedIssuer);
    }

    private AuthenticationManager resolveTrustedManager(String issuer) {
        pruneInactiveManagers();
        TrustedIssuerVO trusted = trustedIssuerRegistry.findByIssuer(issuer).orElse(null);
        if (trusted == null) {
            return null;
        }
        return managers.compute(issuer, (key, current) -> {
            if (current != null && current.trustVersion() == trusted.trustVersion()
                    && current.jwksUri().equals(trusted.jwksUri())) {
                return current;
            }
            return new CachedIssuerManager(
                    trusted.trustVersion(),
                    trusted.jwksUri(),
                    manager(trusted));
        }).authenticationManager();
    }

    private void pruneInactiveManagers() {
        java.util.Set<String> active = trustedIssuerRegistry.activeIssuers().stream()
                .map(value -> value.issuer().toASCIIString())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        managers.keySet().retainAll(active);
    }

    private AuthenticationManager manager(TrustedIssuerVO trusted) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(
                        trusted.jwksUri().toASCIIString())
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .validateType(false)
                .restOperations(jwksRestOperations)
                .cache(jwksCache)
                .jwtProcessorCustomizer(processor -> {
                    var delegate = processor.getJWSVerifierFactory();
                    processor.setJWSVerifierFactory(new com.nimbusds.jose.proc.JWSVerifierFactory() {
                        @Override
                        public com.nimbusds.jose.JWSVerifier createJWSVerifier(com.nimbusds.jose.JWSHeader header,
                                java.security.Key key) throws com.nimbusds.jose.JOSEException {
                            return new ObservedJwsVerifier(delegate.createJWSVerifier(header, key), metrics, metricFailureLogged);
                        }

                        @Override
                        public java.util.Set<com.nimbusds.jose.JWSAlgorithm> supportedJWSAlgorithms() {
                            return delegate.supportedJWSAlgorithms();
                        }

                        @Override
                        public com.nimbusds.jose.jca.JCAContext getJCAContext() {
                            return delegate.getJCAContext();
                        }
                    });
                })
                .build();
        JwtTimestampValidator timestamps = new JwtTimestampValidator(properties.getClockSkew());
        timestamps.setClock(clock);
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                new JwtIssuerValidator(trusted.issuer().toASCIIString()),
                timestamps,
                jwt -> validateJwtHeadersAndTimes(jwt, trusted));
        decoder.setJwtValidator(validator);
        return new JwtAuthenticationProvider(decoder)::authenticate;
    }

    private OAuth2TokenValidatorResult validateJwtHeadersAndTimes(
            Jwt jwt,
            TrustedIssuerVO trusted) {
        Map<String, Object> headers = jwt.getHeaders();
        if (!"RS256".equals(headers.get("alg"))
                || !"at+jwt".equals(headers.get("typ"))
                || !(headers.get("kid") instanceof String keyId)
                || keyId.isBlank() || headers.containsKey("jku") || headers.containsKey("x5u")) {
            return failure("JWT_HEADER_INVALID");
        }
        if (jwt.getIssuedAt() == null || jwt.getNotBefore() == null || jwt.getExpiresAt() == null
                || !jwt.getExpiresAt().isAfter(jwt.getNotBefore())
                || jwt.getNotBefore().isBefore(jwt.getIssuedAt())) {
            return failure("JWT_TIME_INVALID");
        }
        if (jwt.getIssuedAt().isAfter(clock.instant().plus(properties.getClockSkew()))) {
            return failure("JWT_IAT_INVALID");
        }
        Duration maximumLifetime = Duration.ofSeconds(600).plus(properties.getClockSkew());
        if (Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).compareTo(maximumLifetime) > 0) {
            return failure("JWT_LIFETIME_INVALID");
        }
        if (!jwt.getIssuer().toExternalForm().equals(trusted.issuer().toASCIIString())) {
            return failure("JWT_ISSUER_INVALID");
        }
        return OAuth2TokenValidatorResult.success();
    }

    private void validateIssuerBinding(Jwt jwt, TrustedIssuerVO trusted) {
        String principalType = required(jwt.getClaimAsString(IdpClaimNames.PRINCIPAL_TYPE),
                IdpClaimNames.PRINCIPAL_TYPE);
        String scopeContext = required(jwt.getClaimAsString(IdpClaimNames.SCOPE_CONTEXT),
                IdpClaimNames.SCOPE_CONTEXT);
        String tenantId = jwt.getClaimAsString("tenant_id");
        String legacyTenantId = jwt.getClaimAsString("tid");
        if (legacyTenantId != null && tenantId != null && !legacyTenantId.equals(tenantId)) {
            throw new InvalidIssuerException("JWT_TENANT_CLAIM_CONFLICT");
        }
        if (trusted.realmType() == RealmType.TENANT) {
            if (!trusted.tenantId().equals(tenantId) || !"TENANT".equals(scopeContext)) {
                throw new InvalidIssuerException("JWT_ISSUER_TENANT_MISMATCH");
            }
        } else if (tenantId != null || legacyTenantId != null || !"PLATFORM".equals(scopeContext)
                || !PrincipalType.SERVICE.name().equals(principalType)) {
            throw new InvalidIssuerException("JWT_PLATFORM_CONTEXT_INVALID");
        }
        if (!List.of(PrincipalType.USER.name(), PrincipalType.SERVICE.name()).contains(principalType)) {
            throw new InvalidIssuerException("JWT_PRINCIPAL_TYPE_INVALID");
        }
        if ("USER".equals(principalType) && trusted.realmType() != RealmType.TENANT) {
            throw new InvalidIssuerException("JWT_PLATFORM_USER_UNSUPPORTED");
        }
    }

    private VerifiedJwtBO verifiedJwt(Jwt jwt, TrustedIssuerVO trusted) {
        String principalTypeValue = required(
                jwt.getClaimAsString(IdpClaimNames.PRINCIPAL_TYPE), IdpClaimNames.PRINCIPAL_TYPE);
        PrincipalType principalType;
        try {
            principalType = PrincipalType.valueOf(principalTypeValue);
        } catch (IllegalArgumentException invalid) {
            throw new InvalidIssuerException("JWT_PRINCIPAL_TYPE_INVALID", invalid);
        }
        VerifiedJwtBO result = VerifiedJwtBO.builder()
                .jwt(jwt)
                .trustedIssuer(trusted)
                .subject(required(jwt.getSubject(), "sub"))
                .tenantId(jwt.getClaimAsString("tenant_id"))
                .tokenId(required(jwt.getClaimAsString("jti"), "jti"))
                .keyId(required(jwt.getHeaders().get("kid") instanceof String value ? value : null, "kid"))
                .authorizationId(decimalClaim(jwt, "authorization_id"))
                .clientRegistrationId(decimalClaim(jwt, "client_registration_id"))
                .principalType(principalType)
                .expiresAt(Objects.requireNonNull(jwt.getExpiresAt(), "exp"))
                .securityVersion(securityVersion(jwt, principalType))
                .build();
        result.validate();
        return result;
    }

    private OAuthSecurityVersionBO securityVersion(Jwt jwt, PrincipalType principalType) {
        Object raw = jwt.getClaims().get("security_version");
        if (!(raw instanceof Map<?, ?> version)) {
            throw new InvalidIssuerException("JWT_SECURITY_VERSION_INVALID");
        }
        boolean user = principalType == PrincipalType.USER;
        if (version.size() != (user ? 3 : 2)
                || !version.containsKey("realm") || !version.containsKey("client")
                || user != version.containsKey("subject")) {
            throw new InvalidIssuerException("JWT_SECURITY_VERSION_INVALID");
        }
        return OAuthSecurityVersionBO.builder()
                .realm(nonNegativeLong(version.get("realm"), "realm"))
                .client(nonNegativeLong(version.get("client"), "client"))
                .subject(user ? nonNegativeLong(version.get("subject"), "subject") : null)
                .build();
    }

    private long nonNegativeLong(Object value, String field) {
        if (!(value instanceof Number number)) {
            throw new InvalidIssuerException("JWT_SECURITY_VERSION_INVALID_" + field.toUpperCase());
        }
        long normalized = number.longValue();
        if (normalized < 0L || number.doubleValue() != (double) normalized) {
            throw new InvalidIssuerException("JWT_SECURITY_VERSION_INVALID_" + field.toUpperCase());
        }
        return normalized;
    }

    private String decimalClaim(Jwt jwt, String claim) {
        String value = required(jwt.getClaimAsString(claim), claim);
        if (!value.matches("[1-9][0-9]{0,18}")) {
            throw new InvalidIssuerException("JWT_CLAIM_INVALID_" + claim.toUpperCase());
        }
        try {
            if (Long.parseLong(value) <= 0L) {
                throw new InvalidIssuerException("JWT_CLAIM_INVALID_" + claim.toUpperCase());
            }
        } catch (NumberFormatException invalid) {
            throw new InvalidIssuerException("JWT_CLAIM_INVALID_" + claim.toUpperCase(), invalid);
        }
        return value;
    }

    private SignedJWT parseSignedToken(String token) {
        try {
            JWT parsed = JWTParser.parse(token);
            if (!(parsed instanceof SignedJWT signed)) {
                throw new InvalidIssuerException("JWT_SIGNATURE_REQUIRED");
            }
            return signed;
        } catch (java.text.ParseException invalid) {
            throw new InvalidIssuerException("JWT_INVALID", invalid);
        }
    }

    private String unverifiedIssuer(SignedJWT token) {
        try {
            return required(token.getJWTClaimsSet().getIssuer(), "iss");
        } catch (java.text.ParseException invalid) {
            throw new InvalidIssuerException("JWT_INVALID", invalid);
        }
    }

    private void rejectUntrustedHeaders(SignedJWT token) {
        var header = token.getHeader();
        if (!"RS256".equals(header.getAlgorithm().getName())
                || header.getType() == null || !"at+jwt".equals(header.getType().toString())
                || header.getKeyID() == null || header.getKeyID().isBlank()
                || header.toJSONObject().containsKey("jku")
                || header.toJSONObject().containsKey("x5u")) {
            throw new InvalidIssuerException("JWT_HEADER_INVALID");
        }
    }

    private RestOperations jwksRestOperations(IdpStarterProperties.JwksCache jwks) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod)
                    throws IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(jwks.getConnectTimeout());
        factory.setReadTimeout(jwks.getReadTimeout());
        return new RestTemplate(factory);
    }

    private void ensureInitialized() {
        if (trustedManagerResolver == null) {
            initialize();
        }
    }

    private static OAuth2TokenValidatorResult failure(String code) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", code, null));
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new InvalidIssuerException("JWT_CLAIM_INVALID_" + name.toUpperCase());
        }
        return value;
    }

    private record CachedIssuerManager(long trustVersion, URI jwksUri,
                                       AuthenticationManager authenticationManager) {
    }

    public static final class InvalidIssuerException extends RuntimeException {

        public InvalidIssuerException(String message) {
            super(message);
        }

        public InvalidIssuerException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** Decorates only the cryptographic call; key acquisition and claims remain outside the timer. */
    @Slf4j
    @RequiredArgsConstructor(access = lombok.AccessLevel.PRIVATE)
    private static final class ObservedJwsVerifier implements com.nimbusds.jose.JWSVerifier {
        @Qualifier("jwsVerifier")
        private final com.nimbusds.jose.JWSVerifier delegate;
        @Qualifier("idpSecurityMetricsRecorder")
        private final IdpSecurityMetricsRecorder metrics;
        private final java.util.concurrent.atomic.AtomicBoolean metricFailureLogged;

        @Override
        public boolean verify(com.nimbusds.jose.JWSHeader header, byte[] signingInput,
                com.nimbusds.jose.util.Base64URL signature) throws com.nimbusds.jose.JOSEException {
            long started = System.nanoTime();
            String result = "FAILURE";
            try {
                boolean valid = delegate.verify(header, signingInput, signature);
                result = valid ? "SUCCESS" : "FAILURE";
                return valid;
            } finally {
                Duration elapsed = Duration.ofNanos(Math.max(0, System.nanoTime() - started));
                if (metrics != IdpSecurityMetricsRecorder.NOOP) {
                    try {
                        metrics.record(SecurityMetricObservationBO.builder().signal("jwt.verify.duration")
                                .result(result).duration(elapsed).build());
                    } catch (RuntimeException failure) {
                        if (metricFailureLogged.compareAndSet(false, true)) {
                            log.warn("JWT crypto observation unavailable; failureType={}", failure.getClass().getSimpleName());
                        }
                    }
                }
            }
        }

        @Override
        public java.util.Set<com.nimbusds.jose.JWSAlgorithm> supportedJWSAlgorithms() {
            return delegate.supportedJWSAlgorithms();
        }

        @Override
        public com.nimbusds.jose.jca.JCAContext getJCAContext() { return delegate.getJCAContext(); }
    }

    @Slf4j
    @RequiredArgsConstructor(access = lombok.AccessLevel.PRIVATE)
    private static final class BoundedJwkSetCache implements Cache {

        private final String name = "tianquanShoubingJwkSetCache";
        private final int maximumEntries;
        private final long ttlNanos;
        @Qualifier("idpSecurityMetricsRecorder")
        private final IdpSecurityMetricsRecorder metrics;
        private final java.util.concurrent.atomic.AtomicBoolean metricFailureLogged = new java.util.concurrent.atomic.AtomicBoolean();
        private final Map<Object, CacheEntry> entries = new LinkedHashMap<>(16, 0.75f, true);

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object getNativeCache() {
            return entries;
        }

        @Override
        public synchronized ValueWrapper get(Object key) {
            CacheEntry entry = entries.get(Objects.requireNonNull(key, "key"));
            if (entry == null) {
                recordAccess("MISS");
                return null;
            }
            if (System.nanoTime() - entry.expiresAtNanos() >= 0L) {
                entries.remove(key);
                recordAccess("EXPIRED");
                return null;
            }
            recordAccess("HIT");
            return entry::value;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Object key, Class<T> type) {
            ValueWrapper wrapper = get(key);
            if (wrapper == null) {
                return null;
            }
            Object value = wrapper.get();
            if (type != null && value != null && !type.isInstance(value)) {
                throw new IllegalStateException("cached JWK Set has an unexpected type");
            }
            return (T) value;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Object key, Callable<T> valueLoader) {
            ValueWrapper wrapper = get(key);
            if (wrapper != null) {
                return (T) wrapper.get();
            }
            try {
                T value = valueLoader.call();
                put(key, value);
                return value;
            } catch (Exception failure) {
                throw new ValueRetrievalException(key, valueLoader, failure);
            }
        }

        @Override
        public synchronized void put(Object key, Object value) {
            entries.put(Objects.requireNonNull(key, "key"),
                    new CacheEntry(value, System.nanoTime() + ttlNanos));
            while (entries.size() > maximumEntries) {
                entries.remove(entries.keySet().iterator().next());
            }
        }

        @Override
        public synchronized void evict(Object key) {
            entries.remove(key);
        }

        @Override
        public synchronized void clear() {
            entries.clear();
        }

        private void recordAccess(String result) {
            if (metrics == IdpSecurityMetricsRecorder.NOOP) { return; }
            try {
                metrics.record(SecurityMetricObservationBO.builder().signal("jwks.cache.access").result(result).build());
            } catch (RuntimeException failure) {
                if (metricFailureLogged.compareAndSet(false, true)) {
                    log.warn("JWK cache observation unavailable; failureType={}", failure.getClass().getSimpleName());
                }
            }
        }

        private record CacheEntry(Object value, long expiresAtNanos) {
        }
    }
}
