package top.egon.cola.platform.tianquan.shoubing.admin.support.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller.InternalRefreshTokenController;
import top.egon.cola.platform.tianquan.shoubing.core.token.RefreshTokenStatus;
import top.egon.cola.platform.tianquan.shoubing.core.token.TokenFacade;

import java.time.Instant;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalRefreshTokenController.class)
@Import(IdpSecurityConfig.class)
class InternalRefreshTokenSecurityIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private TokenFacade tokens;

    @Test
    void authenticatedServicePostDoesNotRequireBrowserCsrf() throws Exception {
        when(tokens.validateRefresh("refresh-token")).thenReturn(
                new RefreshTokenStatus(
                        "subject-1",
                        "tenant-1",
                        Instant.now().plusSeconds(60)
                ));
        var service = new TestingAuthenticationToken(
                "yuheng-biz-gateway", null);
        service.setAuthenticated(true);

        mockMvc.perform(post(
                        "/internal/v1/oauth2/refresh-token/validate")
                        .with(authentication(service))
                        .contentType("application/x-www-form-urlencoded")
                        .param("token", "refresh-token"))
                .andExpect(status().isOk());

        verify(tokens).validateRefresh("refresh-token");
    }
}
