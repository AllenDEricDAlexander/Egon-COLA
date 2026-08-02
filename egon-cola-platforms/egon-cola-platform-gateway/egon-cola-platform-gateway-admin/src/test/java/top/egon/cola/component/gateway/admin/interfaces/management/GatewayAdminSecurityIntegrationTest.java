package top.egon.cola.component.gateway.admin.interfaces.management;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.gateway.admin.application.GatewayGroupService;
import top.egon.cola.component.gateway.admin.application.scope.GatewayScopeService;
import top.egon.cola.component.gateway.admin.infrastructure.security.GatewayAdminSecurityConfiguration;
import top.egon.cola.component.gateway.admin.interfaces.openapi.GatewayReportHmacFilter;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;
import top.egon.cola.platform.rbac3.starter.security.Rbac3AuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {
                GatewayGroupController.class,
                GatewayAdminSessionController.class,
                GatewayScopeController.class
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = GatewayReportHmacFilter.class
        )
)
@Import({
        GatewayAdminSecurityConfiguration.class,
        GatewayAdminWebMvcConfiguration.class
})
class GatewayAdminSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GatewayGroupService service;

    @MockBean
    private GatewayScopeService scopeService;

    @Test
    void rejectsForgedActorHeaderWithoutBearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/gateway/admin/gateway-groups")
                        .header("X-Admin-Actor-Id", "forged-admin"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deniesAuthenticatedPrincipalWithoutReadCapability()
            throws Exception {
        mockMvc.perform(get("/api/v1/gateway/admin/gateway-groups")
                        .with(jwt().jwt(token -> token
                                .subject("limited-user"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void tokenRoleClaimDoesNotGrantGatewayPermission() throws Exception {
        mockMvc.perform(get("/api/v1/gateway/admin/gateway-groups")
                        .with(jwt().jwt(token -> token.subject("limited-user")
                                .claim("roles", List.of("ADMIN"))
                                .claim("capabilities", List.of("gateway:read")))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rbac3SnapshotGrantsGatewayPermission() throws Exception {
        when(service.list()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/gateway/admin/gateway-groups")
                        .with(authentication(rbac3("gateway:read"))))
                .andExpect(status().isOk());
    }

    @Test
    void rbac3PrincipalKeepsTheStableIdentitySubjectForAudit() throws Exception {
        mockMvc.perform(get("/api/v1/gateway/admin/session")
                        .with(authentication(rbac3("gateway:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actorId").value("admin-sub"))
                .andExpect(jsonPath("$.capabilities[0]").value("gateway:read"));
    }

    @Test
    void permitsPrincipalWithReadCapability() throws Exception {
        when(service.list()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/gateway/admin/gateway-groups")
                        .with(jwt()
                                .jwt(token -> token.subject("reader"))
                                .authorities(new SimpleGrantedAuthority(
                                        "CAP_gateway:read"
                                ))))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsUnauthenticatedScopeCatalogRequest() throws Exception {
        mockMvc.perform(get("/api/v1/gateway/admin/scopes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void permitsScopeCatalogWithReadCapability() throws Exception {
        when(scopeService.list()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/gateway/admin/scopes")
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                "CAP_gateway:read"
                        ))))
                .andExpect(status().isOk());
    }

    @Test
    void exposesVerifiedActorAndCapabilitiesToTheAdminWeb()
            throws Exception {
        mockMvc.perform(get("/api/v1/gateway/admin/session")
                        .with(jwt()
                                .jwt(token -> token
                                        .subject("admin-1")
                                        .claim("preferred_username", "Mario"))
                                .authorities(
                                        new SimpleGrantedAuthority(
                                                "CAP_gateway:read"
                                        ),
                                        new SimpleGrantedAuthority(
                                                "CAP_gateway:groups:write"
                                        ),
                                        new SimpleGrantedAuthority(
                                                "ROLE_gateway-admin"
                                        )
                                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actorId").value("admin-1"))
                .andExpect(jsonPath("$.displayName").value("Mario"))
                .andExpect(jsonPath("$.capabilities[0]")
                        .value("gateway:groups:write"))
                .andExpect(jsonPath("$.capabilities[1]")
                        .value("gateway:read"))
                .andExpect(jsonPath("$.roles[0]")
                        .value("gateway-admin"));
    }

    private Rbac3AuthenticationToken rbac3(String permission) {
        Instant now = Instant.parse("2026-08-02T04:00:00Z");
        IdentityPrincipal identity = new IdentityPrincipal(
                "admin-sub", "tenant-a", "sid-1", "gateway-admin-web",
                "token-1", 2, Set.of("gateway-admin-web"),
                now, now.plusSeconds(900));
        SystemAuthorizationSnapshot snapshot = new SystemAuthorizationSnapshot(
                "tenant-a", "admin-sub", "101", "sid-1", "gateway-admin",
                3, 4, 5, List.of("gateway-reader"), Set.of(permission),
                Map.of(), Map.of(), "sha256:gateway", now, now.plusSeconds(900));
        return new Rbac3AuthenticationToken(
                new AuthorizationService.RuntimeAuthorizationContext(
                        identity, snapshot, false));
    }
}
