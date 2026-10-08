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
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.vo.ResourceServerVO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.service.ResourceServerService;
import top.egon.cola.platform.tianquan.shoubing.admin.support.security.IdpAdminAuthorizationPort;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResourceServerControllerTest {

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
        when(realms.resolve(isNull(), isNull())).thenReturn(PLATFORM_REALM);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ResourceServerController(resources, authorization, realms)
        ).setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
    }

    @Test
    void listsAndReadsResourceServersWithReadPermission() throws Exception {
        when(resources.list(PLATFORM_REALM)).thenReturn(List.of(view()));
        when(resources.detail(PLATFORM_REALM, "permission-tianquan-shoubing-prod")).thenReturn(view());

        mockMvc.perform(get("/api/v1/tianquan-shoubing/resource-servers"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/tianquan-shoubing/resource-servers/{resourceServerId}",
                        "permission-tianquan-shoubing-prod"))
                .andExpect(status().isOk());

        verify(authorization, times(2)).require(isNull(), eq("tianquan-shoubing:resource-server:read"));
        verify(resources).list(PLATFORM_REALM);
        verify(resources).detail(PLATFORM_REALM, "permission-tianquan-shoubing-prod");
    }

    @Test
    void createsResourceServerWithCreatePermission() throws Exception {
        when(resources.create(eq(PLATFORM_REALM), any())).thenReturn(view());

        mockMvc.perform(post("/api/v1/tianquan-shoubing/resource-servers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resourceServerId":"permission-tianquan-shoubing-prod",
                                  "resourceUri":"https://api.egon.internal/prod/permission/tianquan-shoubing",
                                  "bizCode":"permission",
                                  "appCode":"tianquan-shoubing",
                                  "environment":"prod",
                                  "displayName":"Tianquan-Shoubing Production",
                                  "managementClientId":"tianquan-shoubing-service",
                                  "rbacApplicationCode":"tianquan-shoubing",
                                  "entryPermissionCode":"tianquan-shoubing:access"
                                }
                                """))
                .andExpect(status().isCreated());

        verify(authorization).require(isNull(), eq("tianquan-shoubing:resource-server:create"));
        verify(resources).create(eq(PLATFORM_REALM), any());
    }

    @Test
    void statusEndpointUsesDedicatedPermission() throws Exception {
        when(resources.enable(eq(PLATFORM_REALM), eq("permission-tianquan-shoubing-prod"), any()))
                .thenReturn(view());
        when(resources.disable(eq(PLATFORM_REALM), eq("permission-tianquan-shoubing-prod"), any()))
                .thenReturn(view());

        String version = "{\"expectedVersion\":0}";
        mockMvc.perform(post("/api/v1/tianquan-shoubing/resource-servers/{resourceServerId}/enable",
                        "permission-tianquan-shoubing-prod")
                        .contentType(MediaType.APPLICATION_JSON).content(version))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/tianquan-shoubing/resource-servers/{resourceServerId}/disable",
                        "permission-tianquan-shoubing-prod")
                        .contentType(MediaType.APPLICATION_JSON).content(version))
                .andExpect(status().isOk());
        verify(authorization, times(2)).require(isNull(), eq("tianquan-shoubing:resource-server:status"));
    }

    private static ResourceServerVO view() {
        Instant now = Instant.parse("2026-08-10T00:00:00Z");
        return ResourceServerVO.builder()
                .resourceServerId("permission-tianquan-shoubing-prod")
                .resourceUri("https://api.egon.internal/prod/permission/tianquan-shoubing")
                .bizCode("permission")
                .appCode("tianquan-shoubing")
                .environment("prod")
                .displayName("Tianquan-Shoubing Production")
                .managementClientId("tianquan-shoubing-service")
                .rbacApplicationCode("tianquan-shoubing")
                .entryPermissionCode("tianquan-shoubing:access")
                .status("DISABLED")
                .version(0L)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
