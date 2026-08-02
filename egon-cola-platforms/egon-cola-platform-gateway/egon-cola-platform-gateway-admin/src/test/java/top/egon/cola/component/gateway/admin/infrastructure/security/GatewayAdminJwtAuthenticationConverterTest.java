package top.egon.cola.component.gateway.admin.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayAdminJwtAuthenticationConverterTest {

    @Test
    void ignoresAuthorizationClaimsFromIdentityToken() {
        Jwt jwt = new Jwt(
                "token",
                Instant.parse("2026-07-25T00:00:00Z"),
                Instant.parse("2026-07-25T01:00:00Z"),
                Map.of("alg", "none"),
                Map.of(
                        "sub", "admin-42",
                        "capabilities", List.of(
                                "gateway:read",
                                "gateway:groups:write"
                        ),
                        "roles", List.of("GATEWAY_OPERATOR")
                )
        );

        var authentication =
                new GatewayAdminJwtAuthenticationConverter().convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("admin-42");
        assertThat(authentication.getAuthorities()).isEmpty();
    }
}
