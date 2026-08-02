package top.egon.cola.platform.idp.admin.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;
import top.egon.cola.platform.idp.admin.integration.rbac3.HttpTenantMembershipAdapter;
import top.egon.cola.platform.idp.admin.interfaces.http.OAuthAuthorizationController;
import top.egon.cola.platform.idp.admin.oauth.domain.IdentityClientAudienceEntity;
import top.egon.cola.platform.idp.admin.oauth.domain.IdentityClientEntity;
import top.egon.cola.platform.idp.admin.oauth.domain.IdentityClientRedirectUriEntity;
import top.egon.cola.platform.idp.admin.oauth.infrastructure.IdentityClientAudienceRepository;
import top.egon.cola.platform.idp.admin.oauth.infrastructure.IdentityClientRedirectUriRepository;
import top.egon.cola.platform.idp.admin.oauth.infrastructure.IdentityClientRepository;
import top.egon.cola.platform.idp.admin.oauth.infrastructure.JpaOAuthClientStore;
import top.egon.cola.platform.idp.admin.oauth.infrastructure.RedisAuthorizationCodeStore;
import top.egon.cola.platform.idp.core.oauth.AuthorizationCode;
import top.egon.cola.platform.idp.core.oauth.AuthorizationFacade;
import top.egon.cola.platform.idp.core.oauth.OAuthClient;
import top.egon.cola.platform.idp.core.port.TenantMembershipPort;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
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
                "gateway-admin-web",
                "gateway-admin",
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
    void jpaStoreBuildsClientFromExactRedirectsAndAudiences() {
        IdentityClientRepository clients = mock(IdentityClientRepository.class);
        IdentityClientRedirectUriRepository redirects = mock(
                IdentityClientRedirectUriRepository.class
        );
        IdentityClientAudienceRepository audiences = mock(
                IdentityClientAudienceRepository.class
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
        when(audiences.findByClientId("gateway-admin-web")).thenReturn(List.of(
                IdentityClientAudienceEntity.create(
                        "audience-1",
                        "gateway-admin-web",
                        "gateway-admin",
                        now
                )
        ));

        OAuthClient client = new JpaOAuthClientStore(
                clients,
                redirects,
                audiences
        ).findById("gateway-admin-web").orElseThrow();

        assertEquals(OAuthClient.Status.ACTIVE, client.status());
        assertEquals(OAuthClient.ClientType.PUBLIC, client.clientType());
        assertTrue(client.acceptsRedirectUri(
                "https://gateway.example.test/oauth/callback"
        ));
        assertTrue(client.acceptsAudience("gateway-admin"));
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
                          "identitySub": "alice-sub",
                          "tenantId": "tenant-a",
                          "rbac3UserId": "tenant-user-a",
                          "tenantDisplayName": "Tenant A",
                          "status": "ACTIVE"
                        }
                        """, MediaType.APPLICATION_JSON));
        HttpTenantMembershipAdapter adapter = new HttpTenantMembershipAdapter(
                builder.build(),
                "http://127.0.0.1:19090",
                () -> "Bearer service-token"
        );

        TenantMembershipPort.TenantMembership membership = adapter.resolve(
                "alice-sub",
                "tenant-a",
                "gateway-admin-web"
        );

        assertEquals("tenant-user-a", membership.rbac3UserId());
        assertEquals(TenantMembershipPort.MembershipStatus.ACTIVE,
                membership.status());
        server.verify();
    }

    @Test
    void authorizeEndpointRedirectsOnlyToFacadeValidatedUri() throws Exception {
        AuthorizationFacade facade = mock(AuthorizationFacade.class);
        when(facade.authorize(any(), eq("alice-sub"))).thenReturn(
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
                        .principal(() -> "alice-sub")
                        .param("response_type", "code")
                        .param("client_id", "gateway-admin-web")
                        .param("redirect_uri",
                                "https://gateway.example.test/oauth/callback")
                        .param("audience", "gateway-admin")
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
}
