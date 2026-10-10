package top.egon.cola.platform.tianquan.shoubing.starter.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.core.AuthenticationException;
import top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.contract.IdpClaimNames;
import top.egon.cola.platform.tianquan.shoubing.contract.IdpPrincipal;
import top.egon.cola.platform.tianquan.shoubing.contract.PrincipalType;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityOAuthClientStateReader;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerState;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerStateReader;
import top.egon.cola.platform.tianquan.shoubing.starter.security.OAuthRevocationReader.Result;
import top.egon.cola.platform.tianquan.shoubing.starter.security.OAuthRevocationReader.Status;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.VerifiedJwtBO;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Validates Tianquan-Shoubing USER and SERVICE access tokens.
 * USER validation is completely stateless; SERVICE validation keeps its existing
 * Resource/Client runtime checks.
 */
@Slf4j
@RequiredArgsConstructor(staticName = "create")
public final class IdpJwtVerifier {

    private static final Set<String> USER_FORBIDDEN_CLAIMS = Set.of(
            "sid", "session_id", "client_id", "token_version",
            "resource_server_id", "resource", "resource_version", "nonce",
            "roles", "permissions", "capabilities", "dataScopes", "fieldPolicies",
            "authVersion", "sessionVersion", "contextVersion");

    private static final Set<String> NEW_USER_FORBIDDEN_CLAIMS = Set.of(
            "sid", "session_id", "token_version", "resource_server_id", "resource",
            "resource_version", "nonce", "roles", "permissions", "capabilities",
            "dataScopes", "fieldPolicies", "authVersion", "sessionVersion", "contextVersion");

    @Qualifier("idpJwtDecoder")
    private final JwtDecoder decoder;
    @Qualifier("identityResourceServerStateReader")
    private final IdentityResourceServerStateReader resourceStates;
    @Qualifier("identityOAuthClientStateReader")
    private final IdentityOAuthClientStateReader clientStates;
    private final String resourceServerId;
    private final URI resourceUri;
    private final String platformAudience;
    @Qualifier("idpClock")
    private final Clock clock;
    @Qualifier("tenantIssuerAuthenticationManagerComponent")
    private final TenantIssuerAuthenticationManagerComponent tenantIssuerAuthenticationManager;
    @Qualifier("oauthRevocationReader")
    private final OAuthRevocationReader revocationReader;
    @Qualifier("verifiedPrincipalConverter")
    private final VerifiedPrincipalConverter verifiedPrincipalConverter;
    @Qualifier("legacyIssuerCutoverPolicy")
    private final LegacyIssuerCutoverPolicy legacyIssuerCutoverPolicy;
    @Qualifier("idpObservedVerificationComponent")
    private final IdpObservedVerificationComponent observations;

    /** Keeps the former stateful constructor as a source-compatible legacy adapter. */
    public IdpJwtVerifier(
            JwtDecoder decoder,
            IdentityResourceServerStateReader resourceStates,
            IdentityOAuthClientStateReader clientStates,
            String resourceServerId,
            URI resourceUri,
            String platformAudience,
            Clock clock
    ) {
        this(decoder, resourceStates, clientStates, resourceServerId, resourceUri,
                platformAudience, clock, null, null, null, null, null);
        Objects.requireNonNull(decoder, "decoder");
        Objects.requireNonNull(resourceStates, "resourceStates");
        Objects.requireNonNull(clientStates, "clientStates");
        required(resourceServerId, "resourceServerId");
        validResource(resourceUri);
        required(platformAudience, "platformAudience");
        Objects.requireNonNull(clock, "clock");
    }

    /** Keeps the former stateless USER constructor as a source-compatible adapter. */
    public IdpJwtVerifier(
            JwtDecoder decoder,
            String platformAudience,
            Clock clock) {
        this(decoder, null, null, null, null, platformAudience, clock,
                null, null, null, null, null);
        Objects.requireNonNull(decoder, "decoder");
        required(platformAudience, "platformAudience");
        Objects.requireNonNull(clock, "clock");
    }

