package top.egon.cola.platform.idp.core.token;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.contract.AuthenticationContext;
import top.egon.cola.platform.idp.contract.PrincipalType;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class StatelessUserTokenContractTest {

    private static final Instant ISSUED_AT =
            Instant.parse("2026-08-14T00:00:00Z");

    @Test
    void userAccessClaimsContainOnlyStatelessIdentityAndFiveMinuteLifetime() {
        AccessTokenClaims claims = new AccessTokenClaims(
                PrincipalType.USER,
                "alice-sub",
                "tenant-a",
                "access-jti",
                "platform",
                ISSUED_AT,
                ISSUED_AT,
                ISSUED_AT.plusSeconds(300),
                AuthenticationContext.password()
        );

        assertEquals(Duration.ofMinutes(5),
                Duration.between(claims.issuedAt(), claims.expiresAt()));
        assertEquals("platform", claims.audience());
        assertEquals("PASSWORD", claims.authenticationContext().acr());

        Map<String, Object> encoded = claims.asMap();
        assertFalse(encoded.containsKey("sid"));
        assertFalse(encoded.containsKey("client_id"));
        assertFalse(encoded.containsKey("token_version"));
        assertFalse(encoded.containsKey("resource_version"));
        assertFalse(encoded.containsKey("nonce"));
        assertFalse(encoded.containsKey("roles"));
        assertFalse(encoded.containsKey("permissions"));
    }

    @Test
    void refreshClaimsContainNoSessionFamilyOrRotationState() {
        RefreshTokenClaims claims = new RefreshTokenClaims(
                "alice-sub",
                "tenant-a",
                "refresh-jti",
                ISSUED_AT,
                ISSUED_AT,
                ISSUED_AT.plus(Duration.ofDays(30))
        );

        assertEquals(ISSUED_AT, claims.notBefore());
        assertEquals(ISSUED_AT.plus(Duration.ofDays(30)),
                claims.expiresAt());
    }
}
