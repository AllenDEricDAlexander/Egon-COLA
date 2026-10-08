package top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.CreatedOAuthClientVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.OAuthClientVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.RotatedClientSecretVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.OAuthClientService;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver;
import top.egon.cola.platform.tianquan.shoubing.admin.support.security.IdpAdminAuthorizationPort;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OAuthClientControllerTest {

    private static final TrustedRealmBO TENANT_REALM = TrustedRealmBO.builder()
            .issuer(URI.create("https://idp.example.test/tenants/7"))
            .realmKey("tenant:7")
            .realmType(RealmTypeEnum.TENANT)
            .storagePartitionId(7L)
            .businessTenantId(7L)
            .trustVersion(1L)
            .build();
    private static final TrustedRealmBO PLATFORM_REALM = TrustedRealmBO.builder()
            .issuer(URI.create("https://idp.example.test/platform"))
            .realmKey("platform")
            .realmType(RealmTypeEnum.PLATFORM)
            .storagePartitionId(1L)
            .trustVersion(1L)
            .build();

    private final OAuthClientService clients = mock(OAuthClientService.class);
    private final IdpAdminAuthorizationPort authorization = mock(IdpAdminAuthorizationPort.class);
    private final OAuthManagementRealmResolver realms = mock(OAuthManagementRealmResolver.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(realms.resolve(isNull(), isNull())).thenReturn(TENANT_REALM);
        when(realms.resolve(isNull(), eq("tenant:7"))).thenReturn(TENANT_REALM);
        when(realms.platformRealm()).thenReturn(PLATFORM_REALM);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new OAuthClientController(clients, authorization, realms)
        ).setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
    }

    @Test
    void createsClientWithCreatePermissionAndNoStoreSecretResponse() throws Exception {
        when(clients.create(eq(TENANT_REALM), eq(PLATFORM_REALM), any(), eq("SYSTEM")))
                .thenReturn(created());

        mockMvc.perform(post("/api/v1/tianquan-shoubing/clients?realm=tenant:7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "appId":"orders-service",
                                  "clientId":"orders-service-local",
                                  "clientName":"Orders Service",
                                  "clientType":"CONFIDENTIAL",
                                  "purpose":"SERVICE",
                                  "accessTokenTtlSeconds":600,
                                  "refreshTokenTtlSeconds":86400,
                                  "redirectUris":[],
                                  "resourceUris":[],
                                  "allowedScopes":["orders:read"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().json("""
                        {"clientId":"orders-service-local","issuer":"https://idp.example.test/tenants/7"}
                        """));

        verify(authorization).require(isNull(), eq("tianquan-shoubing:oauth-client:create"));
        verify(realms).resolve(isNull(), eq("tenant:7"));
        verify(clients).create(eq(TENANT_REALM), eq(PLATFORM_REALM), any(), eq("SYSTEM"));
    }

    @Test
    void rotatesClientSecretWithUpdatePermissionAndNoStoreResponse() throws Exception {
        when(clients.rotateSecret(eq(TENANT_REALM), eq("orders-service-local"), any(), eq("SYSTEM")))
                .thenReturn(rotated());

        mockMvc.perform(post("/api/v1/tianquan-shoubing/clients/{clientId}/secret-rotations",
                        "orders-service-local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("new-one-time-secret")));

        verify(authorization).require(isNull(), eq("tianquan-shoubing:oauth-client:update"));
        verify(clients).rotateSecret(eq(TENANT_REALM), eq("orders-service-local"), any(), eq("SYSTEM"));
    }

    @Test
    void listsClientsWithReadPermissionAndSafeViewContract() throws Exception {
        when(clients.list(TENANT_REALM)).thenReturn(List.of(OAuthClientVO.builder()
                .clientId("orders-service-local")
                .appId("orders-service")
                .clientName("Orders Service")
                .clientType("CONFIDENTIAL")
                .purpose("SERVICE")
                .status("ACTIVE")
                .pkceRequired(false)
                .accessTokenTtlSeconds(900)
                .refreshTokenTtlSeconds(0)
                .redirectUris(List.of())
                .resourceUris(List.of("https://api.example.test/orders"))
                .allowedScopes(List.of("orders:read"))
                .version(0L)
                .createdAt(Instant.EPOCH)
                .updatedAt(Instant.EPOCH)
                .secretHint("7Kp2")
                .secretStatus("ACTIVE")
                .issuer(TENANT_REALM.getIssuer().toString())
                .build()));

        mockMvc.perform(get("/api/v1/tianquan-shoubing/clients"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"clientId":"orders-service-local","issuer":"https://idp.example.test/tenants/7"}]
                        """))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("clientSecret"))));

        verify(authorization).require(isNull(), eq("tianquan-shoubing:oauth-client:read"));
        verify(clients).list(TENANT_REALM);
    }

    private static CreatedOAuthClientVO created() {
        return CreatedOAuthClientVO.builder()
                .clientId("orders-service-local")
                .appId("orders-service")
                .clientName("Orders Service")
                .clientType("CONFIDENTIAL")
                .purpose("SERVICE")
                .status("ACTIVE")
                .clientSecret("one-time-secret")
                .secretHint("7Kp2")
                .version(0L)
                .createdAt(Instant.EPOCH)
                .issuer(TENANT_REALM.getIssuer().toString())
                .build();
    }

    private static RotatedClientSecretVO rotated() {
        return RotatedClientSecretVO.builder()
                .clientId("orders-service-local")
                .appId("orders-service")
                .clientSecret("new-one-time-secret")
                .secretHint("yQ8m")
                .version(1L)
                .rotatedAt(Instant.EPOCH)
                .issuer(TENANT_REALM.getIssuer().toString())
                .build();
    }
}