    /** Validates and creates the configured Servlet verifier through the generated required-args constructor. */
    public static IdpJwtVerifier createConfigured(
            JwtDecoder decoder,
            IdentityResourceServerStateReader resourceStates,
            IdentityOAuthClientStateReader clientStates,
            String resourceServerId,
            URI resourceUri,
            String platformAudience,
            Clock clock,
            TenantIssuerAuthenticationManagerComponent tenantIssuerAuthenticationManager,
            OAuthRevocationReader revocationReader,
            VerifiedPrincipalConverter verifiedPrincipalConverter,
            LegacyIssuerCutoverPolicy legacyIssuerCutoverPolicy) {
        return createConfigured(decoder, resourceStates, clientStates, resourceServerId, resourceUri, platformAudience,
                clock, tenantIssuerAuthenticationManager, revocationReader, verifiedPrincipalConverter, legacyIssuerCutoverPolicy, null);
    }

    /** Creates the same single-pass verifier with one optional outer usage observer. */
    public static IdpJwtVerifier createConfigured(JwtDecoder decoder, IdentityResourceServerStateReader resourceStates,
            IdentityOAuthClientStateReader clientStates, String resourceServerId, URI resourceUri, String platformAudience,
            Clock clock, TenantIssuerAuthenticationManagerComponent tenantIssuerAuthenticationManager,
            OAuthRevocationReader revocationReader, VerifiedPrincipalConverter verifiedPrincipalConverter,
            LegacyIssuerCutoverPolicy legacyIssuerCutoverPolicy, IdpObservedVerificationComponent observations) {
        Objects.requireNonNull(resourceStates, "resourceStates");
        Objects.requireNonNull(clientStates, "clientStates");
        required(resourceServerId, "resourceServerId");
        validResource(resourceUri);
        required(platformAudience, "platformAudience");
        Objects.requireNonNull(clock, "clock");
        TenantIssuerAuthenticationManagerComponent manager = Objects.requireNonNull(
                tenantIssuerAuthenticationManager, "tenantIssuerAuthenticationManager");
        Objects.requireNonNull(verifiedPrincipalConverter, "verifiedPrincipalConverter");
        if (manager.isConfigured() && revocationReader == null) {
            throw new IllegalStateException("multi-issuer mode requires shared revocation reader");
        }
        if (!manager.isConfigured() && decoder == null) {
            throw new IllegalStateException("a configured legacy JWT decoder is required");
        }
        return new IdpJwtVerifier(decoder, resourceStates, clientStates, resourceServerId, resourceUri, platformAudience,
                clock, manager, revocationReader, verifiedPrincipalConverter, legacyIssuerCutoverPolicy, observations);
    }

    /** Verifies USER credentials, completing the enabled usage handoff before returning. */
    public AccessTokenVerification<IdentityPrincipal> verifyUser(String token) {
        return observations == null ? verifyUserInternal(token, ignored -> { })
                : observations.observe(capture -> verifyUserInternal(token, capture));
    }

    private AccessTokenVerification<IdentityPrincipal> verifyUserInternal(String token,
            java.util.function.Consumer<VerifiedJwtBO> capture) {
        if (multiIssuerConfigured()) {
            if (legacyIssuerCutoverPolicy != null
                    && legacyIssuerCutoverPolicy.isLegacyIssuerHint(token)) {
                return verifyLegacyUser(token);
            }
            return castVerification(
                    verifyMultiIssuer(token, PrincipalType.USER, capture), IdentityPrincipal.class);
        }
        return verifyLegacyUser(token);
    }

    private AccessTokenVerification<IdentityPrincipal> verifyLegacyUser(String token) {
        try {
            Jwt jwt = decode(token);
            return verifyLegacyUserJwt(jwt);
        } catch (LegacyStateUnavailableException unavailable) {
            return new AccessTokenVerification.Unavailable<>("LEGACY_ISSUER_STATE_UNAVAILABLE");
        } catch (ExpiredTokenException expired) {
            return new AccessTokenVerification.Expired<>();
        } catch (InvalidTokenException invalid) {
            return invalidOutcome(invalid.getMessage());
        } catch (JwtException | IllegalArgumentException | NullPointerException invalid) {
            return new AccessTokenVerification.Invalid<>("JWT_INVALID");
        }
    }

    /**
     * Verifies a SERVICE token and retains Resource/Client runtime checks.
     */
    public AccessTokenVerification<ServiceIdentityPrincipal> verifyService(String token) {
        return observations == null ? verifyServiceInternal(token, ignored -> { })
                : observations.observe(capture -> verifyServiceInternal(token, capture));
    }

