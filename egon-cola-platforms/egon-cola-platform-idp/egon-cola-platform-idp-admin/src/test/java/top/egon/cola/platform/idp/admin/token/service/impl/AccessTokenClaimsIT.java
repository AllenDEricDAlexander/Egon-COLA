package top.egon.cola.platform.idp.admin.token.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import top.egon.cola.platform.idp.contract.AuthenticationContext;
import top.egon.cola.platform.idp.contract.PrincipalType;
import top.egon.cola.platform.idp.core.token.AccessTokenClaims;
import top.egon.cola.platform.idp.core.token.RefreshTokenClaims;
import top.egon.cola.platform.idp.core.token.TokenException;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccessTokenClaimsIT {

    private static final Instant NOW = Instant.now()
            .minusSeconds(5)
            .truncatedTo(ChronoUnit.SECONDS);
    private static final String ISSUER = "https://idp.example.test";
    private static final String AUDIENCE = "platform";

    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;
    private Rs256TokenService tokens;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        publicKey = (RSAPublicKey) pair.getPublic();
        privateKey = (RSAPrivateKey) pair.getPrivate();
        tokens = new Rs256TokenService(
                publicKey,
                privateKey,
                "idp-key-1",
                ISSUER
        );
    }

    @Test
    void rs256AccessJwtHasExactIdentityClaimsAndNoAuthorizationFacts() {
        String token = tokens.signAccess(new AccessTokenClaims(
                PrincipalType.USER,
                "alice-sub",
                "tenant-a",
                "jti-1",
                AUDIENCE,
                NOW,
                NOW,
                NOW.plusSeconds(300),
                AuthenticationContext.password()
        ));
        Jwt jwt = tokens.jwtDecoder().decode(token);

        assertEquals("RS256", jwt.getHeaders().get("alg"));
        assertEquals("idp-key-1", jwt.getHeaders().get("kid"));
        assertEquals("at+jwt", jwt.getHeaders().get("typ"));
        assertEquals(ISSUER, jwt.getIssuer().toString());
        assertEquals("alice-sub", jwt.getSubject());
        assertEquals("tenant-a", jwt.getClaimAsString("tid"));
        assertEquals("USER", jwt.getClaimAsString("principal_type"));
        assertEquals("jti-1", jwt.getId());
        assertEquals(List.of(AUDIENCE), jwt.getAudience());
        assertEquals("PASSWORD", jwt.getClaimAsString("acr"));
        assertEquals(NOW, (Instant) jwt.getClaim("auth_time"));
        assertEquals(NOW.plusSeconds(300), jwt.getExpiresAt());
        assertNull(jwt.getClaim("sid"));
        assertNull(jwt.getClaim("client_id"));
        assertNull(jwt.getClaim("token_version"));
        assertNull(jwt.getClaim("resource_version"));
        assertNull(jwt.getClaim("roles"));
        assertNull(jwt.getClaim("permissions"));
        assertNull(jwt.getClaim("data_scopes"));
        assertNull(jwt.getClaim("field_policies"));
        assertNull(jwt.getClaim("scope"));
    }

    @Test
    void refreshJwtRoundTripsAndRejectsTampering() {
        RefreshTokenClaims expected = new RefreshTokenClaims(
                "alice-sub",
                "tenant-a",
                "refresh-jti-1",
                NOW,
                NOW,
                NOW.plusSeconds(3_600)
        );
        String token = tokens.signRefresh(expected);
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        Jwt jwt = decoder.decode(token);

        assertEquals("refresh", jwt.getClaimAsString("token_use"));
        assertEquals("refresh-jti-1", jwt.getId());
        assertEquals("alice-sub", jwt.getSubject());
        assertEquals("tenant-a", jwt.getClaimAsString("tid"));
        assertNull(jwt.getAudience());
        assertNull(jwt.getClaim("resource"));
        assertNull(jwt.getClaim("resource_version"));
        assertEquals(expected, tokens.verifyRefresh(token));
        assertThrows(TokenException.class, () -> tokens.verifyRefresh(
                token.substring(0, token.length() - 2) + "xx"
        ));
    }

    @Test
    void publishedJwkSetContainsNoPrivateKeyMaterial() {
        Map<String, Object> jwkSet = tokens.jwkSet();
        Map<?, ?> publicJwk = (Map<?, ?>) ((List<?>) jwkSet.get("keys"))
                .getFirst();

        assertEquals(1, ((List<?>) jwkSet.get("keys")).size());
        assertFalse(publicJwk.containsKey("d"));
        assertFalse(publicJwk.containsKey("p"));
        assertFalse(publicJwk.containsKey("q"));
    }

    @Test
    void issuerTrailingSlashIsNormalizedForTokensAndMetadata() {
        Rs256TokenService normalized = new Rs256TokenService(
                publicKey,
                privateKey,
                "idp-key-1",
                ISSUER + '/'
        );

        assertEquals(ISSUER, normalized.issuer());
    }
}
