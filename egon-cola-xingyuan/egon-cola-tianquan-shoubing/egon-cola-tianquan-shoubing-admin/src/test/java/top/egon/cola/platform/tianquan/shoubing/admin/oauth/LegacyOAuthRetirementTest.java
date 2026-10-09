package top.egon.cola.platform.tianquan.shoubing.admin.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.platform.tianquan.shoubing.admin.token.config.TokenConfig;

import static org.junit.jupiter.api.Assertions.*;

class LegacyOAuthRetirementTest {

    private static final String ADMIN = "top.egon.cola.platform.tianquan.shoubing.admin.";
    private static final List<String> ROUTES = List.of(
            "GET /.well-known/oauth-authorization-server", "GET /oauth2/jwks",
            "GET /oauth2/login/csrf", "POST /oauth2/login", "POST /oauth2/token",
            "POST /oauth2/revoke", "POST /oauth2/logout", "POST /oauth2/step-up",
            "GET /oauth2/userinfo", "POST /internal/v1/oauth2/refresh-token/validate");

    @Test
    void oldTenRoutes410WithoutParsingCredentialsOrIssuingCookies() throws Exception {
        Filter filter = retirementFilter();
        for (String route : ROUTES) {
            String[] parts = route.split(" ", 2);
            MockHttpServletRequest request = new MockHttpServletRequest(parts[0], parts[1]) {
                @Override
                public String getParameter(String name) {
                    throw new AssertionError("Retirement must not read credentials");
                }
                @Override
                public java.util.Map<String, String[]> getParameterMap() {
                    throw new AssertionError("Retirement must not parse request bodies");
                }
            };
            request.addHeader("Authorization", "Basic invalid-secret-material");
            request.addHeader("Cookie", "egon_user_rt_local=retired-secret");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, (nextRequest, nextResponse) ->
                    fail("Retired route reached the application: " + route));
            assertEquals(410, response.getStatus(), route);
            assertEquals("no-store", response.getHeader("Cache-Control"));
            assertEquals("legacy_endpoint_retired", new ObjectMapper()
                    .readTree(response.getContentAsString()).path("error").asText());
            assertNull(response.getHeader("Set-Cookie"));
            assertNull(response.getHeader("Location"));
            assertFalse(response.getContentAsString().contains("secret"));
            assertFalse(response.getContentAsString().contains("access_token"));
        }
    }

    @Test
    void contextPathIsHandledButIssuerPathsAndOtherMethodsContinue() throws Exception {
        Filter filter = retirementFilter();
        MockHttpServletRequest retired = new MockHttpServletRequest("POST", "/idp/oauth2/token");
        retired.setContextPath("/idp");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(retired, response, (request, next) -> fail("Context route was not retired"));
        assertEquals(410, response.getStatus());
        for (String route : List.of("POST /tenants/1001/oauth2/token", "GET /platform/oauth2/jwks",
                "GET /oauth2/token", "POST /oauth2/unknown", "GET /api/v1/tianquan-shoubing/me")) {
            String[] parts = route.split(" ", 2);
            boolean[] continued = {false};
            MockHttpServletResponse unaffected = new MockHttpServletResponse();
            filter.doFilter(new MockHttpServletRequest(parts[0], parts[1]), unaffected,
                    (request, next) -> continued[0] = true);
            assertTrue(continued[0], route);
            assertEquals(200, unaffected.getStatus());
        }
    }

    @Test
    void legacyControllersHaveNoRegisteredProtocolMappings() {
        for (String name : List.of("OAuthTokenController", "OAuthMetadataController", "OAuthLoginController",
                "InternalRefreshTokenController", "OAuthStepUpController", "OAuthUserInfoController")) {
            Class<?> type = optionalClass(ADMIN + "oauth.controller." + name);
            assertTrue(type == null || !type.isAnnotationPresent(RestController.class), name);
        }
    }

    @Test
    void noLegacySignerOrRefreshLifecycleBeanIsDeclared() {
        List<String> methods = java.util.Arrays.stream(TokenConfig.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(org.springframework.context.annotation.Bean.class))
                .map(java.lang.reflect.Method::getName).toList();
        for (String name : List.of("signingKeyRuntime", "rs256TokenService", "tokenFacade")) {
            assertFalse(methods.contains(name), name);
        }
    }

    @Test
    void readinessUsesTheRegisteredPlatformSigningKeyAndRestoresItsPreviousIssuerContext() throws Exception {
        verifyPlatformReadiness(false);
    }

    @Test
    void failedPlatformKeyReadinessRemainsClosedAndRestoresItsPreviousIssuerContext() throws Exception {
        verifyPlatformReadiness(true);
    }

    @SuppressWarnings("unchecked")
    private void verifyPlatformReadiness(boolean unavailable) throws Exception {
        var settings = org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
                .builder().multipleIssuersAllowed(true).build();
        var realm = top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO.builder()
                .issuer(java.net.URI.create("https://idp.example/platform")).realmKey("platform")
                .realmType(top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum.PLATFORM)
                .storagePartitionId(90001L).trustVersion(0L).build();
        var realms = org.mockito.Mockito.mock(
                top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver.class);
        org.mockito.Mockito.when(realms.platformRealm()).thenReturn(realm);
        var repository = org.mockito.Mockito.mock(
                top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository.class);
        org.mockito.Mockito.when(repository.findByIssuer(realm)).thenReturn(java.util.Optional.of(
                top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthRealmBO.builder()
                        .status("ACTIVE").activeKid("active-platform-key").build()));
        org.springframework.beans.factory.ObjectProvider<top.egon.cola.component.tianshu.service.lifecycle.DdcRuntimeCoordinator>
                coordinators = org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class);
        com.nimbusds.jose.jwk.source.JWKSource<com.nimbusds.jose.proc.SecurityContext> keys = (selector, context) -> {
            assertEquals(realm.getIssuer().toString(), org.springframework.security.oauth2.server.authorization.context
                    .AuthorizationServerContextHolder.getContext().getIssuer());
            assertTrue(selector.getMatcher().isPrivateOnly());
            assertTrue(selector.getMatcher().getKeyIDs().contains("active-platform-key"));
            if (unavailable) {
                throw new com.nimbusds.jose.KeySourceException("NOT_READY");
            }
            return List.of(org.mockito.Mockito.mock(com.nimbusds.jose.jwk.JWK.class));
        };
        var prior = new org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext() {
            @Override
            public String getIssuer() { return "https://previous.example/tenants/1002"; }
            @Override
            public org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
                    getAuthorizationServerSettings() { return settings; }
        };
        org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder.setContext(prior);
        try {
            var method = top.egon.cola.platform.tianquan.shoubing.admin.support.runtime.IdpPlatformConfiguration.class
                    .getDeclaredMethod("idpRuntimeReadiness", org.springframework.beans.factory.ObjectProvider.class,
                            top.egon.cola.component.outbox.store.OutboxStore.class, org.redisson.api.RedissonClient.class,
                            top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver.class,
                            top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository.class,
                            com.nimbusds.jose.jwk.source.JWKSource.class,
                            org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings.class);
            method.setAccessible(true);
            var readiness = (top.egon.cola.platform.tianquan.shoubing.admin.support.runtime.IdpRuntimeReadiness)
                    method.invoke(new top.egon.cola.platform.tianquan.shoubing.admin.support.runtime.IdpPlatformConfiguration(),
                            coordinators, org.mockito.Mockito.mock(top.egon.cola.component.outbox.store.OutboxStore.class),
                            org.mockito.Mockito.mock(org.redisson.api.RedissonClient.class), realms, repository, keys, settings);
            assertEquals(!unavailable, readiness.status().oauthRuntimeReady());
            assertSame(prior, org.springframework.security.oauth2.server.authorization.context
                    .AuthorizationServerContextHolder.getContext());
            org.mockito.Mockito.verify(repository).findByIssuer(realm);
        } finally {
            org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder.setContext(null);
        }
    }

    private static Filter retirementFilter() throws Exception {
        Class<?> type = optionalClass(ADMIN + "oauth.controller.filter.LegacyOAuthRetirementFilter");
        assertNotNull(type, "The exact legacy retirement filter is not implemented");
        return (Filter) type.getConstructor(ObjectMapper.class).newInstance(new ObjectMapper());
    }

    private static Class<?> optionalClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException missing) {
            return null;
        }
    }
}