    private AccessTokenVerification<ServiceIdentityPrincipal> verifyServiceInternal(String token,
            java.util.function.Consumer<VerifiedJwtBO> capture) {
        if (multiIssuerConfigured()) {
            if (legacyIssuerCutoverPolicy != null
                    && legacyIssuerCutoverPolicy.isLegacyIssuerHint(token)) {
                return verifyLegacyService(token);
            }
            return castVerification(
                    verifyMultiIssuer(token, PrincipalType.SERVICE, capture), ServiceIdentityPrincipal.class);
        }
        return verifyLegacyService(token);
    }

    private AccessTokenVerification<ServiceIdentityPrincipal> verifyLegacyService(String token) {
        try {
            if (resourceStates == null || clientStates == null
                    || resourceServerId == null || resourceUri == null) {
                return new AccessTokenVerification.Invalid<>(
                        "SERVICE_VERIFIER_NOT_CONFIGURED");
            }
            Jwt jwt = decode(token);
            return verifyLegacyServiceJwt(jwt);
        } catch (LegacyStateUnavailableException unavailable) {
            return new AccessTokenVerification.Unavailable<>("LEGACY_ISSUER_STATE_UNAVAILABLE");
        } catch (ExpiredTokenException expired) {
            return new AccessTokenVerification.Expired<>();
        } catch (InvalidTokenException invalid) {
            return invalidOutcome(invalid.getMessage());
        } catch (JwtException | IllegalArgumentException | NullPointerException invalid) {
            return new AccessTokenVerification.Invalid<>("JWT_INVALID");
        }
    }

    private AccessTokenVerification<IdentityPrincipal> verifyLegacyUserJwt(Jwt jwt) {
        commonHeaders(jwt);
        if (principalType(jwt) != PrincipalType.USER) {
            return new AccessTokenVerification.Invalid<>("JWT_PRINCIPAL_TYPE_INVALID");
        }
        enforceLegacyTenantCutover(jwt);
        Instant expiresAt = instant(jwt, "exp");
        if (!expiresAt.isAfter(clock.instant())) {
            return new AccessTokenVerification.Expired<>();
        }
        IdentityPrincipal principal = user(jwt, exactAudience(jwt, platformAudience));
        Set<String> scopes = jwt.hasClaim(IdpClaimNames.SCOPE)
                ? scopes(jwt) : Set.of();
        return new AccessTokenVerification.Valid<>(principal, scopes);
    }

    private AccessTokenVerification<ServiceIdentityPrincipal> verifyLegacyServiceJwt(Jwt jwt) {
        if (resourceStates == null || clientStates == null
                || resourceServerId == null || resourceUri == null) {
            return new AccessTokenVerification.Invalid<>("SERVICE_VERIFIER_NOT_CONFIGURED");
        }
        commonHeaders(jwt);
        if (principalType(jwt) != PrincipalType.SERVICE) {
            return new AccessTokenVerification.Invalid<>("JWT_PRINCIPAL_TYPE_INVALID");
        }
        enforceLegacyTenantCutover(jwt);
        Instant expiresAt = instant(jwt, "exp");
        if (!expiresAt.isAfter(clock.instant())) {
            return new AccessTokenVerification.Expired<>();
        }
        long resourceVersion = number(jwt, IdpClaimNames.RESOURCE_VERSION);
        verifyResource(resourceVersion);
        ServiceIdentityPrincipal principal = service(jwt, resourceVersion);
        return new AccessTokenVerification.Valid<>(principal, principal.scopes());
    }

    private AccessTokenVerification<IdpPrincipal> verifyLegacyAny(String token) {
        try {
            Jwt jwt = decode(token);
            PrincipalType type = principalType(jwt);
            if (type == PrincipalType.USER) {
                return copyVerification(verifyLegacyUserJwt(jwt));
            }
            return copyVerification(verifyLegacyServiceJwt(jwt));
        } catch (LegacyStateUnavailableException unavailable) {
            return new AccessTokenVerification.Unavailable<>("LEGACY_ISSUER_STATE_UNAVAILABLE");
        } catch (ExpiredTokenException expired) {
            return new AccessTokenVerification.Expired<>();
        } catch (InvalidTokenException invalid) {
            return invalidOutcome(invalid.getMessage());
        } catch (JwtException | IllegalArgumentException | NullPointerException invalid) {
            return new AccessTokenVerification.Invalid<>("JWT_INVALID");
        }
    }

