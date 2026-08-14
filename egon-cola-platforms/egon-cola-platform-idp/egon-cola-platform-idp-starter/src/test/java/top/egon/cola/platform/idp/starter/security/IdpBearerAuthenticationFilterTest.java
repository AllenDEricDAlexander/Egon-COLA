package top.egon.cola.platform.idp.starter.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.idp.core.oauth.OAuthClient;
import top.egon.cola.platform.idp.core.resource.ResourceServerStatus;
import top.egon.cola.platform.idp.starter.state.IdentityOAuthClientStateReader;
import top.egon.cola.platform.idp.starter.state.IdentityResourceServerState;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IdpBearerAuthenticationFilterTest {

    private static final URI RESOURCE_URI = URI.create(
            "https://api.example/prod/permission/rbac3"
    );

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void establishesIdentityPrincipalForOneBearerCredential() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        var chain = new CapturingFilterChain();

        filter().doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.principal).isInstanceOf(IdentityPrincipal.class);
        assertThat(((IdentityPrincipal) chain.principal).subject())
                .isEqualTo("identity-1");
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();
    }

    @Test
    void authenticatesInternalPathsWithTheSameResourceToken() throws Exception {
        var request = new MockHttpServletRequest("GET", "/internal/v1/state");
        request.addHeader("Authorization", "Bearer access-token");
        var chain = new CapturingFilterChain();

        filter().doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.principal).isInstanceOf(IdentityPrincipal.class);
    }

    @Test
    void rejectsMultipleAuthorizationHeadersAsUnauthorized() throws Exception {
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

    @Test
    void servicePathUsesOnlyServiceVerifierWithoutUserFallback() throws Exception {
        UserAccessTokenVerifier userVerifier = mock(UserAccessTokenVerifier.class);
        ServiceAccessTokenVerifier serviceVerifier = mock(ServiceAccessTokenVerifier.class);
        ServiceIdentityPrincipal principal = new ServiceIdentityPrincipal(
                "service-1",
                "tenant-1",
                "service-1",
                "service-token-1",
                RESOURCE_URI,
                1L,
                Set.of("service:read"),
                "platform",
                "rbac3",
                "local",
                "credential-1",
                Instant.parse("2026-08-02T08:00:00Z"),
                Instant.parse("2026-08-02T08:01:00Z"));
        when(serviceVerifier.verify("service-token"))
                .thenReturn(new AccessTokenVerification.Valid<>(principal));
        var filter = new IdpBearerAuthenticationFilter(
                userVerifier,
                serviceVerifier,
                new IdpEndpointAuthenticationPolicy(
                        List.of(),
                        List.of("/internal/**")),
                new ObjectMapper());
        var request = new MockHttpServletRequest("GET", "/internal/v1/state");
        request.addHeader("Authorization", "Bearer service-token");
        var chain = new CapturingFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.principal).isEqualTo(principal);
        verify(serviceVerifier).verify("service-token");
        verifyNoInteractions(userVerifier);
    }

    private IdpBearerAuthenticationFilter filter() {
        Instant now = Instant.parse("2026-08-02T08:00:00Z");
        return new IdpBearerAuthenticationFilter(
                new IdpJwtVerifier(
                        token -> jwt(now),
                        resourceId -> Optional.of(
                                new IdentityResourceServerState(
                                        "resource-rbac3-prod",
                                        RESOURCE_URI,
                                        "permission",
                                        "rbac3",
                                        "prod",
                                        ResourceServerStatus.ACTIVE,
                                        12L)),
                        clientId -> Optional.of(
                                new IdentityOAuthClientStateReader
                                        .IdentityOAuthClientState(
                                        "rbac3-service",
                                        OAuthClient.ClientType.CONFIDENTIAL,
                                        OAuthClient.Status.ACTIVE,
                                        "resource-idp-prod",
                                        3L)),
                        "resource-rbac3-prod",
                        RESOURCE_URI,
                        "egon-platform",
                        java.time.Clock.fixed(now, java.time.ZoneOffset.UTC)),
                new ObjectMapper());
    }

    private org.springframework.security.oauth2.jwt.Jwt jwt(Instant now) {
        return org.springframework.security.oauth2.jwt.Jwt
                .withTokenValue("token")
                .header("alg", "RS256")
                .header("kid", "key-1")
                .header("typ", "at+jwt")
                .issuer("https://idp.local")
                .subject("identity-1")
                .audience(List.of("egon-platform"))
                .issuedAt(now)
                .notBefore(now)
                .expiresAt(now.plusSeconds(300))
                .claim("principal_type", "USER")
                .claim("tid", "tenant-1")
                .claim("jti", "token-1")
                .claim("acr", "PASSWORD")
                .claim("auth_time", now)
                .build();
    }

    private static final class CapturingFilterChain extends MockFilterChain {
        private Object principal;

        @Override
        public void doFilter(
                jakarta.servlet.ServletRequest request,
                jakarta.servlet.ServletResponse response
        ) {
            var authentication = SecurityContextHolder.getContext()
                    .getAuthentication();
            principal = authentication == null
                    ? null : authentication.getPrincipal();
        }
    }
}
