package top.egon.cola.platform.idp.starter.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class IdpBearerAuthenticationFilterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void establishesIdentityPrincipalForOneBearerCredential() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        var response = new MockHttpServletResponse();
        var chain = new CapturingFilterChain();

        filter().doFilter(request, response, chain);

        assertThat(chain.principal).isInstanceOf(IdentityPrincipal.class);
        assertThat(((IdentityPrincipal) chain.principal).subject())
                .isEqualTo("identity-1");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void rejectsMultipleAuthorizationHeaders() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer first");
        request.addHeader("Authorization", "Bearer second");
        var response = new MockHttpServletResponse();

        filter().doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString())
                .contains("AUTHORIZATION_HEADER_INVALID")
                .doesNotContain("first")
                .doesNotContain("second");
    }

    private IdpBearerAuthenticationFilter filter() {
        return new IdpBearerAuthenticationFilter(
                new IdpJwtVerifier(token -> jwt(),
                        subject -> java.util.Optional.of(state()),
                        Set.of("egon-api"), Set.of("gateway-admin")),
                new ObjectMapper());
    }

    private org.springframework.security.oauth2.jwt.Jwt jwt() {
        Instant now = Instant.parse("2026-08-02T08:00:00Z");
        return org.springframework.security.oauth2.jwt.Jwt.withTokenValue("token")
                .header("alg", "RS256").header("kid", "key-1")
                .issuer("https://idp.local").subject("identity-1")
                .audience(java.util.List.of("egon-api"))
                .issuedAt(now).expiresAt(now.plusSeconds(300))
                .claim("tid", "tenant-1").claim("sid", "session-1")
                .claim("client_id", "gateway-admin")
                .claim("jti", "token-1").claim("token_version", 7L)
                .build();
    }

    private top.egon.cola.platform.idp.contract.IdentityUserState state() {
        return new top.egon.cola.platform.idp.contract.IdentityUserState(
                "identity-1",
                top.egon.cola.platform.idp.contract.IdentityUserState.Status.ACTIVE,
                7L,
                Instant.parse("2026-08-02T08:00:00Z"));
    }

    private static final class CapturingFilterChain extends MockFilterChain {
        private Object principal;

        @Override
        public void doFilter(
                jakarta.servlet.ServletRequest request,
                jakarta.servlet.ServletResponse response
        ) {
            principal = SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
        }
    }
}