    private void enforceLegacyTenantCutover(Jwt jwt) {
        if (legacyIssuerCutoverPolicy == null) {
            return;
        }
        String issuer = jwt.getIssuer() == null ? null : jwt.getIssuer().toExternalForm();
        if (!legacyIssuerCutoverPolicy.isLegacyIssuer(issuer)) {
            return;
        }
        LegacyIssuerCutoverPolicy.Decision decision = legacyIssuerCutoverPolicy.check(
                issuer, jwt.getClaimAsString("tid"));
        if (decision == LegacyIssuerCutoverPolicy.Decision.UNAVAILABLE) {
            throw new LegacyStateUnavailableException();
        }
        if (decision != LegacyIssuerCutoverPolicy.Decision.ALLOWED) {
            throw new InvalidTokenException("LEGACY_ISSUER_TENANT_RETIRED");
        }
    }

    /** Verifies a shared USER-or-SERVICE path with one local decode and one shared-state read. */
    public AccessTokenVerification<IdpPrincipal> verifyAny(String token, java.util.function.Consumer<VerifiedJwtBO> capture) {
        Objects.requireNonNull(capture, "capture");
        return verifyAnyInternal(token, capture);
    }

    public AccessTokenVerification<IdpPrincipal> verifyAny(String token) {
        return observations == null ? verifyAnyInternal(token, ignored -> { })
                : observations.observe(capture -> verifyAnyInternal(token, capture));
    }

    private AccessTokenVerification<IdpPrincipal> verifyAnyInternal(String token,
            java.util.function.Consumer<VerifiedJwtBO> capture) {
        if (multiIssuerConfigured()) {
            if (legacyIssuerCutoverPolicy != null
                    && legacyIssuerCutoverPolicy.isLegacyIssuerHint(token)) {
                return verifyLegacyAny(token);
            }
            return verifyMultiIssuer(token, null, capture);
        }
        Jwt jwt;
        try {
            jwt = decode(token);
            PrincipalType type = principalType(jwt);
            if (type == PrincipalType.USER) {
                return copyVerification(verifyLegacyUserJwt(jwt));
            }
            return copyVerification(verifyLegacyServiceJwt(jwt));
        } catch (LegacyStateUnavailableException unavailable) {
            return new AccessTokenVerification.Unavailable<>("LEGACY_ISSUER_STATE_UNAVAILABLE");
        } catch (ExpiredTokenException expired) {
            return new AccessTokenVerification.Expired<>();
        } catch (InvalidTokenException invalid) {
            return invalidOutcome(invalid.getMessage());
        } catch (JwtException | IllegalArgumentException | NullPointerException invalid) {
            return new AccessTokenVerification.Invalid<>("JWT_INVALID");
        }
    }

    private AccessTokenVerification<IdpPrincipal> verifyMultiIssuer(
            String token,
            PrincipalType expectedType) {
        return verifyMultiIssuer(token, expectedType, ignored -> { });
    }

