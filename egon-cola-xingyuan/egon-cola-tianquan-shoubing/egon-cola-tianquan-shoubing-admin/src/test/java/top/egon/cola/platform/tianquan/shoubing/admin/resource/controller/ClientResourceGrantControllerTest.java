package top.egon.cola.platform.tianquan.shoubing.admin.resource.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.vo.ClientResourceGrantVO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.service.ResourceServerService;
import top.egon.cola.platform.tianquan.shoubing.admin.support.security.IdpAdminAuthorizationPort;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClientResourceGrantControllerTest {

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

    private final ResourceServerService resources = mock(ResourceServerService.class);
    private final IdpAdminAuthorizationPort authorization = mock(IdpAdminAuthorizationPort.class);
    private final OAuthManagementRealmResolver realms = mock(OAuthManagementRealmResolver.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(realms.resolve(isNull(), isNull())).thenReturn(TENANT_REALM);
        when(realms.platformRealm()).thenReturn(PLATFORM_REALM);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ClientResourceGrantController(resources, authorization, realms)
        ).setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
    }

    @Test
    void putsAndDeletesOneApplicationLevelGrant() throws Exception {
        when(resources.putGrant(eq(TENANT_REALM), eq(PLATFORM_REALM), eq("tianquan-shoubing-service"),
                eq("permission-tianquan-jianshen-prod"), any())).thenReturn(grant());

        mockMvc.perform(put(
                        "/api/v1/tianquan-shoubing/clients/{clientId}/resources/{resourceServerId}",
                        "tianquan-shoubing-service", "permission-tianquan-jianshen-prod")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {
                                  "grantType":"CLIENT_CREDENTIALS",
                                  "tenantId":"tenant-1",
                                  "allowedScopes":["tianquan-jianshen:policy:read"],
                                  "expectedResourceVersion":0,
                                  "expectedGrantVersion":null
                                }
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(delete(
                        "/api/v1/tianquan-shoubing/clients/{clientId}/resources/{resourceServerId}",
                        "tianquan-shoubing-service", "permission-tianquan-jianshen-prod")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {
                                  "grantType":"CLIENT_CREDENTIALS",
                                  "tenantId":"tenant-1",
                                  "expectedResourceVersion":1,
                                  "expectedGrantVersion":0
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(resources).deleteGrant(eq(TENANT_REALM), eq(PLATFORM_REALM),
                eq("tianquan-shoubing-service"), eq("permission-tianquan-jianshen-prod"), any());
        verify(authorization, times(2)).require(isNull(), eq("tianquan-shoubing:resource-server:grant"));
    }

    @Test
    void userScopeSchemaRejectsInvalidTokensBeforeManagementWrites() throws Exception {
        for (String scope : List.of("*", "orders/read", "x".repeat(129))) {
            mockMvc.perform(put("/api/v1/tianquan-shoubing/clients/user-client/resources/orders")
                            .contentType(MediaType.APPLICATION_JSON).content("""
                                    {"grantType":"USER_DELEGATION","allowedScopes":["%s"],
                                     "expectedResourceVersion":0}
                                    """.formatted(scope)))
                    .andExpect(status().isBadRequest());
        }
        verify(resources, org.mockito.Mockito.never()).putGrant(any(), any(), any(), any(), any());
    }

    @Test
    void userGrantRequestAndViewRetainExactBusinessScopes() throws Exception {
        ClientResourceGrantVO userGrant = grant().setGrantType("USER_DELEGATION")
                .setTenantId(null).setAllowedScopes(Set.of("orders:read"));
        when(resources.putGrant(eq(TENANT_REALM), eq(PLATFORM_REALM), eq("user-client"), eq("orders"), any()))
                .thenReturn(userGrant);
        mockMvc.perform(put("/api/v1/tianquan-shoubing/clients/user-client/resources/orders")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"grantType":"USER_DELEGATION","allowedScopes":["orders:read"],
                                 "expectedResourceVersion":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"grantType\":\"USER_DELEGATION\",\"allowedScopes\":[\"orders:read\"]}"));
        verify(resources).putGrant(eq(TENANT_REALM), eq(PLATFORM_REALM), eq("user-client"), eq("orders"),
                org.mockito.ArgumentMatchers.argThat(value -> value.getAllowedScopes().equals(Set.of("orders:read"))
                        && value.getTenantId() == null));
    }

    @Test
    void batchGrantRequiresExplicitApplicationCodes() throws Exception {
        when(resources.batchGrants(eq(TENANT_REALM), eq(PLATFORM_REALM), eq("tianquan-shoubing-service"), any()))
                .thenReturn(List.of(grant()));

        mockMvc.perform(post(
                        "/api/v1/tianquan-shoubing/clients/{clientId}/resource-grants/actions/batch",
                        "tianquan-shoubing-service")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {
                                  "bizCode":"permission",
                                  "environment":"prod",
                                  "appCodes":["tianquan-jianshen"],
                                  "action":"UPSERT",
                                  "grantType":"CLIENT_CREDENTIALS",
                                  "tenantId":"tenant-1",
                                  "allowedScopes":["tianquan-jianshen:policy:read"],
                                  "expectedResourceVersions":{"tianquan-jianshen":0},
                                  "expectedGrantVersions":{}
                                }
                                """))
                .andExpect(status().isOk());

        verify(resources).batchGrants(eq(TENANT_REALM), eq(PLATFORM_REALM), eq("tianquan-shoubing-service"), any());
        verify(authorization).require(isNull(), eq("tianquan-shoubing:resource-server:grant"));
    }

    @Test
    void listsBoundedGrantsAndUsesNoStore() throws Exception {
        when(resources.listGrants(TENANT_REALM, PLATFORM_REALM, "tianquan-shoubing-service"))
                .thenReturn(List.of(grant()));

        mockMvc.perform(get("/api/v1/tianquan-shoubing/clients/{clientId}/resources",
                        "tianquan-shoubing-service"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().json("[{\"resourceServerId\":\"permission-tianquan-jianshen-prod\"}]"));

        verify(resources).listGrants(TENANT_REALM, PLATFORM_REALM, "tianquan-shoubing-service");
    }

    @Test
    void returnsUnavailableWhenBoundedQueryFindsMoreThanSixtyFourRows() throws Exception {
        List<ClientResourceGrantVO> overflow = new ArrayList<>();
        for (int index = 0; index < 65; index++) {
            overflow.add(grant());
        }
        when(resources.listGrants(TENANT_REALM, PLATFORM_REALM, "tianquan-shoubing-service"))
                .thenReturn(overflow);

        mockMvc.perform(get("/api/v1/tianquan-shoubing/clients/{clientId}/resources",
                        "tianquan-shoubing-service"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().json("{\"code\":\"TEMPORARILY_UNAVAILABLE\"}"));

        verify(resources).listGrants(TENANT_REALM, PLATFORM_REALM, "tianquan-shoubing-service");
    }

    private static ClientResourceGrantVO grant() {
        return ClientResourceGrantVO.builder()
                .clientId("tianquan-shoubing-service")
                .resourceServerId("permission-tianquan-jianshen-prod")
                .grantType("CLIENT_CREDENTIALS")
                .tenantId("tenant-1")
                .allowedScopes(Set.of("tianquan-jianshen:policy:read"))
                .status("ACTIVE")
                .version(0L)
                .build();
    }
}
