package top.egon.cola.platform.rbac3.starter.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import top.egon.cola.platform.rbac3.contract.auth.Rbac3TokenClaims;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Rbac3JwtVerifierTest {

    @Test
    void requiresAllRbac3ClaimsAndNeverReadsAuthorizationCollections() {
        Instant now = Instant.parse("2026-07-30T08:00:00Z");
        Jwt jwt = new Jwt(
                "opaque-reference", now.minusSeconds(10), now.plusSeconds(300),
                Map.of("alg", "RS256", "kid", "kid-1"),
                Map.ofEntries(
                        Map.entry("iss", "rbac3"),
                        Map.entry("aud", List.of("business")),
                        Map.entry("sub", "20001"),
                        Map.entry("tid", "10001"),
                        Map.entry("sid", "30001"),
                        Map.entry("av", 1L),
                        Map.entry("sv", 2L),
                        Map.entry("pv", 3L),
                        Map.entry("jti", "jti-1"),
                        Map.entry("iat", now.minusSeconds(10)),
                        Map.entry("nbf", now.minusSeconds(10)),
                        Map.entry("exp", now.plusSeconds(300)),
                        Map.entry("roles", List.of("must-be-ignored"))));
        Rbac3JwtVerifier verifier = new Rbac3JwtVerifier(token -> jwt);

        Rbac3TokenClaims claims = verifier.verify("opaque-reference");

        assertEquals("10001", claims.tid());
        assertEquals("kid-1", claims.kid());
    }

    @Test
    void rejectsTokensWithMissingKidOrUnexpectedAlgorithm() {
        JwtDecoder decoder = token -> new Jwt(
                token,
                Instant.EPOCH,
                Instant.EPOCH.plusSeconds(60),
                Map.of("alg", "HS256"),
                Map.of("sub", "20001"));

        assertThrows(Rbac3JwtVerifier.InvalidTokenException.class,
                () -> new Rbac3JwtVerifier(decoder).verify("bad-token"));
    }
}