    private AccessTokenVerification<IdpPrincipal> verifyMultiIssuer(String token, PrincipalType expectedType,
            java.util.function.Consumer<VerifiedJwtBO> capture) {
        try {
            if (tenantIssuerAuthenticationManager == null
                    || revocationReader == null || verifiedPrincipalConverter == null) {
                return new AccessTokenVerification.Unavailable<>(
                        "OAUTH_SECURITY_STATE_UNAVAILABLE");
            }
            VerifiedJwtBO verified = tenantIssuerAuthenticationManager.authenticate(token);
            Jwt jwt = verified.getJwt();
            commonHeaders(jwt);
            PrincipalType actualType = verified.getPrincipalType();
            if (expectedType != null && expectedType != actualType) {
                return new AccessTokenVerification.Invalid<>("JWT_PRINCIPAL_TYPE_INVALID");
            }
            Set<String> audiences = verifiedAudiences(jwt);
            if (!audiences.contains(resourceUri.toASCIIString())) {
                throw new InvalidTokenException("JWT_AUDIENCE_INVALID");
            }
            String clientId = claim(jwt, IdpClaimNames.CLIENT_ID);
            Set<String> verifiedScopes = verifiedPrincipalConverter.scopes(jwt);
            IdpPrincipal principal;
            if (actualType == PrincipalType.USER) {
                rejectNewUserClaims(jwt);
                if (clientId.isBlank()) {
                    throw new InvalidTokenException("JWT_CLIENT_ID_INVALID");
                }
                principal = verifiedPrincipalConverter.toTarget(jwt);
            } else if (actualType == PrincipalType.SERVICE) {
                if (!jwt.getSubject().equals(clientId)) {
                    throw new InvalidTokenException("SERVICE_SUBJECT_INVALID");
                }
                if (audiences.size() != 1) {
                    throw new InvalidTokenException("SERVICE_AUDIENCE_INVALID");
                }
                long serviceResourceVersion = number(jwt, IdpClaimNames.RESOURCE_VERSION);
                verifyResource(serviceResourceVersion);
                principal = verifiedPrincipalConverter.toServicePrincipal(jwt, resourceUri);
            } else {
                throw new InvalidTokenException("JWT_PRINCIPAL_TYPE_INVALID");
            }
            OAuthRevocationReader.Result result;
            try {
                result = revocationReader.check(verified);
            } catch (RuntimeException unavailable) {
                return new AccessTokenVerification.Unavailable<>(
                        "OAUTH_SECURITY_STATE_UNAVAILABLE");
            }
            if (result == null || result.status() == Status.UNAVAILABLE) {
                return new AccessTokenVerification.Unavailable<>(result == null
                        ? "OAUTH_SECURITY_STATE_UNAVAILABLE" : result.reasonCode());
            }
            if (result.status() == Status.REVOKED) {
                return new AccessTokenVerification.Invalid<>(result.reasonCode());
            }
            capture.accept(verified);
            return new AccessTokenVerification.Valid<>(principal, verifiedScopes);
        } catch (ExpiredTokenException expired) {
            return new AccessTokenVerification.Expired<>();
        } catch (TenantIssuerAuthenticationManagerComponent.InvalidIssuerException invalid) {
            return new AccessTokenVerification.Invalid<>(invalid.getMessage());
        } catch (InvalidTokenException invalid) {
            return invalidOutcome(invalid.getMessage());
        } catch (AuthenticationException | JwtException invalid) {
            if (expired(invalid)) {
                return new AccessTokenVerification.Expired<>();
            }
            return new AccessTokenVerification.Invalid<>("JWT_INVALID");
        } catch (IllegalArgumentException | NullPointerException invalid) {
            return new AccessTokenVerification.Invalid<>("JWT_INVALID");
        }
    }

    private void rejectNewUserClaims(Jwt jwt) {
        for (String forbidden : NEW_USER_FORBIDDEN_CLAIMS) {
            if (jwt.hasClaim(forbidden)) {
                throw new InvalidTokenException(
                        "JWT_FORBIDDEN_CLAIM_" + forbidden.toUpperCase());
            }
        }
    }

    private Set<String> verifiedAudiences(Jwt jwt) {
        List<String> values = jwt.getAudience();
        if (values == null || values.isEmpty()) {
            throw new InvalidTokenException("JWT_AUDIENCE_INVALID");
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String value : values) {
            String audience = text(value, "aud");
            URI normalized;
            try {
                normalized = URI.create(audience);
            } catch (IllegalArgumentException invalid) {
                throw new InvalidTokenException("JWT_AUDIENCE_INVALID", invalid);
            }
            if (!normalized.isAbsolute() || normalized.getHost() == null
                    || !"https".equalsIgnoreCase(normalized.getScheme())
                    || normalized.getUserInfo() != null || normalized.getFragment() != null
                    || !normalized.equals(normalized.normalize())
                    || "*".equals(audience) || !normalized.toASCIIString().equals(audience)
                    || !result.add(normalized.toASCIIString())) {
                throw new InvalidTokenException("JWT_AUDIENCE_INVALID");
            }
        }
        return Set.copyOf(result);
    }

    private boolean multiIssuerConfigured() {
        return tenantIssuerAuthenticationManager != null
                && tenantIssuerAuthenticationManager.isConfigured();
    }

