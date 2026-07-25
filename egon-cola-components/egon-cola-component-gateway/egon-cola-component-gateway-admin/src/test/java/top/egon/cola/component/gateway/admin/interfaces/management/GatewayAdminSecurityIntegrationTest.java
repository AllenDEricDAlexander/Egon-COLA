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
import top.egon.cola.component.gateway.admin.infrastructure.security.GatewayAdminSecurityConfiguration;
import top.egon.cola.component.gateway.admin.interfaces.openapi.GatewayReportHmacFilter;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {
                GatewayGroupController.class,
                GatewayAdminSessionController.class
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
}
