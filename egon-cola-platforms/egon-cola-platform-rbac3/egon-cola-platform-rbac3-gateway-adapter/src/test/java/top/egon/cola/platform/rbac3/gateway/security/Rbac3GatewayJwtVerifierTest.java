package top.egon.cola.platform.rbac3.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import top.egon.cola.platform.rbac3.core.runtime.Rbac3RuntimeKeyFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Rbac3GatewayJwtVerifierTest {

    private static final Instant NOW = Instant.parse("2026-07-30T12:00:00Z");

    @Test
    void verifiesRs256AndFallsBackToAnUnexpiredKeyOnlyWhenRedisFails()
            throws Exception {
        RSAKey key = key("kid-1");
        AtomicReference<Object> ring = new AtomicReference<>(ring(key));
        Rbac3GatewayJwtVerifier verifier = verifier(ring);

        assertThat(verifier.verify(token(key, "kid-1", "issuer", "orders",
                NOW.minusSeconds(1), NOW.plusSeconds(300))).sub()).isEqualTo("9");

        ring.set(new IllegalStateException("redis unavailable"));
        assertThat(verifier.verify(token(key, "kid-1", "issuer", "orders",
                NOW.minusSeconds(1), NOW.plusSeconds(300))).sid()).isEqualTo("99");
    }

    @Test
    void rejectsUnknownRemovedAndIncorrectlySignedKeys() throws Exception {
        RSAKey trusted = key("kid-1");
        RSAKey replacement = key("kid-2");
        AtomicReference<Object> ring = new AtomicReference<>(ring(trusted));
        Rbac3GatewayJwtVerifier verifier = verifier(ring);
        verifier.verify(token(trusted, "kid-1", "issuer", "orders",
                NOW.minusSeconds(1), NOW.plusSeconds(300)));

        ring.set(ring(replacement));
        assertThatThrownBy(() -> verifier.verify(token(
                trusted, "kid-1", "issuer", "orders",
                NOW.minusSeconds(1), NOW.plusSeconds(300))))
                .isInstanceOf(Rbac3GatewayJwtVerifier.InvalidTokenException.class)
                .hasMessage("RBAC3_JWT_KID_UNKNOWN");

        assertThatThrownBy(() -> verifier.verify(token(
                trusted, "kid-2", "issuer", "orders",
                NOW.minusSeconds(1), NOW.plusSeconds(300))))
                .isInstanceOf(Rbac3GatewayJwtVerifier.InvalidTokenException.class)
                .hasMessage("RBAC3_JWT_SIGNATURE_INVALID");
    }

    @Test
    void rejectsIssuerAudienceAndTimeClaimMismatches() throws Exception {
        RSAKey key = key("kid-1");
        Rbac3GatewayJwtVerifier verifier = verifier(new AtomicReference<>(ring(key)));

        assertThatThrownBy(() -> verifier.verify(token(
                key, "kid-1", "other", "orders",
                NOW.minusSeconds(1), NOW.plusSeconds(300))))
                .hasMessage("RBAC3_JWT_ISSUER_INVALID");
        assertThatThrownBy(() -> verifier.verify(token(
                key, "kid-1", "issuer", "other",
                NOW.minusSeconds(1), NOW.plusSeconds(300))))
                .hasMessage("RBAC3_JWT_AUDIENCE_INVALID");
        assertThatThrownBy(() -> verifier.verify(token(
                key, "kid-1", "issuer", "orders",
                NOW.plusSeconds(300), NOW.plusSeconds(600))))
                .hasMessage("RBAC3_JWT_TIME_INVALID");
    }

    private Rbac3GatewayJwtVerifier verifier(AtomicReference<Object> ring) {
        RedissonClient redisson = mock(RedissonClient.class);
        @SuppressWarnings("unchecked")
        RBucket<Object> bucket = mock(RBucket.class);
        when(redisson.<Object>getBucket("rbac3:{7}:key-ring")).thenReturn(bucket);
        when(bucket.get()).thenAnswer(invocation -> {
            Object value = ring.get();
            if (value instanceof RuntimeException exception) {
                throw exception;
            }
            return value;
        });
        return new Rbac3GatewayJwtVerifier(
                redisson, new ObjectMapper(), new Rbac3RuntimeKeyFactory(),
                Clock.fixed(NOW, ZoneOffset.UTC), "issuer", "orders",
                Duration.ofMinutes(2), Duration.ofMinutes(5));
    }

    private RSAKey key(String kid) throws Exception {
        return new RSAKeyGenerator(2048)
                .keyID(kid)
                .algorithm(JWSAlgorithm.RS256)
                .generate();
    }

    private Map<String, Object> ring(RSAKey key) {
        return Map.of("keys", List.of(key.toPublicJWK().toJSONObject()));
    }

    private String token(
            RSAKey signingKey,
            String kid,
            String issuer,
            String audience,
            Instant notBefore,
            Instant expiresAt
    ) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .subject("9")
                .claim("tid", "7")
                .claim("sid", "99")
                .claim("av", 3L)
                .claim("sv", 4L)
                .claim("pv", 5L)
                .jwtID("jti-1")
                .issueTime(Date.from(NOW.minusSeconds(1)))
                .notBeforeTime(Date.from(notBefore))
                .expirationTime(Date.from(expiresAt))
                .build();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(kid).build(),
                claims);
        jwt.sign(new RSASSASigner(signingKey.toPrivateKey()));
        return jwt.serialize();
    }
}
