package top.egon.cola.platform.idp.admin.resource.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import top.egon.cola.platform.idp.admin.resource.domain.vo.ClientResourceGrantVO;
import top.egon.cola.platform.idp.admin.resource.service.ResourceServerService;
import top.egon.cola.platform.idp.admin.support.security.IdpAdminAuthorizationPort;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClientResourceGrantControllerTest {

    private final ResourceServerService resources =
            mock(ResourceServerService.class);
    private final IdpAdminAuthorizationPort authorization =
            mock(IdpAdminAuthorizationPort.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ClientResourceGrantController(resources, authorization)
        ).setCustomArgumentResolvers(
                new AuthenticationPrincipalArgumentResolver()
        ).build();
    }

    @Test
    void putsAndDeletesOneApplicationLevelGrant() throws Exception {
        when(resources.putGrant(any(), any(), any())).thenReturn(grant());

        mockMvc.perform(put(
                        "/api/v1/identity/clients/{clientId}/resources/{resourceServerId}",
                        "idp-service",
                        "permission-rbac3-prod"
                ).contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "grantType":"CLIENT_CREDENTIALS",
                          "tenantId":"tenant-1",
                          "allowedScopes":["rbac3:policy:read"],
                          "expectedResourceVersion":0,
                          "expectedGrantVersion":null
                        }
                        """))
                .andExpect(status().isOk());
        mockMvc.perform(delete(
                        "/api/v1/identity/clients/{clientId}/resources/{resourceServerId}",
                        "idp-service",
                        "permission-rbac3-prod"
                ).contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "grantType":"CLIENT_CREDENTIALS",
                          "tenantId":"tenant-1",
                          "expectedResourceVersion":1,
                          "expectedGrantVersion":0
                        }
                        """))
                .andExpect(status().isNoContent());

        verify(resources).deleteGrant(
                eq("idp-service"),
                eq("permission-rbac3-prod"),
                any()
        );
        verify(authorization, org.mockito.Mockito.times(2)).require(
                isNull(),
                eq("idp:resource-server:grant")
        );
    }

    @Test
    void batchGrantRequiresExplicitApplicationCodes() throws Exception {
        when(resources.batchGrants(eq("idp-service"), any()))
                .thenReturn(List.of(grant()));

        mockMvc.perform(post(
                        "/api/v1/identity/clients/{clientId}/resource-grants/actions/batch",
                        "idp-service"
                ).contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "bizCode":"permission",
                          "environment":"prod",
                          "appCodes":["rbac3"],
                          "action":"UPSERT",
                          "grantType":"CLIENT_CREDENTIALS",
                          "tenantId":"tenant-1",
                          "allowedScopes":["rbac3:policy:read"],
                          "expectedResourceVersions":{"rbac3":0},
                          "expectedGrantVersions":{}
                        }
                        """))
                .andExpect(status().isOk());

        verify(resources).batchGrants(eq("idp-service"), any());
        verify(authorization).require(
                isNull(),
                eq("idp:resource-server:grant")
        );
    }

    private static ClientResourceGrantVO grant() {
        return new ClientResourceGrantVO(
                "idp-service",
                "permission-rbac3-prod",
                "CLIENT_CREDENTIALS",
                "tenant-1",
                Set.of("rbac3:policy:read"),
                "ACTIVE",
                0L
        );
    }
}
