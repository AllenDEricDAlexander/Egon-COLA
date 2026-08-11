package top.egon.cola.platform.idp.admin.oauth.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;
import top.egon.cola.platform.idp.admin.support.rbac3.HttpTenantMembershipAdapter;
import top.egon.cola.platform.idp.admin.support.rbac3.HttpUserResourceAccessAuthorizationAdapter;
import top.egon.cola.platform.idp.admin.oauth.controller.OAuthAuthorizationController;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientEntity;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientRedirectUriEntity;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRedirectUriRepository;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRepository;
import top.egon.cola.platform.idp.admin.oauth.repo.JpaOAuthClientStore;
import top.egon.cola.platform.idp.admin.oauth.repo.RedisAuthorizationCodeStore;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityClientResourceGrantEntity;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityResourceServerEntity;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityClientResourceGrantRepository;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityResourceServerRepository;
import top.egon.cola.platform.idp.admin.resource.repo.JpaResourceServerStore;
import top.egon.cola.platform.idp.admin.support.security.IdpSsoPrincipal;
import top.egon.cola.platform.idp.core.oauth.AuthorizationCode;
import top.egon.cola.platform.idp.core.oauth.AuthorizationFacade;
import top.egon.cola.platform.idp.core.oauth.OAuthClient;
import top.egon.cola.platform.idp.core.port.TenantMembershipPort;
import top.egon.cola.platform.idp.core.port.UserResourceAccessAuthorizationPort;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OAuthAuthorizationFlowIT {

    private static final String DIGEST =
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    @Test
    void redisStoreUsesDigestKeyTtlAndAtomicGetDelete() throws Exception {
        RedissonClient redisson = mock(RedissonClient.class);
        @SuppressWarnings("unchecked")
        RBucket<String> bucket = mock(RBucket.class);
        when(redisson.<String>getBucket(
                "identity:v1:auth-code:" + DIGEST,
                StringCodec.INSTANCE
        )).thenReturn(bucket);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        AuthorizationCode authorizationCode = new AuthorizationCode(
                "alice-sub",
                "tenant-a",
                "tenant-user-a",
                "sso-session-1",
                "gateway-admin-web",
                URI.create("https://api.egon.internal/local/platform/gateway"),
                "platform-gateway-local",
                4L,
                "https://gateway.example.test/oauth/callback",
                "nonce-123",
                "challenge-123",
                Instant.parse("2026-08-02T00:00:00Z"),
                Instant.parse("2026-08-02T00:01:00Z")
        );
        String encoded = objectMapper.writeValueAsString(authorizationCode);
        when(bucket.setIfAbsent(encoded, Duration.ofSeconds(60)))
                .thenReturn(true);
        when(bucket.getAndDelete()).thenReturn(encoded).thenReturn(null);
        RedisAuthorizationCodeStore store = new RedisAuthorizationCodeStore(
                redisson,
                objectMapper,
                "identity:v1:auth-code:"
        );

        store.put(DIGEST, authorizationCode, Duration.ofSeconds(60));

        verify(bucket).setIfAbsent(encoded, Duration.ofSeconds(60));
        assertEquals(authorizationCode, store.consume(DIGEST));
        assertNull(store.consume(DIGEST));
        verify(bucket, org.mockito.Mockito.times(2)).getAndDelete();
    }

    @Test
    void jpaStoresBuildClientRedirectsAndExplicitResourceGrantSeparately() {
        IdentityClientRepository clients = mock(IdentityClientRepository.class);
        IdentityClientRedirectUriRepository redirects = mock(
                IdentityClientRedirectUriRepository.class
        );
        IdentityResourceServerRepository resources = mock(
                IdentityResourceServerRepository.class
        );
        IdentityClientResourceGrantRepository grants = mock(
                IdentityClientResourceGrantRepository.class
        );
        Instant now = Instant.parse("2026-08-02T00:00:00Z");
        IdentityClientEntity entity = IdentityClientEntity.createPublic(
                "gateway-admin-web",
                "Gateway Admin",
                900,
                604_800,
                now
        );
        when(clients.findById("gateway-admin-web"))
                .thenReturn(Optional.of(entity));
        when(redirects.findByClientId("gateway-admin-web")).thenReturn(List.of(
                IdentityClientRedirectUriEntity.create(
                        "redirect-1",
                        "gateway-admin-web",
                        "https://gateway.example.test/oauth/callback",
                        now
                )
        ));
        IdentityResourceServerEntity resource =
                IdentityResourceServerEntity.create(
                        "resource-row-1",
                        "platform-gateway-local",
                        "https://api.egon.internal/local/platform/gateway",
                        "platform",
                        "gateway",
                        "local",
                        "Gateway Local",
                        "gateway-admin-web",
                        "gateway",
                        "gateway:access",
                        300,
                        IdentityResourceServerEntity.Status.ACTIVE,
                        now
                );
        IdentityClientResourceGrantEntity userGrant =
                IdentityClientResourceGrantEntity.userDelegation(
                        "grant-row-1",
                        "gateway-admin-web",
                        resource.getResourceServerId(),
                        now
                );
        when(grants.findByClientIdAndResourceServerIdAndGrantTypeAndTenantId(
                "gateway-admin-web",
                resource.getResourceServerId(),
                IdentityClientResourceGrantEntity.GrantType.USER_DELEGATION,
                null
        )).thenReturn(Optional.of(userGrant));
        when(resources.findByResourceServerId(resource.getResourceServerId()))
                .thenReturn(Optional.of(resource));
        when(resources.findByResourceUri(resource.getResourceUri()))
                .thenReturn(Optional.of(resource));

        OAuthClient client = new JpaOAuthClientStore(clients, redirects)
                .findById("gateway-admin-web").orElseThrow();
        JpaResourceServerStore resourceStore = new JpaResourceServerStore(
                resources, grants, new ObjectMapper());

        assertEquals(OAuthClient.Status.ACTIVE, client.status());
        assertEquals(OAuthClient.ClientType.PUBLIC, client.clientType());
        assertEquals(Duration.ofMinutes(15), client.accessTokenTtl());
        assertEquals(Duration.ofDays(7), client.refreshTokenTtl());
        assertTrue(client.acceptsRedirectUri(
                "https://gateway.example.test/oauth/callback"
        ));
        assertFalse(Arrays.stream(OAuthClient.class.getRecordComponents())
                .anyMatch(component -> "audiences".equals(component.getName())));
        assertEquals(resource.getResourceUri(), resourceStore.findByUri(
                URI.create(resource.getResourceUri())).orElseThrow()
                .resourceUri().toString());
        assertTrue(resourceStore.findGrant(
                "gateway-admin-web", resource.getResourceServerId(),
                top.egon.cola.platform.idp.core.resource.ResourceGrantType.USER_DELEGATION,
                null).orElseThrow().active());
    }

    @Test
    void trustedRbac3AdapterSendsServiceCredentialAndMapsMembership() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder)
                .build();
        server.expect(once(), requestTo(
                        "http://127.0.0.1:19090/internal/v1/identity/resolve"
                ))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer service-token"))
                .andRespond(withSuccess("""
                        {
                          "data": {
                            "identitySub": "alice-sub",
                            "tenantId": "tenant-a",
                            "rbac3UserId": "tenant-user-a",
                            "tenantDisplayName": "Tenant A",
                            "status": "ACTIVE"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));
        AtomicReference<String> requestedTenant = new AtomicReference<>();
        HttpTenantMembershipAdapter adapter = new HttpTenantMembershipAdapter(
                builder.build(),
                "http://127.0.0.1:19090",
                tenantId -> {
                    requestedTenant.set(tenantId);
                    return "Bearer service-token";
                },
                () -> "Bearer default-service-token"
        );

        TenantMembershipPort.TenantMembership membership = adapter.resolve(
                "alice-sub",
                "tenant-a",
                "gateway-admin-web"
        );

        assertEquals("tenant-user-a", membership.rbac3UserId());
        assertEquals(TenantMembershipPort.MembershipStatus.ACTIVE,
                membership.status());
        assertEquals("tenant-a", requestedTenant.get());
        server.verify();
    }

    @Test
    void trustedRbac3ResourceDecisionAdapterReturnsMinimalAllowAndDeny()
            throws Exception {
        ObjectMapper strictMapper = new ObjectMapper()
                .findAndRegisterModules()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        RestClient.Builder builder = RestClient.builder()
                .messageConverters(converters -> converters.replaceAll(
                        converter -> converter
                                instanceof MappingJackson2HttpMessageConverter
                                ? new MappingJackson2HttpMessageConverter(strictMapper)
                                : converter));
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo(
                        "http://127.0.0.1:19090/internal/v1/authorization/"
                                + "resource-access-decisions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer service-token"))
                .andExpect(content().json("""
                        {"identitySub":"alice-sub","tid":"tenant-a",
                        "sid":"sso-session-1","rbacApplicationCode":"gateway",
                        "entryPermissionCode":"gateway:access"}
                        """))
                .andRespond(withSuccess("""
                        {"data":{"decision":"ALLOW","reasonCode":"ALLOW",
                        "authVersion":43,"sessionVersion":2,"policyVersion":18,
                        "decidedAt":"2026-08-10T00:00:00Z"},
                        "meta":{"requestId":"request-1","traceId":"trace-1",
                        "timestamp":"2026-08-10T00:00:01Z"}}
                        """, MediaType.APPLICATION_JSON));
        AtomicReference<String> requestedTenant = new AtomicReference<>();
        HttpUserResourceAccessAuthorizationAdapter adapter =
                new HttpUserResourceAccessAuthorizationAdapter(
                        builder.build(), "http://127.0.0.1:19090",
                        tenantId -> {
                            requestedTenant.set(tenantId);
                            return "Bearer service-token";
                        });

        UserResourceAccessAuthorizationPort.AccessDecision decision = adapter.decide(
                new UserResourceAccessAuthorizationPort.AccessRequest(
                        "alice-sub", "tenant-a", "sso-session-1",
                        "gateway", "gateway:access"));

        assertEquals(UserResourceAccessAuthorizationPort.Decision.ALLOW,
                decision.decision());
        assertEquals(43L, decision.authorizationVersion());
        assertEquals(2L, decision.contextVersion());
        assertEquals(18L, decision.policyVersion());
        assertEquals("tenant-a", requestedTenant.get());
        server.verify();
    }

    @Test
    void authorizeEndpointRedirectsOnlyToFacadeValidatedUri() throws Exception {
        AuthorizationFacade facade = mock(AuthorizationFacade.class);
        when(facade.authorize(
                any(), eq("alice-sub"), eq("sso-session-1"))).thenReturn(
                new AuthorizationFacade.AuthorizationResult(
                        "code-value",
                        "state-value",
                        "https://gateway.example.test/oauth/callback",
                        Instant.parse("2026-08-02T00:01:00Z")
                )
        );
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new OAuthAuthorizationController(facade)
        ).build();

        mockMvc.perform(get("/oauth2/authorize")
                        .principal(UsernamePasswordAuthenticationToken.authenticated(
                                new IdpSsoPrincipal(
                                        "alice-sub", "sso-session-1"),
                                "",
                                List.of()))
                        .param("response_type", "code")
                        .param("client_id", "gateway-admin-web")
                        .param("redirect_uri",
                                "https://gateway.example.test/oauth/callback")
                        .param("resource",
                                "https://api.egon.internal/local/platform/gateway")
                        .param("tenant_id", "tenant-a")
                        .param("state", "state-value")
                        .param("nonce", "nonce-value")
                        .param("code_challenge", "challenge-value")
                        .param("code_challenge_method", "S256"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        "Location",
                        "https://gateway.example.test/oauth/callback"
                                + "?code=code-value&state=state-value"
                ));
    }

    @Test
    void authorizeEndpointRequiresExactlyOneResourceAndRejectsAudience()
            throws Exception {
        AuthorizationFacade facade = mock(AuthorizationFacade.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new OAuthAuthorizationController(facade)).build();
        UsernamePasswordAuthenticationToken principal =
                UsernamePasswordAuthenticationToken.authenticated(
                        new IdpSsoPrincipal("alice-sub", "sso-session-1"),
                        "", List.of());

        var base = get("/oauth2/authorize")
                .principal(principal)
                .param("response_type", "code")
                .param("client_id", "gateway-admin-web")
                .param("redirect_uri",
                        "https://gateway.example.test/oauth/callback")
                .param("tenant_id", "tenant-a")
                .param("state", "state-value")
                .param("nonce", "nonce-value")
                .param("code_challenge", "challenge-value")
                .param("code_challenge_method", "S256");
        mvc.perform(base).andExpect(status().isBadRequest());

        mvc.perform(get("/oauth2/authorize")
                        .principal(principal)
                        .param("response_type", "code")
                        .param("client_id", "gateway-admin-web")
                        .param("redirect_uri",
                                "https://gateway.example.test/oauth/callback")
                        .param("resource", "https://api.example.test/a",
                                "https://api.example.test/b")
                        .param("tenant_id", "tenant-a")
                        .param("state", "state-value")
                        .param("nonce", "nonce-value")
                        .param("code_challenge", "challenge-value")
                        .param("code_challenge_method", "S256"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/oauth2/authorize")
                        .principal(principal)
                        .param("response_type", "code")
                        .param("client_id", "gateway-admin-web")
                        .param("redirect_uri",
                                "https://gateway.example.test/oauth/callback")
                        .param("resource", "https://api.example.test/a")
                        .param("audience", "legacy-api")
                        .param("tenant_id", "tenant-a")
                        .param("state", "state-value")
                        .param("nonce", "nonce-value")
                        .param("code_challenge", "challenge-value")
                        .param("code_challenge_method", "S256"))
                .andExpect(status().isBadRequest());
    }
}
