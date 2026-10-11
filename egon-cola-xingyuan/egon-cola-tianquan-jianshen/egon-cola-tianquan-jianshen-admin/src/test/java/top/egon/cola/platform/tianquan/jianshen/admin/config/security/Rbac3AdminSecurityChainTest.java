package top.egon.cola.platform.tianquan.jianshen.admin.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpBearerAuthenticationFilter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.UserAccessTokenVerifier;
import top.egon.cola.platform.tianquan.shoubing.starter.security.ServiceAccessTokenVerifier;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpEndpointAuthenticationPolicy;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3BearerAuthenticationFilter;
import top.egon.cola.platform.tianquan.jianshen.starter.cache.SingleFlightSnapshotLoader;

/** Tests the two real host chains without booting an application or infrastructure. */
class Rbac3AdminSecurityChainTest {
    @Test
    void scopeAndIdentityErrorFamiliesOnTheActualProfileAboutAndActivationPaths() throws Exception {
        var context = new AnnotationConfigWebApplicationContext(); context.setServletContext(new MockServletContext());
        context.register(Rbac3AdminSecurityConfiguration.class, FixtureConfiguration.class); context.refresh();
        try {
            var now = java.time.Instant.parse("2026-10-12T00:00:00Z");
            var identity = new top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal("identity-1", "1001", "901",
                    java.util.Set.of("resource"), now, now.plusSeconds(300), top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext.password());
            var snapshot = new top.egon.cola.platform.tianquan.jianshen.contract.authorization.SystemAuthorizationSnapshot("1001", "identity-1", "2001",
                    "app", 1, 2, java.util.List.of(), null, null, java.util.Set.of("system:about:read", "system:role-activation:read", "system:role-activation:use"),
                    java.util.Set.of(), java.util.Map.of(), java.util.Map.of(), "sha256:scope-snapshot", now, now.plusSeconds(300));
            var users = context.getBean(UserAccessTokenVerifier.class);
            org.mockito.Mockito.when(users.verify("scopeless")).thenReturn(new top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification.Valid<>(identity, java.util.Set.of()));
            org.mockito.Mockito.when(context.getBean(SingleFlightSnapshotLoader.class).load(identity)).thenReturn(snapshot);
            var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
            for (String path : java.util.List.of("/api/v1/auth/about", "/api/tianquan-jianshen/v1/auth/role-activation-candidates", "/api/tianquan-jianshen/v1/auth/role-activations")) {
                var response = mvc.perform(get(path).header("Authorization", "Bearer scopeless")).andReturn().getResponse();
                assertThat(response.getStatus()).isEqualTo(403); assertThat(response.getHeader("Cache-Control")).contains("no-store");
                assertThat(response.getContentAsString()).contains("AUTHORIZATION_DENIED");
            }
            var response = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/tianquan-jianshen/v1/auth/role-activations")
                    .header("Authorization", "Bearer scopeless").contentType("application/json").content("{}")) .andReturn().getResponse();
            assertThat(response.getStatus()).isEqualTo(403);
            assertThat(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()).isNull();
        } finally { context.close(); }
    }

    @Test
    void missingBearerUsesOwn401FamilyWithoutRedirectOrBusiness() throws Exception {
        var context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(Rbac3AdminSecurityConfiguration.class, FixtureConfiguration.class);
        context.refresh();
        try {
            var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
            for (String path : java.util.List.of("/internal/test", "/api/tianquan-jianshen/v1/test")) {
                var response = mvc.perform(get(path)).andReturn().getResponse();
                assertThat(response.getStatus()).isEqualTo(401);
                assertThat(response.getHeader("Cache-Control")).contains("no-store");
                assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
                assertThat(response.getHeader("Location")).isNull();
                assertThat(response.getContentAsString()).contains("Tianquan-Shoubing authentication failed");
            }
        } finally { context.close(); }
    }

    @Test
    void fieldDeniedAndUnavailableKeepTheirOwnFamiliesAndNoStore() {
        var handler = new top.egon.cola.platform.tianquan.jianshen.starter.web.Rbac3AuthorizationExceptionHandler();
        var denied = handler.fieldDenied(new org.springframework.security.authorization.AuthorizationDeniedException("FIELD_ACCESS_DENIED"));
        assertThat(denied.getStatusCode().value()).isEqualTo(403);
        assertThat(denied.getHeaders().getFirst("Cache-Control")).isEqualTo("no-store");
        assertThat(denied.getBody()).containsEntry("code", "FIELD_ACCESS_DENIED");
        var identity = new top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal("user-1", "1001", "901", java.util.Set.of("resource"),
                java.time.Instant.now(), java.time.Instant.now().plusSeconds(300), top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext.password());
        var unavailable = handler.unavailable(new top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService.RuntimeUnavailableException(
                "AUTHORIZATION_RUNTIME_UNAVAILABLE", identity));
        assertThat(unavailable.getStatusCode().value()).isEqualTo(503);
        assertThat(unavailable.getHeaders().getFirst("Cache-Control")).isEqualTo("no-store");
        assertThat(unavailable.getBody()).containsEntry("code", "AUTHORIZATION_RUNTIME_UNAVAILABLE");
    }

    @Test
    void rbacBridgePreservesVerifiedProofAndVerifiedOAuthScopes() throws Exception {
        var now = java.time.Instant.now();
        var identity = new top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal("identity-1", "1001", "901",
                java.util.Set.of("resource"), now, now.plusSeconds(300), top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext.password());
        var snapshot = new top.egon.cola.platform.tianquan.jianshen.contract.authorization.SystemAuthorizationSnapshot("1001", "identity-1", "2001",
                "app", 1, 2, java.util.List.of(), null, null, java.util.Set.of(), java.util.Set.of(), java.util.Map.of(), java.util.Map.of(),
                "sha256:snapshot", now, now.plusSeconds(300));
        var loader = org.mockito.Mockito.mock(SingleFlightSnapshotLoader.class);
        org.mockito.Mockito.when(loader.load(identity)).thenReturn(snapshot);
        var proof = new top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditProofBO(java.net.URI.create("https://idp.example/tenants/1001"),
                java.net.URI.create("https://api.example"), "a".repeat(64), "b".repeat(64), "c".repeat(64));
        var original = new top.egon.cola.platform.tianquan.shoubing.starter.security.IdpAuthenticationToken(identity,
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_read")));
        original.setDetails(proof);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(original);
        try {
            new Rbac3BearerAuthenticationFilter(loader, new ObjectMapper()).doFilter(new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/test"),
                    new org.springframework.mock.web.MockHttpServletResponse(), (request, response) -> {
                        var current = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                        assertThat(current.getDetails()).isSameAs(proof);
                        assertThat(current.getAuthorities()).anySatisfy(authority -> assertThat(authority.getAuthority()).isEqualTo("SCOPE_read"));
                    });
            assertThat(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()).isNull();
        } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    }

    @Test
    void invalidTokenAndUnavailableSnapshotFailBeforeBusinessWithOwnStatus() throws Exception {
        var context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(Rbac3AdminSecurityConfiguration.class, FixtureConfiguration.class);
        context.refresh();
        try {
            var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
            var users = context.getBean(UserAccessTokenVerifier.class);
            var loader = context.getBean(SingleFlightSnapshotLoader.class);
            org.mockito.Mockito.when(users.verify("invalid")).thenReturn(
                    new top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification.Invalid<>("TOKEN_INVALID"));
            var invalid = mvc.perform(get("/api/tianquan-jianshen/v1/test").header("Authorization", "Bearer invalid")).andReturn().getResponse();
            assertThat(invalid.getStatus()).isEqualTo(401);
            assertThat(invalid.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
            var now = java.time.Instant.now();
            var identity = new top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal("identity-1", "1001", "901",
                    java.util.Set.of("resource"), now, now.plusSeconds(300),
                    top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext.password());
            org.mockito.Mockito.when(users.verify("valid")).thenReturn(
                    new top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification.Valid<>(identity));
            org.mockito.Mockito.doThrow(
                    new top.egon.cola.platform.tianquan.jianshen.starter.client.Rbac3AuthorizationClient.AuthorizationDeniedException("SNAPSHOT_DENIED")).when(loader).load(identity);
            var denied = mvc.perform(get("/api/tianquan-jianshen/v1/test").header("Authorization", "Bearer valid")).andReturn().getResponse();
            assertThat(denied.getStatus()).isEqualTo(403);
            assertThat(denied.getContentAsString()).contains("SNAPSHOT_DENIED");
            assertThat(denied.getHeader("Cache-Control")).contains("no-store");
            org.mockito.Mockito.doThrow(
                    new top.egon.cola.platform.tianquan.jianshen.starter.client.Rbac3AuthorizationClient.AuthorizationUnavailableException("SNAPSHOT_UNAVAILABLE")).when(loader).load(identity);
            var unavailable = mvc.perform(get("/api/tianquan-jianshen/v1/test").header("Authorization", "Bearer valid")).andReturn().getResponse();
            assertThat(unavailable.getStatus()).isEqualTo(503);
            assertThat(unavailable.getContentAsString()).contains("SNAPSHOT_UNAVAILABLE");
            assertThat(unavailable.getHeader("Cache-Control")).contains("no-store");
            assertThat(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()).isNull();
        } finally { context.close(); }
    }

    @Test
    void realChainRequiresAuditBeforeRenderingAndSkipsPublicHealth() throws Exception {
        var context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(Rbac3AdminSecurityConfiguration.class, FixtureConfiguration.class);
        context.refresh();
        try {
            var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
            var observer = context.getBean(top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent.class);
            org.mockito.Mockito.when(observer.enabled()).thenReturn(true);
            org.mockito.Mockito.when(observer.authenticationFailure()).thenReturn(false);
            var response = mvc.perform(get("/api/tianquan-jianshen/v1/test")).andReturn().getResponse();
            assertThat(response.getStatus()).isEqualTo(503);
            assertThat(response.getContentAsString()).contains("OAUTH_AUDIT_DELIVERY_UNAVAILABLE");
            assertThat(response.getHeader("Cache-Control")).contains("no-store");
            org.mockito.Mockito.verify(observer, org.mockito.Mockito.times(1)).authenticationFailure();
            mvc.perform(get("/actuator/health/readiness"));
            org.mockito.Mockito.verify(observer, org.mockito.Mockito.times(1)).authenticationFailure();
            assertThat(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(org.slf4j.MDC.get("traceId")).isNull();
        } finally { context.close(); }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebSecurity
    @EnableWebMvc
    static class FixtureConfiguration {
        @Bean("idpObservedVerificationComponent") top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent observer() {
            return org.mockito.Mockito.mock(top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent.class);
        }
        @Bean("idpSecurityObservationFilter") top.egon.cola.platform.tianquan.shoubing.starter.security.IdpSecurityObservationFilter observation(
                top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent observer, Rbac3AdminSecurityConfiguration config) {
            return new top.egon.cola.platform.tianquan.shoubing.starter.security.IdpSecurityObservationFilter(observer, config.rbac3EndpointAuthenticationPolicy());
        }
        @Bean("jacksonObjectMapper") ObjectMapper mapper() { return new ObjectMapper(); }
        @Bean("idpBearerAuthenticationFilter") IdpBearerAuthenticationFilter idp(Rbac3AdminSecurityConfiguration config, ObjectMapper mapper, UserAccessTokenVerifier users) {
            return new IdpBearerAuthenticationFilter(users,
                    org.mockito.Mockito.mock(ServiceAccessTokenVerifier.class), config.rbac3EndpointAuthenticationPolicy(), mapper);
        }
        @Bean("userAccessTokenVerifier") UserAccessTokenVerifier users() { return org.mockito.Mockito.mock(UserAccessTokenVerifier.class); }
        @Bean("singleFlightSnapshotLoader") SingleFlightSnapshotLoader loader() { return org.mockito.Mockito.mock(SingleFlightSnapshotLoader.class); }
        @Bean("rbac3BearerAuthenticationFilter") Rbac3BearerAuthenticationFilter rbac(ObjectMapper mapper, SingleFlightSnapshotLoader loader) {
            return new Rbac3BearerAuthenticationFilter(loader, mapper);
        }
    }
}
