package top.egon.cola.component.ddc.admin.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DdcAdminJwtAuthenticationConverterTest {

    @Test
    void ignoresAuthorizationClaimsFromIdentityToken() {
        Jwt jwt = new Jwt(
                "token",
                Instant.parse("2026-07-26T00:00:00Z"),
                Instant.parse("2026-07-26T01:00:00Z"),
                Map.of("alg", "none"),
                Map.of(
                        "sub", "admin-42",
                        "capabilities", List.of(
                                "DDC_READ",
                                "DDC_WRITE"
                        ),
                        "roles", "DDC_OPERATOR, AUDITOR"
                )
        );

        var authentication =
                new DdcAdminJwtAuthenticationConverter().convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("admin-42");
        assertThat(authentication.getAuthorities()).isEmpty();
    }
}