    private static <T extends IdpPrincipal> AccessTokenVerification<T> castVerification(
            AccessTokenVerification<IdpPrincipal> result,
            Class<T> principalType) {
        if (result instanceof AccessTokenVerification.Valid<IdpPrincipal> valid
                && principalType.isInstance(valid.principal())) {
            return new AccessTokenVerification.Valid<>(
                    principalType.cast(valid.principal()), valid.scopes(), valid.proof());
        }
        if (result instanceof AccessTokenVerification.Expired<?>) {
            return new AccessTokenVerification.Expired<>();
        }
        if (result instanceof AccessTokenVerification.Unavailable<?> unavailable) {
            return new AccessTokenVerification.Unavailable<>(unavailable.reasonCode());
        }
        if (result instanceof AccessTokenVerification.Invalid<?> invalid) {
            return new AccessTokenVerification.Invalid<>(invalid.reasonCode());
        }
        return new AccessTokenVerification.Invalid<>("JWT_PRINCIPAL_TYPE_INVALID");
    }

    private static <T extends IdpPrincipal> AccessTokenVerification<IdpPrincipal> copyVerification(
            AccessTokenVerification<T> result) {
        if (result instanceof AccessTokenVerification.Valid<?> valid) {
            return new AccessTokenVerification.Valid<>((IdpPrincipal) valid.principal(), valid.scopes(), valid.proof());
        }
        if (result instanceof AccessTokenVerification.Expired<?>) {
            return new AccessTokenVerification.Expired<>();
        }
        if (result instanceof AccessTokenVerification.Unavailable<?> unavailable) {
            return new AccessTokenVerification.Unavailable<>(unavailable.reasonCode());
        }
        if (result instanceof AccessTokenVerification.Invalid<?> invalid) {
            return new AccessTokenVerification.Invalid<>(invalid.reasonCode());
        }
        return new AccessTokenVerification.Invalid<>("JWT_INVALID");
    }

    /**
     * Compatibility facade used by existing filters while they migrate to explicit paths.
     */
    public IdpPrincipal verify(String token) {
        if (multiIssuerConfigured()) {
            AccessTokenVerification<IdpPrincipal> result = verifyAny(token);
            if (result instanceof AccessTokenVerification.Valid<IdpPrincipal> valid) {
                return valid.principal();
            }
            throw verificationException(result);
        }
        AccessTokenVerification<IdpPrincipal> result = verifyLegacyAny(token);
        if (result instanceof AccessTokenVerification.Valid<IdpPrincipal> valid) {
            return valid.principal();
        }
        throw verificationException(result);
    }

    private Jwt decode(String token) {
        try {
            return decoder.decode(required(token, "token"));
        } catch (JwtException exception) {
            String message = exception.getMessage();
            if (message != null && message.toLowerCase().contains("expired")) {
                throw new ExpiredTokenException();
            }
            throw exception;
        }
    }

    private void commonHeaders(Jwt jwt) {
        if (!"RS256".equals(jwt.getHeaders().get("alg"))) {
            throw new InvalidTokenException("JWT_ALGORITHM_INVALID");
        }
        text(jwt.getHeaders().get("kid"), "kid");
        if (!"at+jwt".equals(jwt.getHeaders().get("typ"))) {
            throw new InvalidTokenException("JWT_TYPE_INVALID");
        }
        if (jwt.hasClaim(IdpClaimNames.TOKEN_USE)) {
            throw new InvalidTokenException("JWT_TOKEN_USE_INVALID");
        }
        instant(jwt, "iat");
        Instant nbf = instant(jwt, "nbf");
        Instant exp = instant(jwt, "exp");
        if (nbf.isBefore(instant(jwt, "iat")) || !exp.isAfter(nbf)) {
            throw new InvalidTokenException("JWT_TIME_INVALID");
        }
    }

    private Set<String> exactAudience(Jwt jwt, String expected) {
        List<String> values = jwt.getAudience();
        if (values == null || values.size() != 1 || !expected.equals(values.getFirst())) {
            throw new InvalidTokenException("JWT_AUDIENCE_INVALID");
        }
        return Set.of(values.getFirst());
    }

