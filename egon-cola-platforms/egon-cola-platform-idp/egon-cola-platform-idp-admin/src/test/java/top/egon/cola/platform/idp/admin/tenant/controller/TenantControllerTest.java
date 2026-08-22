package top.egon.cola.platform.idp.admin.tenant.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import top.egon.cola.platform.idp.admin.support.security.IdpAdminAuthorizationPort;
import top.egon.cola.platform.idp.admin.tenant.domain.pojo.IdentityTenantEntity;
import top.egon.cola.platform.idp.admin.tenant.domain.pojo.IdentityTenantMembershipEntity;
import top.egon.cola.platform.idp.admin.tenant.service.TenantMembershipService;
import top.egon.cola.platform.idp.admin.tenant.service.TenantService;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TenantControllerTest {

    private static final Instant NOW =
            Instant.parse("2026-08-22T02:00:00Z");

    private final TenantService tenants = mock(TenantService.class);
    private final TenantMembershipService memberships =
            mock(TenantMembershipService.class);
    private final IdpAdminAuthorizationPort authorization =
            mock(IdpAdminAuthorizationPort.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new TenantController(
                        tenants,
                        memberships,
                        authorization,
                        objectMapper
                )
        ).setCustomArgumentResolvers(
                new AuthenticationPrincipalArgumentResolver()
        ).build();
    }

    @Test
    void listsTenantsWithReadPermissionAndStablePageShape() throws Exception {
        when(tenants.list()).thenReturn(List.of(
                tenant("10001", "acme", "Acme", IdentityTenantEntity.Status.ACTIVE),
                tenant("10002", "beta", "Beta", IdentityTenantEntity.Status.SUSPENDED)
        ));

        mockMvc.perform(get("/api/v1/identity/tenants")
                        .param("page", "0")
                        .param("size", "1")
                        .param("query", "ac")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tenantId").value("10001"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(authorization).require(
                isNull(),
                eq("idp:tenant:read")
        );
    }

    @Test
    void createsTenantWithManagePermissionAndIgnoresCallerIdentityFields()
            throws Exception {
        when(tenants.create(any(TenantService.CreateTenantCommand.class)))
                .thenReturn(tenant(
                        "10001",
                        "acme",
                        "Acme",
                        IdentityTenantEntity.Status.INITIALIZING
                ));

        mockMvc.perform(post("/api/v1/identity/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantCode":"acme",
                                  "tenantName":"Acme",
                                  "settings":{"region":"cn"},
                                  "tenantId":"attacker-id",
                                  "status":"ACTIVE",
                                  "version":99
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value("10001"))
                .andExpect(jsonPath("$.status").value("INITIALIZING"));

        verify(authorization).require(
                isNull(),
                eq("idp:tenant:manage")
        );
    }

    @Test
    void updatesTenantWithManagePermissionAndVersion() throws Exception {
        when(tenants.update(eq("10001"), any(TenantService.UpdateTenantCommand.class)))
                .thenReturn(tenant(
                        "10001",
                        "acme",
                        "Acme China",
                        IdentityTenantEntity.Status.ACTIVE
                ));

        mockMvc.perform(patch("/api/v1/identity/tenants/{tenantId}", "10001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expectedVersion":0,
                                  "tenantName":"Acme China",
                                  "settings":{"region":"cn"},
                                  "status":"ACTIVE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantName").value("Acme China"));

        verify(authorization).require(
                isNull(),
                eq("idp:tenant:manage")
        );
    }

    @Test
    void listsAndUpsertsMembershipWithSeparateReadAndManagePermissions()
            throws Exception {
        when(memberships.listByTenant("10001")).thenReturn(List.of(
                membership(
                        "10001",
                        "user-1",
                        IdentityTenantMembershipEntity.Status.ACTIVE,
                        0L
                )
        ));
        when(memberships.upsert(any(
                TenantMembershipService.UpsertMembershipCommand.class
        ))).thenReturn(membership(
                "10001",
                "user-1",
                IdentityTenantMembershipEntity.Status.ACTIVE,
                0L
        ));

        mockMvc.perform(get(
                        "/api/v1/identity/tenants/{tenantId}/members",
                        "10001"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].identitySub").value("user-1"));
        mockMvc.perform(put(
                        "/api/v1/identity/tenants/{tenantId}/members/{identitySub}",
                        "10001",
                        "user-1"
                ).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status":"ACTIVE",
                                  "expectedVersion":null
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value("10001"));

        verify(authorization).require(
                isNull(),
                eq("idp:tenant:read")
        );
        verify(authorization).require(
                isNull(),
                eq("idp:tenant:manage")
        );
    }

    private static TenantService.TenantView tenant(
            String id,
            String code,
            String name,
            IdentityTenantEntity.Status status
    ) {
        return new TenantService.TenantView(
                id,
                code,
                name,
                status,
                "{}",
                0L,
                NOW,
                NOW
        );
    }

    private static TenantMembershipService.MembershipView membership(
            String tenantId,
            String identitySub,
            IdentityTenantMembershipEntity.Status status,
            long version
    ) {
        return new TenantMembershipService.MembershipView(
                tenantId,
                identitySub,
                "Mario",
                status,
                version,
                NOW
        );
    }
}
