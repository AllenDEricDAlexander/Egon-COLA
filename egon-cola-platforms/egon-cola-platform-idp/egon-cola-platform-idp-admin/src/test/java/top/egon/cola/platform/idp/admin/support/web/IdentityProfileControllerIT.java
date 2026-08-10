package top.egon.cola.platform.idp.admin.support.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.platform.idp.admin.audit.controller.IdentityAuditController;
import top.egon.cola.platform.idp.admin.audit.domain.vo.IdentityAuditPageVO;
import top.egon.cola.platform.idp.admin.audit.domain.vo.IdentityAuditVO;
import top.egon.cola.platform.idp.admin.audit.service.IdentityAuditService;
import top.egon.cola.platform.idp.admin.identity.controller.IdentityProfileController;
import top.egon.cola.platform.idp.admin.oauth.controller.OAuthUserInfoController;
import top.egon.cola.platform.idp.admin.support.security.IdpAdminAuthenticationToken;
import top.egon.cola.platform.idp.admin.support.security.IdpAdminAuthorizationPort;
import top.egon.cola.platform.idp.admin.support.security.IdpSecurityConfig;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        IdentityAuditController.class,
        IdentityProfileController.class,
        OAuthUserInfoController.class
})
@Import(IdpSecurityConfig.class)
class IdentityProfileControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private IdentityAuditService audits;

    @MockitoBean
    private IdpAdminAuthorizationPort authorization;

    @Test
    void returnsOnlySafePagedAuditFields() throws Exception {
        IdentityAuditVO audit = new IdentityAuditVO(
                "audit-1",
                "IDENTITY_LOGIN_SUCCEEDED",
                "admin-sub",
                "alice-sub",
                "SUCCESS",
                "AUTHENTICATED",
                Instant.parse("2026-08-02T00:00:00Z")
        );
        when(audits.list(any())).thenReturn(new IdentityAuditPageVO(
                List.of(audit),
                0,
                20,
                1,
                1
        ));

        mockMvc.perform(get("/api/v1/identity/audits")
                        .param("page", "0")
                        .param("size", "20")
                        .with(identityJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("audit-1"))
                .andExpect(jsonPath("$.content[0].eventType")
                        .value("IDENTITY_LOGIN_SUCCEEDED"))
                .andExpect(jsonPath("$.content[0].payload").doesNotExist())
                .andExpect(jsonPath("$.size").value(20));

        verify(authorization).require(
                any(IdentityPrincipal.class),
                eq("idp:audit:read")
        );
    }

    @Test
    void returnsAuthenticatedIdentityFromMeAndUserinfo() throws Exception {
        mockMvc.perform(get("/api/v1/identity/me").with(identityJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("admin-sub"))
                .andExpect(jsonPath("$.tenantId").value("tenant-a"))
                .andExpect(jsonPath("$.sessionId").value("session-a"))
                .andExpect(jsonPath("$.tokenVersion").value(3));
        verify(authorization).require(
                any(IdentityPrincipal.class),
                eq("idp:identity:self:read")
        );

        mockMvc.perform(get("/oauth2/userinfo").with(identityJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").value("admin-sub"))
                .andExpect(jsonPath("$.tid").value("tenant-a"))
                .andExpect(jsonPath("$.sid").value("session-a"))
                .andExpect(jsonPath("$.client_id")
                        .value("idp-admin-web"))
                .andExpect(jsonPath("$.token_version").value(3));
    }

    @Test
    void invalidAuditPageReturnsSafeBadRequest() throws Exception {
        when(audits.list(any())).thenThrow(
                new IllegalArgumentException("invalid audit page request")
        );
        mockMvc.perform(get("/api/v1/identity/audits")
                        .param("page", "-1")
                        .with(identityJwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("request is invalid"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor identityJwt() {
        return authentication(new IdpAdminAuthenticationToken(
                new IdentityPrincipal(
                        "admin-sub",
                        "tenant-a",
                        "session-a",
                        "idp-admin-web",
                        "token-a",
                        3L,
                        Set.of("idp-admin"),
                        Instant.parse("2026-08-02T00:00:00Z"),
                        Instant.parse("2026-08-02T00:15:00Z")
                ),
                "raw-token"
        ));
    }
}