    private IdentityPrincipal user(Jwt jwt, Set<String> audience) {
        for (String forbidden : USER_FORBIDDEN_CLAIMS) {
            if (jwt.hasClaim(forbidden)) {
                throw new InvalidTokenException("JWT_FORBIDDEN_CLAIM_" + forbidden.toUpperCase());
            }
        }
        String acr = jwt.getClaimAsString("acr");
        if (acr == null || acr.isBlank()) {
            acr = "PASSWORD";
        }
        Instant authTime = jwt.hasClaim("auth_time")
                ? instant(jwt, "auth_time") : instant(jwt, "iat");
        AuthenticationContext authenticationContext = AuthenticationContext.of(acr, authTime);
        return new IdentityPrincipal(
                claim(jwt, "sub"),
                claim(jwt, IdpClaimNames.TENANT_ID),
                claim(jwt, "jti"),
                audience,
                instant(jwt, "iat"),
                instant(jwt, "exp"),
                authenticationContext);
    }

    private ServiceIdentityPrincipal service(Jwt jwt, long resourceVersion) {
        String subject = claim(jwt, "sub");
        String clientId = claim(jwt, IdpClaimNames.CLIENT_ID);
        if (!subject.equals(clientId)) {
            throw new InvalidTokenException("SERVICE_SUBJECT_INVALID");
        }
        String tenantId = optionalClaim(jwt, IdpClaimNames.TENANT_ID);
        if ("*".equals(tenantId)) {
            throw new InvalidTokenException("SERVICE_TENANT_INVALID");
        }
        ServiceTokenContext scopeContext = scopeContext(jwt);
        if (scopeContext == ServiceTokenContext.PLATFORM && tenantId != null) {
            throw new InvalidTokenException("SERVICE_CONTEXT_TENANT_CONFLICT");
        }
        if (scopeContext == ServiceTokenContext.TENANT && tenantId == null) {
            throw new InvalidTokenException("SERVICE_CONTEXT_TENANT_MISSING");
        }
        IdentityOAuthClientStateReader.IdentityOAuthClientState state;
        try {
            state = clientStates.read(clientId).orElseThrow(
                    () -> new InvalidTokenException("OAUTH_CLIENT_STATE_MISSING"));
        } catch (InvalidTokenException invalid) {
            throw invalid;
        } catch (RuntimeException unavailable) {
            throw new InvalidTokenException("OAUTH_CLIENT_STATE_UNAVAILABLE", unavailable);
        }
        if (!clientId.equals(state.clientId())) {
            throw new InvalidTokenException("OAUTH_CLIENT_ID_MISMATCH");
        }
        if (state.status() != OAuthClient.Status.ACTIVE) {
            throw new InvalidTokenException("OAUTH_CLIENT_NOT_ACTIVE");
        }
        if (state.clientType() != OAuthClient.ClientType.CONFIDENTIAL) {
            throw new InvalidTokenException("OAUTH_CLIENT_TYPE_INVALID");
        }
        return new ServiceIdentityPrincipal(
                subject, tenantId, clientId, claim(jwt, "jti"), resourceUri,
                resourceVersion, scopes(jwt), claim(jwt, IdpClaimNames.SOURCE_BIZ),
                claim(jwt, IdpClaimNames.SOURCE_APP), claim(jwt, IdpClaimNames.SOURCE_ENV),
                claim(jwt, IdpClaimNames.CREDENTIAL_ID), instant(jwt, "iat"),
                instant(jwt, "exp"), claim(jwt, IdpClaimNames.APP_ID),
                scopeContext);
    }

    private ServiceTokenContext scopeContext(Jwt jwt) {
        String value = claim(jwt, IdpClaimNames.SCOPE_CONTEXT);
        try {
            return ServiceTokenContext.valueOf(value);
        } catch (IllegalArgumentException invalid) {
            throw new InvalidTokenException("SERVICE_CONTEXT_INVALID", invalid);
        }
    }

    private void verifyResource(long tokenResourceVersion) {
        IdentityResourceServerState state;
        try {
            state = resourceStates.read(resourceServerId).orElseThrow(
                    () -> new InvalidTokenException("RESOURCE_STATE_MISSING"));
        } catch (InvalidTokenException invalid) {
            throw invalid;
        } catch (RuntimeException unavailable) {
            throw new InvalidTokenException("RESOURCE_STATE_UNAVAILABLE", unavailable);
        }
        if (!resourceServerId.equals(state.resourceServerId())) {
            throw new InvalidTokenException("RESOURCE_ID_MISMATCH");
        }
        if (state.status() != ResourceServerStatus.ACTIVE) {
            throw new InvalidTokenException("RESOURCE_NOT_ACTIVE");
        }
        if (!resourceUri.equals(state.resourceUri())) {
            throw new InvalidTokenException("RESOURCE_URI_MISMATCH");
        }
        if (state.version() != tokenResourceVersion) {
            throw new InvalidTokenException("RESOURCE_VERSION_STALE");
        }
    }

