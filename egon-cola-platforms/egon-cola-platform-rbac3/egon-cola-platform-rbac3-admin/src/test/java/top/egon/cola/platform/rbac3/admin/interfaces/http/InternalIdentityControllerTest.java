package top.egon.cola.platform.rbac3.admin.interfaces.http;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import top.egon.cola.platform.idp.starter.security.RequiresServiceScope;
import top.egon.cola.platform.rbac3.admin.shared.repository.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.identity.application.IdentityMappingFacade;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalIdentityControllerTest {

    private final IdentityMappingFacade facade = mock(IdentityMappingFacade.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new InternalIdentityController(facade, mock(DatabaseClock.class)))
            .build();

    @Test
    void resolveReturnsTheStableIdpMembershipContract() throws Exception {
        when(facade.resolve("alice-sub", "7", "rbac3-admin-web"))
                .thenReturn(Optional.of(new IdentityMappingFacade.ResolvedMembership(
                        "7", "default", "Default Tenant", "alice-sub", "9",
                        "Alice", true, 3L, 5L)));

        mockMvc.perform(post("/internal/v1/identity/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identitySub": "alice-sub",
                                  "tenantId": "7",
                                  "clientId": "rbac3-admin-web"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.identitySub").value("alice-sub"))
                .andExpect(jsonPath("$.data.tenantId").value("7"))
                .andExpect(jsonPath("$.data.rbac3UserId").value("9"))
                .andExpect(jsonPath("$.data.tenantDisplayName")
                        .value("Default Tenant"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.authorizationContextRequired")
                        .value(true));
    }

    @Test
    void tenantsReturnIdentityAndActiveStatusForEveryMembership()
            throws Exception {
        when(facade.tenants("alice-sub", "mock-backend")).thenReturn(List.of(
                new IdentityMappingFacade.TenantMembership(
                        "7", "default", "Default Tenant", "9", "Alice")));

        mockMvc.perform(get("/internal/v1/identity/alice-sub/tenants")
                        .param("clientId", "mock-backend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].identitySub")
                        .value("alice-sub"))
                .andExpect(jsonPath("$.data[0].tenantDisplayName")
                        .value("Default Tenant"))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
    }

    @Test
    void internalIdentityOperationsDeclareIdpOwnedServiceScopes()
            throws NoSuchMethodException {
        assertThat(InternalIdentityController.class
                .getMethod("tenants", String.class, String.class)
                .getAnnotation(RequiresServiceScope.class).value())
                .isEqualTo("service:identity:resolve");
        assertThat(InternalIdentityController.class
                .getMethod("resolve", InternalIdentityController.ResolveRequest.class)
                .getAnnotation(RequiresServiceScope.class).value())
                .isEqualTo("service:identity:resolve");
        assertThat(InternalIdentityController.class
                .getMethod("bind", InternalIdentityController.BindRequest.class)
                .getAnnotation(RequiresServiceScope.class).value())
                .isEqualTo("service:identity:bind");
    }
}
