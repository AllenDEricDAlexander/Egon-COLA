package top.egon.cola.platform.rbac3.starter.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import top.egon.cola.platform.rbac3.contract.auth.Rbac3TokenClaims;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Verifies a signed JWT and maps only the frozen RBAC3 identity/version claims.
 */
public final class Rbac3JwtVerifier {

    private final JwtDecoder decoder;

    public Rbac3JwtVerifier(JwtDecoder decoder) {
        this.decoder = Objects.requireNonNull(decoder, "decoder");
    }

    public Rbac3TokenClaims verify(String token) {
        try {
            Jwt jwt = decoder.decode(required(token, "token"));
            if (!"RS256".equals(jwt.getHeaders().get("alg"))) {
                throw new InvalidTokenException("JWT_ALGORITHM_INVALID");
            }
            String kid = text(jwt.getHeaders().get("kid"), "kid");
            return new Rbac3TokenClaims(
                    claim(jwt, "iss"), audience(jwt), claim(jwt, "sub"),
                    claim(jwt, "tid"), claim(jwt, "sid"), number(jwt, "av"),
                    number(jwt, "sv"), number(jwt, "pv"), claim(jwt, "jti"),
                    instant(jwt.getIssuedAt(), "iat"),
                    instant(jwt.getNotBefore(), "nbf"),
                    instant(jwt.getExpiresAt(), "exp"), kid);
        } catch (InvalidTokenException exception) {
            throw exception;
        } catch (JwtException | IllegalArgumentException | NullPointerException exception) {
            throw new InvalidTokenException("JWT_INVALID", exception);
        }
    }

    private List<String> audience(Jwt jwt) {
        List<String> audience = jwt.getAudience();
        if (audience == null || audience.isEmpty()) {
            throw new InvalidTokenException("JWT_AUDIENCE_MISSING");
        }
        return audience;
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

    private Instant instant(Instant value, String name) {
        if (value == null) {
            throw new InvalidTokenException("JWT_CLAIM_INVALID_" + name.toUpperCase());
        }
        return value;
    }

    private String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new InvalidTokenException(name + " is required");
        }
        return value.trim();
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