    private PrincipalType principalType(Jwt jwt) {
        try {
            return PrincipalType.valueOf(claim(jwt, IdpClaimNames.PRINCIPAL_TYPE));
        } catch (IllegalArgumentException invalid) {
            throw new InvalidTokenException("JWT_PRINCIPAL_TYPE_INVALID", invalid);
        }
    }

    private Set<String> scopes(Jwt jwt) {
        Object raw = jwt.getClaims().get(IdpClaimNames.SCOPE);
        Collection<?> values;
        if (raw instanceof String text) {
            values = List.of(text.trim().split("\\s+"));
        } else if (raw instanceof Collection<?> collection) {
            values = collection;
        } else {
            throw new InvalidTokenException("JWT_SCOPE_INVALID");
        }
        LinkedHashSet<String> scopes = new LinkedHashSet<>();
        for (Object value : values) {
            scopes.add(text(value, IdpClaimNames.SCOPE));
        }
        if (scopes.isEmpty() || scopes.size() != values.size()) {
            throw new InvalidTokenException("JWT_SCOPE_INVALID");
        }
        return Set.copyOf(scopes);
    }

    private String claim(Jwt jwt, String name) {
        return text(jwt.getClaims().get(name), name);
    }

    private String optionalClaim(Jwt jwt, String name) {
        Object value = jwt.getClaims().get(name);
        if (value == null) {
            return null;
        }
        return text(value, name);
    }

    private long number(Jwt jwt, String name) {
        Object value = jwt.getClaims().get(name);
        if (!(value instanceof Number number) || number.longValue() < 0L) {
            throw new InvalidTokenException("JWT_CLAIM_INVALID_" + name.toUpperCase());
        }
        return number.longValue();
    }

    private String text(Object value, String name) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw new InvalidTokenException("JWT_CLAIM_INVALID_" + name.toUpperCase());
        }
        return text.trim();
    }

    private Instant instant(Jwt jwt, String name) {
        Object value = jwt.getClaims().get(name);
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Number number && number.longValue() >= 0L) {
            return Instant.ofEpochSecond(number.longValue());
        }
        throw new InvalidTokenException("JWT_CLAIM_INVALID_" + name.toUpperCase());
    }

    private static RuntimeException verificationException(AccessTokenVerification<?> result) {
        if (result instanceof AccessTokenVerification.Unavailable<?> unavailable) {
            return new TokenVerificationUnavailableException(unavailable.reasonCode());
        }
        if (result instanceof AccessTokenVerification.Expired<?>) {
            return new InvalidTokenException("JWT_EXPIRED");
        }
        if (result instanceof AccessTokenVerification.Invalid<?> invalid) {
            return new InvalidTokenException(invalid.reasonCode());
        }
        return new InvalidTokenException("JWT_INVALID");
    }

    private static <T extends IdpPrincipal> AccessTokenVerification<T> invalidOutcome(
            String reasonCode) {
        String safeReason = reasonCode == null || reasonCode.isBlank()
                ? "JWT_INVALID" : reasonCode;
        if (safeReason.endsWith("_UNAVAILABLE")) {
            return new AccessTokenVerification.Unavailable<>("OAUTH_SECURITY_STATE_UNAVAILABLE");
        }
        return new AccessTokenVerification.Invalid<>(safeReason);
    }

    private static boolean expired(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("expired")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new InvalidTokenException(name + " is required");
        }
        return value.trim();
    }

    private static URI validResource(URI value) {
        Objects.requireNonNull(value, "resourceUri");
        if (!value.isAbsolute() || value.getFragment() != null
                || !value.equals(value.normalize())) {
            throw new IllegalArgumentException("resourceUri is invalid");
        }
        return value;
    }

    private static final class ExpiredTokenException extends RuntimeException {
    }

    private static final class LegacyStateUnavailableException extends RuntimeException {
    }

    public static final class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }

        public InvalidTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static final class TokenVerificationUnavailableException extends RuntimeException {

        public TokenVerificationUnavailableException(String reasonCode) {
            super(reasonCode);
        }
    }
}
