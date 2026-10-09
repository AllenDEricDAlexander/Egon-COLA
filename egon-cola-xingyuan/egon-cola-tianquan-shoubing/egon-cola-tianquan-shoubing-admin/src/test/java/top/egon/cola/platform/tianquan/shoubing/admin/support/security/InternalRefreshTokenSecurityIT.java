package top.egon.cola.platform.tianquan.shoubing.admin.support.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller.filter.TenantLoginAuthenticationFilter;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.controller.IdentityProfileController;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller.filter.LegacyOAuthRetirementFilter;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = IdentityProfileController.class, excludeFilters =
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TenantLoginAuthenticationFilter.class))
@Import({IdpSecurityConfig.class, LegacyOAuthRetirementFilter.class})
class InternalRefreshTokenSecurityIT {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private IdpAdminAuthorizationPort authorization;

    @Test
    void anonymousAndAuthenticatedServiceRequestsRetireWithoutCsrfOrTokenValidation() throws Exception {
        var service = new TestingAuthenticationToken("yuheng-biz-gateway", null);
        service.setAuthenticated(true);
        for (boolean authenticated : new boolean[]{false, true}) {
            var request = post("/internal/v1/oauth2/refresh-token/validate")
                    .contentType("application/x-www-form-urlencoded")
                    .header("Authorization", "Basic malformed")
                    .param("token", "retired-refresh-token");
            if (authenticated) {
                request.with(authentication(service));
            }
            mockMvc.perform(request)
                    .andExpect(status().isGone())
                    .andExpect(jsonPath("$.error").value("legacy_endpoint_retired"))
                    .andExpect(jsonPath("$.access_token").doesNotExist())
                    .andExpect(header().doesNotExist("Set-Cookie"))
                    .andExpect(header().doesNotExist("Location"));
        }
    }
}
