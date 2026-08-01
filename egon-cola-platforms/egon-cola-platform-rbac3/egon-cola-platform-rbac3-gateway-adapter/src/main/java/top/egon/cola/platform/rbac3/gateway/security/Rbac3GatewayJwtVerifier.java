package top.egon.cola.platform.rbac3.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.platform.rbac3.contract.auth.Rbac3TokenClaims;
import top.egon.cola.platform.rbac3.core.runtime.Rbac3RuntimeKeyFactory;

import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RS256 verifier backed by the Redis public Key Ring with a bounded-time in-memory LKG.
 */
public final class Rbac3GatewayJwtVerifier
        implements Rbac3JwtSessionAuthenticationProvider.TokenVerifier {

    private final RedissonClient redisson;
    private final ObjectMapper objectMapper;
    private final Rbac3RuntimeKeyFactory keyFactory;
    private final Clock clock;
    private final String issuer;
    private final String audience;
    private final Duration clockSkew;
    private final Duration lkgTtl;
    private final Map<KeyId, CachedKey> cache = new ConcurrentHashMap<>();

    public Rbac3GatewayJwtVerifier(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            ObjectMapper objectMapper,
            Rbac3RuntimeKeyFactory keyFactory,
            Clock clock,
            String issuer,
            String audience,
            Duration clockSkew,
            Duration lkgTtl
    ) {
        this.redisson = Objects.requireNonNull(redisson, "redisson");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.keyFactory = Objects.requireNonNull(keyFactory, "keyFactory");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.issuer = required(issuer, "issuer");
        this.audience = required(audience, "audience");
        this.clockSkew = positive(clockSkew, "clockSkew");
        this.lkgTtl = positive(lkgTtl, "lkgTtl");
    }

    @Override
    public Rbac3TokenClaims verify(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(required(token, "token"));
            if (!JWSAlgorithm.RS256.equals(jwt.getHeader().getAlgorithm())) {
                throw invalid("RBAC3_JWT_ALGORITHM_INVALID");
            }
            String kid = required(jwt.getHeader().getKeyID(), "kid");
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            String tenantId = required(claims.getStringClaim("tid"), "tid");
            RSAKey publicKey = key(tenantId, kid);
            if (!jwt.verify(new RSASSAVerifier(publicKey.toRSAPublicKey()))) {
                throw invalid("RBAC3_JWT_SIGNATURE_INVALID");
            }
            validateStandardClaims(claims);
            return new Rbac3TokenClaims(
                    claims.getIssuer(), claims.getAudience(), claims.getSubject(),
                    tenantId, required(claims.getStringClaim("sid"), "sid"),
                    nonNegative(claims.getLongClaim("av"), "av"),
                    nonNegative(claims.getLongClaim("sv"), "sv"),
                    nonNegative(claims.getLongClaim("pv"), "pv"),
                    required(claims.getJWTID(), "jti"),
                    instant(claims.getIssueTime(), "iat"),
                    instant(claims.getNotBeforeTime(), "nbf"),
                    instant(claims.getExpirationTime(), "exp"), kid);
        } catch (InvalidTokenException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidTokenException("RBAC3_JWT_INVALID", exception);
        }
    }

    private void validateStandardClaims(JWTClaimsSet claims) throws ParseException {
        if (!issuer.equals(claims.getIssuer())) {
            throw invalid("RBAC3_JWT_ISSUER_INVALID");
        }
        if (claims.getAudience() == null || !claims.getAudience().contains(audience)) {
            throw invalid("RBAC3_JWT_AUDIENCE_INVALID");
        }
        Instant now = clock.instant();
        Instant issuedAt = instant(claims.getIssueTime(), "iat");
        Instant notBefore = instant(claims.getNotBeforeTime(), "nbf");
        Instant expiresAt = instant(claims.getExpirationTime(), "exp");
        if (issuedAt.isAfter(now.plus(clockSkew))
                || notBefore.isAfter(now.plus(clockSkew))
                || !expiresAt.isAfter(now.minus(clockSkew))) {
            throw invalid("RBAC3_JWT_TIME_INVALID");
        }
    }

    private RSAKey key(String tenantId, String kid) {
        KeyId id = new KeyId(tenantId, kid);
        try {
            Object value = redisson.getBucket(keyFactory.keyRing(tenantId)).get();
            Map<?, ?> ring = objectMapper.convertValue(value, Map.class);
            Object values = ring.get("keys");
            if (!(values instanceof Collection<?> keys)) {
                throw new IllegalArgumentException("public Key Ring is missing keys");
            }
            Instant expiresAt = clock.instant().plus(lkgTtl);
            Map<KeyId, CachedKey> refreshed = new HashMap<>();
            for (Object entry : keys) {
                @SuppressWarnings("unchecked")
                Map<String, Object> jwk = objectMapper.convertValue(entry, Map.class);
                RSAKey key = RSAKey.parse(jwk).toPublicJWK();
                if (JWSAlgorithm.RS256.equals(key.getAlgorithm())
                        && key.getKeyID() != null) {
                    refreshed.put(new KeyId(tenantId, key.getKeyID()),
                            new CachedKey(key, expiresAt));
                }
            }
            cache.keySet().removeIf(keyId -> tenantId.equals(keyId.tenantId()));
            cache.putAll(refreshed);
            CachedKey selected = refreshed.get(id);
            if (selected == null) {
                throw invalid("RBAC3_JWT_KID_UNKNOWN");
            }
            return selected.key();
        } catch (InvalidTokenException exception) {
            throw exception;
        } catch (RuntimeException | java.text.ParseException exception) {
            CachedKey selected = cache.get(id);
            if (selected != null && selected.expiresAt().isAfter(clock.instant())) {
                return selected.key();
            }
            throw new InvalidTokenException("RBAC3_KEY_RING_UNAVAILABLE", exception);
        }
    }

    private Instant instant(java.util.Date value, String field) {
        if (value == null) {
            throw invalid("RBAC3_JWT_CLAIM_INVALID_" + field.toUpperCase());
        }
        return value.toInstant();
    }

    private long nonNegative(Long value, String field) {
        if (value == null || value < 0) {
            throw invalid("RBAC3_JWT_CLAIM_INVALID_" + field.toUpperCase());
        }
        return value;
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw invalid("RBAC3_JWT_CLAIM_INVALID_" + field.toUpperCase());
        }
        return value.trim();
    }

    private Duration positive(Duration value, String field) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    private InvalidTokenException invalid(String code) {
        return new InvalidTokenException(code, null);
    }

    private record KeyId(String tenantId, String kid) {
    }

    private record CachedKey(RSAKey key, Instant expiresAt) {
    }

    public static final class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
