package top.egon.cola.platform.idp.admin.resource.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import top.egon.cola.platform.idp.admin.resource.domain.vo.ClientJwkVO;
import top.egon.cola.platform.idp.admin.resource.domain.vo.ResourceServerVO;
import top.egon.cola.platform.idp.admin.resource.service.ResourceServerService;
import top.egon.cola.platform.idp.admin.support.security.IdpAdminAuthorizationPort;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResourceServerControllerTest {

    private final ResourceServerService resources =
            mock(ResourceServerService.class);
    private final IdpAdminAuthorizationPort authorization =
            mock(IdpAdminAuthorizationPort.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ResourceServerController(resources, authorization)
        ).setCustomArgumentResolvers(
                new AuthenticationPrincipalArgumentResolver()
        ).build();
    }

    @Test
    void listsAndReadsResourceServersWithReadPermission() throws Exception {
        when(resources.list()).thenReturn(List.of(view()));
        when(resources.detail("permission-idp-prod")).thenReturn(view());

        mockMvc.perform(get("/api/v1/identity/resource-servers"))
                .andExpect(status().isOk());
        mockMvc.perform(get(
                        "/api/v1/identity/resource-servers/{resourceServerId}",
                        "permission-idp-prod"
                ))
                .andExpect(status().isOk());

        verify(authorization, org.mockito.Mockito.times(2)).require(
                isNull(),
                eq("idp:resource-server:read")
        );
    }

    @Test
    void createsResourceServerWithCreatePermission() throws Exception {
        when(resources.create(any())).thenReturn(view());

        mockMvc.perform(post("/api/v1/identity/resource-servers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resourceServerId":"permission-idp-prod",
                                  "resourceUri":"https://api.egon.internal/prod/permission/idp",
                                  "bizCode":"permission",
                                  "appCode":"idp",
                                  "environment":"prod",
                                  "displayName":"IdP Production",
                                  "managementClientId":"idp-service",
                                  "rbacApplicationCode":"idp",
                                  "entryPermissionCode":"idp:access",
                                  "admissionTicketTtlSeconds":300,
                                  "key":{
                                    "kid":"idp-prod-2026-08",
                                    "algorithm":"RS256",
                                    "publicJwk":"{\\"kty\\":\\"RSA\\"}",
                                    "validFrom":"2026-08-10T00:00:00Z",
                                    "validTo":"2026-08-11T00:00:00Z",
                                    "expectedResourceVersion":0
                                  }
                                }
                                """))
                .andExpect(status().isCreated());

        verify(authorization).require(
                isNull(),
                eq("idp:resource-server:create")
        );
    }

    @Test
    void statusAndKeyEndpointsUseDedicatedPermissions() throws Exception {
        when(resources.enable(any(), any())).thenReturn(view());
        when(resources.disable(any(), any())).thenReturn(view());
        when(resources.addKey(any(), any())).thenReturn(view());
        when(resources.removeKey(any(), any(), any(Long.class), any(Long.class)))
                .thenReturn(view());

        String version = "{\"expectedVersion\":0}";
        mockMvc.perform(post(
                        "/api/v1/identity/resource-servers/{resourceServerId}/enable",
                        "permission-idp-prod"
                ).contentType(MediaType.APPLICATION_JSON).content(version))
                .andExpect(status().isOk());
        mockMvc.perform(post(
                        "/api/v1/identity/resource-servers/{resourceServerId}/disable",
                        "permission-idp-prod"
                ).contentType(MediaType.APPLICATION_JSON).content(version))
                .andExpect(status().isOk());
        mockMvc.perform(post(
                        "/api/v1/identity/resource-servers/{resourceServerId}/keys",
                        "permission-idp-prod"
                ).contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "kid":"next-key",
                          "algorithm":"RS256",
                          "publicJwk":"{\\"kty\\":\\"RSA\\"}",
                          "validFrom":"2026-08-10T00:00:00Z",
                          "validTo":"2026-08-11T00:00:00Z",
                          "expectedResourceVersion":0
                        }
                        """))
                .andExpect(status().isOk());
        mockMvc.perform(delete(
                        "/api/v1/identity/resource-servers/{resourceServerId}/keys/{kid}",
                        "permission-idp-prod",
                        "next-key"
                ).queryParam("expectedResourceVersion", "1")
                        .queryParam("expectedKeyVersion", "0"))
                .andExpect(status().isOk());

        verify(authorization, org.mockito.Mockito.times(2)).require(
                isNull(),
                eq("idp:resource-server:status")
        );
        verify(authorization, org.mockito.Mockito.times(2)).require(
                isNull(),
                eq("idp:resource-server:key")
        );
    }

    private static ResourceServerVO view() {
        Instant now = Instant.parse("2026-08-10T00:00:00Z");
        return new ResourceServerVO(
                "permission-idp-prod",
                "https://api.egon.internal/prod/permission/idp",
                "permission",
                "idp",
                "prod",
                "IdP Production",
                "idp-service",
                "idp",
                "idp:access",
                300,
                "DISABLED",
                0L,
                List.of(new ClientJwkVO(
                        "idp-prod-2026-08",
                        "RS256",
                        "ACTIVE",
                        now,
                        now.plusSeconds(3600),
                        null,
                        0L
                )),
                now,
                now
        );
    }
}
