package top.egon.cola.platform.idp.starter.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import top.egon.cola.platform.idp.contract.IdentityUserState;
import top.egon.cola.platform.idp.starter.state.IdentityUserStateReader;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdpJwtVerifierTest {

    private static final Instant NOW = Instant.parse("2026-08-02T08:00:00Z");

    @Test
    void verifiesIdentityClaimsAndCurrentUserState() {
        IdpJwtVerifier verifier = verifier(jwt(7L), state(
                IdentityUserState.Status.ACTIVE, 7L));

        var principal = verifier.verify("access-token");

        assertThat(principal.subject()).isEqualTo("identity-1");
        assertThat(principal.tenantId()).isEqualTo("tenant-1");
        assertThat(principal.sessionId()).isEqualTo("session-1");
        assertThat(principal.clientId()).isEqualTo("gateway-admin");
        assertThat(principal.audience()).containsExactly("egon-api");
    }

    @Test
    void rejectsTokenVersionThatDoesNotMatchRedisState() {
        IdpJwtVerifier verifier = verifier(jwt(6L), state(
                IdentityUserState.Status.ACTIVE, 7L));

        assertThatThrownBy(() -> verifier.verify("access-token"))
                .isInstanceOf(IdpJwtVerifier.InvalidTokenException.class)
                .hasMessage("IDENTITY_TOKEN_VERSION_STALE");
    }

    @Test
    void rejectsDisabledIdentityAndWrongClient() {
        IdpJwtVerifier disabled = verifier(jwt(7L), state(
                IdentityUserState.Status.DISABLED, 7L));
        IdpJwtVerifier wrongClient = new IdpJwtVerifier(
                token -> jwt(7L), subject -> Optional.of(state(
                        IdentityUserState.Status.ACTIVE, 7L)),
                Set.of("egon-api"), Set.of("rbac3-admin"));

        assertThatThrownBy(() -> disabled.verify("access-token"))
                .hasMessage("IDENTITY_NOT_ACTIVE");
        assertThatThrownBy(() -> wrongClient.verify("access-token"))
                .hasMessage("JWT_CLIENT_INVALID");
    }

    @Test
    void rejectsMissingTenantAndRefreshTokenShape() {
        Jwt missingTenant = Jwt.withTokenValue("access-token")
                .header("alg", "RS256")
                .header("kid", "key-1")
                .issuer("https://idp.local")
                .subject("identity-1")
                .audience(List.of("egon-api"))
                .issuedAt(NOW)
                .notBefore(NOW)
                .expiresAt(NOW.plusSeconds(300))
                .claim("sid", "session-1")
                .claim("client_id", "gateway-admin")
                .claim("jti", "token-1")
                .claim("token_version", 7L)
                .build();
        Jwt refresh = Jwt.withTokenValue("refresh-token")
                .headers(headers -> headers.putAll(jwt(7L).getHeaders()))
                .claims(claims -> claims.putAll(jwt(7L).getClaims()))
                .claim("token_use", "refresh")
                .build();

        assertThatThrownBy(() -> verifier(missingTenant, state(
                IdentityUserState.Status.ACTIVE, 7L)).verify("access-token"))
                .hasMessage("JWT_CLAIM_INVALID_TID");
        assertThatThrownBy(() -> verifier(refresh, state(
                IdentityUserState.Status.ACTIVE, 7L)).verify("refresh-token"))
                .hasMessage("JWT_TOKEN_USE_INVALID");
    }

    @Test
    void rejectsAccessTokenWithoutNotBeforeClaim() {
        Jwt missingNotBefore = Jwt.withTokenValue("access-token")
                .header("alg", "RS256")
                .header("kid", "key-1")
                .issuer("https://idp.local")
                .subject("identity-1")
                .audience(List.of("egon-api"))
                .issuedAt(NOW)
                .expiresAt(NOW.plusSeconds(300))
                .claim("tid", "tenant-1")
                .claim("sid", "session-1")
                .claim("client_id", "gateway-admin")
                .claim("jti", "token-1")
                .claim("token_version", 7L)
                .build();

        assertThatThrownBy(() -> verifier(missingNotBefore, state(
                IdentityUserState.Status.ACTIVE, 7L)).verify("access-token"))
                .hasMessage("JWT_CLAIM_INVALID_NBF");
    }

    private IdpJwtVerifier verifier(Jwt jwt, IdentityUserState state) {
        JwtDecoder decoder = token -> jwt;
        IdentityUserStateReader reader = subject -> Optional.of(state);
        return new IdpJwtVerifier(
                decoder, reader, Set.of("egon-api"), Set.of("gateway-admin"));
    }

    private Jwt jwt(long tokenVersion) {
        return Jwt.withTokenValue("access-token")
                .header("alg", "RS256")
                .header("kid", "key-1")
                .issuer("https://idp.local")
                .subject("identity-1")
                .audience(List.of("egon-api"))
                .issuedAt(NOW)
                .notBefore(NOW)
                .expiresAt(NOW.plusSeconds(300))
                .claim("tid", "tenant-1")
                .claim("sid", "session-1")
                .claim("client_id", "gateway-admin")
                .claim("jti", "token-1")
                .claim("token_version", tokenVersion)
                .build();
    }

    private IdentityUserState state(
            IdentityUserState.Status status,
            long tokenVersion
    ) {
        return new IdentityUserState(
                "identity-1", status, tokenVersion, NOW);
    }
}
