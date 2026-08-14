package top.egon.cola.platform.idp.starter.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import top.egon.cola.platform.idp.contract.AuthenticationContext;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.idp.contract.IdpClaimNames;
import top.egon.cola.platform.idp.contract.IdpPrincipal;
import top.egon.cola.platform.idp.contract.PrincipalType;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.idp.core.oauth.OAuthClient;
import top.egon.cola.platform.idp.core.resource.ResourceServerStatus;
import top.egon.cola.platform.idp.starter.state.IdentityOAuthClientStateReader;
import top.egon.cola.platform.idp.starter.state.IdentityResourceServerState;
import top.egon.cola.platform.idp.starter.state.IdentityResourceServerStateReader;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Validates IdP USER and SERVICE access tokens.
 * USER validation is completely stateless; SERVICE validation keeps its existing
 * Resource/Client runtime checks.
 */
public final class IdpJwtVerifier {

    private static final Set<String> USER_FORBIDDEN_CLAIMS = Set.of(
            "sid", "session_id", "client_id", "token_version",
            "resource_server_id", "resource", "resource_version", "nonce",
            "roles", "permissions", "capabilities", "dataScopes", "fieldPolicies",
            "authVersion", "sessionVersion", "contextVersion");

    private final JwtDecoder decoder;
    private final IdentityResourceServerStateReader resourceStates;
    private final IdentityOAuthClientStateReader clientStates;
    private final String resourceServerId;
    private final URI resourceUri;
    private final String platformAudience;
    private final Clock clock;

    public IdpJwtVerifier(
            JwtDecoder decoder,
            IdentityResourceServerStateReader resourceStates,
            IdentityOAuthClientStateReader clientStates,
            String resourceServerId,
            URI resourceUri,
            String platformAudience,
            Clock clock
    ) {
        this.decoder = Objects.requireNonNull(decoder, "decoder");
        this.resourceStates = Objects.requireNonNull(resourceStates, "resourceStates");
        this.clientStates = Objects.requireNonNull(clientStates, "clientStates");
        this.resourceServerId = required(resourceServerId, "resourceServerId");
        this.resourceUri = validResource(resourceUri);
        this.platformAudience = required(platformAudience, "platformAudience");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Creates a verifier for stateless USER tokens only.
     */
    public IdpJwtVerifier(
            JwtDecoder decoder,
            String platformAudience,
            Clock clock) {
        this.decoder = Objects.requireNonNull(decoder, "decoder");
        this.resourceStates = null;
        this.clientStates = null;
        this.resourceServerId = null;
        this.resourceUri = null;
        this.platformAudience = required(platformAudience, "platformAudience");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Verifies a USER token without any Redis or IdP-user-state lookup.
     */
    public AccessTokenVerification<IdentityPrincipal> verifyUser(String token) {
        try {
            Jwt jwt = decode(token);
            commonHeaders(jwt);
            if (principalType(jwt) != PrincipalType.USER) {
                return new AccessTokenVerification.Invalid<>("JWT_PRINCIPAL_TYPE_INVALID");
            }
            Instant expiresAt = instant(jwt, "exp");
            if (!expiresAt.isAfter(clock.instant())) {
                return new AccessTokenVerification.Expired<>();
            }
            return new AccessTokenVerification.Valid<>(user(jwt, exactAudience(jwt, platformAudience)));
        } catch (ExpiredTokenException expired) {
            return new AccessTokenVerification.Expired<>();
        } catch (InvalidTokenException invalid) {
            return new AccessTokenVerification.Invalid<>(invalid.getMessage());
        } catch (JwtException | IllegalArgumentException | NullPointerException invalid) {
            return new AccessTokenVerification.Invalid<>("JWT_INVALID");
        }
    }

    /**
     * Verifies a SERVICE token and retains Resource/Client runtime checks.
     */
    public AccessTokenVerification<ServiceIdentityPrincipal> verifyService(String token) {
        try {
            if (resourceStates == null || clientStates == null
                    || resourceServerId == null || resourceUri == null) {
                return new AccessTokenVerification.Invalid<>(
                        "SERVICE_VERIFIER_NOT_CONFIGURED");
            }
            Jwt jwt = decode(token);
            commonHeaders(jwt);
            if (principalType(jwt) != PrincipalType.SERVICE) {
                return new AccessTokenVerification.Invalid<>("JWT_PRINCIPAL_TYPE_INVALID");
            }
            Instant expiresAt = instant(jwt, "exp");
            if (!expiresAt.isAfter(clock.instant())) {
                return new AccessTokenVerification.Expired<>();
            }
            long resourceVersion = number(jwt, IdpClaimNames.RESOURCE_VERSION);
            verifyResource(resourceVersion);
            return new AccessTokenVerification.Valid<>(service(jwt, resourceVersion));
        } catch (ExpiredTokenException expired) {
            return new AccessTokenVerification.Expired<>();
        } catch (InvalidTokenException invalid) {
            return new AccessTokenVerification.Invalid<>(invalid.getMessage());
        } catch (JwtException | IllegalArgumentException | NullPointerException invalid) {
            return new AccessTokenVerification.Invalid<>("JWT_INVALID");
        }
    }

    /**
     * Compatibility facade used by existing filters while they migrate to explicit paths.
     */
    public IdpPrincipal verify(String token) {
        Jwt jwt;
        try {
            jwt = decode(token);
            PrincipalType type = principalType(jwt);
            if (type == PrincipalType.USER) {
                AccessTokenVerification<IdentityPrincipal> result = verifyUser(token);
                if (result instanceof AccessTokenVerification.Valid<IdentityPrincipal> valid) {
                    return valid.principal();
                }
                throw verificationException(result);
            }
            AccessTokenVerification<ServiceIdentityPrincipal> result = verifyService(token);
            if (result instanceof AccessTokenVerification.Valid<ServiceIdentityPrincipal> valid) {
                return valid.principal();
            }
            throw verificationException(result);
        } catch (InvalidTokenException invalid) {
            throw invalid;
        } catch (JwtException | IllegalArgumentException | NullPointerException invalid) {
            throw new InvalidTokenException("JWT_INVALID", invalid);
        }
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
        String tenantId = claim(jwt, IdpClaimNames.TENANT_ID);
        if ("*".equals(tenantId)) {
            throw new InvalidTokenException("SERVICE_TENANT_INVALID");
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
                instant(jwt, "exp"));
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
        if (!(value instanceof Instant instant)) {
            throw new InvalidTokenException("JWT_CLAIM_INVALID_" + name.toUpperCase());
        }
        return instant;
    }

    private static RuntimeException verificationException(AccessTokenVerification<?> result) {
        if (result instanceof AccessTokenVerification.Expired<?>) {
            return new InvalidTokenException("JWT_EXPIRED");
        }
        if (result instanceof AccessTokenVerification.Invalid<?> invalid) {
            return new InvalidTokenException(invalid.reasonCode());
        }
        return new InvalidTokenException("JWT_INVALID");
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

    public static final class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }

        public InvalidTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
