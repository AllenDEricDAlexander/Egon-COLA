package top.egon.cola.platform.idp.starter.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.idp.contract.IdentityUserState;
import top.egon.cola.platform.idp.contract.IdpClaimNames;
import top.egon.cola.platform.idp.starter.state.IdentityUserStateReader;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Verifies IdP access-token claims and the current global user state.
 */
public final class IdpJwtVerifier {

    private final JwtDecoder decoder;
    private final IdentityUserStateReader stateReader;
    private final Set<String> audiences;
    private final Set<String> clientIds;

    public IdpJwtVerifier(
            JwtDecoder decoder,
            IdentityUserStateReader stateReader,
            Set<String> audiences,
            Set<String> clientIds
    ) {
        this.decoder = Objects.requireNonNull(decoder, "decoder");
        this.stateReader = Objects.requireNonNull(stateReader, "stateReader");
        this.audiences = requiredValues(audiences, "audiences");
        this.clientIds = requiredValues(clientIds, "clientIds");
    }

    public IdentityPrincipal verify(String token) {
        try {
            Jwt jwt = decoder.decode(required(token, "token"));
            if (!"RS256".equals(jwt.getHeaders().get("alg"))) {
                throw new InvalidTokenException("JWT_ALGORITHM_INVALID");
            }
            text(jwt.getHeaders().get("kid"), "kid");
            if (jwt.hasClaim("token_use")) {
                throw new InvalidTokenException("JWT_TOKEN_USE_INVALID");
            }
            Set<String> tokenAudience = audience(jwt);
            if (tokenAudience.stream().noneMatch(audiences::contains)) {
                throw new InvalidTokenException("JWT_AUDIENCE_INVALID");
            }
            String clientId = claim(jwt, IdpClaimNames.CLIENT_ID);
            if (!clientIds.contains(clientId)) {
                throw new InvalidTokenException("JWT_CLIENT_INVALID");
            }
            String subject = claim(jwt, "sub");
            long tokenVersion = number(jwt, IdpClaimNames.TOKEN_VERSION);
            instant(jwt.getNotBefore(), "nbf");
            IdentityUserState state = stateReader.read(subject)
                    .orElseThrow(() -> new InvalidTokenException(
                            "IDENTITY_STATE_MISSING"));
            if (state.status() != IdentityUserState.Status.ACTIVE) {
                throw new InvalidTokenException("IDENTITY_NOT_ACTIVE");
            }
            if (state.tokenVersion() != tokenVersion) {
                throw new InvalidTokenException(
                        "IDENTITY_TOKEN_VERSION_STALE");
            }
            return new IdentityPrincipal(
                    subject,
                    claim(jwt, IdpClaimNames.TENANT_ID),
                    claim(jwt, IdpClaimNames.SESSION_ID),
                    clientId,
                    claim(jwt, "jti"),
                    tokenVersion,
                    tokenAudience,
                    instant(jwt.getIssuedAt(), "iat"),
                    instant(jwt.getExpiresAt(), "exp"));
        } catch (InvalidTokenException exception) {
            throw exception;
        } catch (JwtException | IllegalArgumentException
                 | NullPointerException exception) {
            throw new InvalidTokenException("JWT_INVALID", exception);
        } catch (RuntimeException exception) {
            throw new InvalidTokenException(
                    "IDENTITY_STATE_UNAVAILABLE", exception);
        }
    }

    private Set<String> audience(Jwt jwt) {
        List<String> values = jwt.getAudience();
        if (values == null || values.isEmpty()
                || values.stream().anyMatch(
                        value -> value == null || value.isBlank())) {
            throw new InvalidTokenException("JWT_AUDIENCE_MISSING");
        }
        return Set.copyOf(values);
    }

    private String claim(Jwt jwt, String name) {
        return text(jwt.getClaims().get(name), name);
    }

    private long number(Jwt jwt, String name) {
        Object value = jwt.getClaims().get(name);
        if (!(value instanceof Number number) || number.longValue() < 0L) {
            throw new InvalidTokenException(
                    "JWT_CLAIM_INVALID_" + name.toUpperCase());
        }
        return number.longValue();
    }

    private String text(Object value, String name) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw new InvalidTokenException(
                    "JWT_CLAIM_INVALID_" + name.toUpperCase());
        }
        return text.trim();
    }

    private Instant instant(Instant value, String name) {
        if (value == null) {
            throw new InvalidTokenException(
                    "JWT_CLAIM_INVALID_" + name.toUpperCase());
        }
        return value;
    }

    private String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new InvalidTokenException(name + " is required");
        }
        return value.trim();
    }

    private Set<String> requiredValues(Set<String> values, String name) {
        Objects.requireNonNull(values, name);
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(
                        name + " must contain only non-blank values");
            }
            normalized.add(value.trim());
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return Set.copyOf(normalized);
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
