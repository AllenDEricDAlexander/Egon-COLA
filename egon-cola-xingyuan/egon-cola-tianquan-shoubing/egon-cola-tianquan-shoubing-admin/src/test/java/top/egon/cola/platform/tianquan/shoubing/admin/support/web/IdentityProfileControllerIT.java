package top.egon.cola.platform.tianquan.shoubing.admin.support.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.controller.IdentityAuditController;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.dto.IdentityAuditQueryDTO;
import org.mockito.ArgumentCaptor;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.vo.IdentityAuditPageVO;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.vo.IdentityAuditVO;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.service.IdentityAuditService;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.controller.IdentityProfileController;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller.OAuthUserInfoController;
import top.egon.cola.platform.tianquan.shoubing.admin.support.security.IdpAdminAuthenticationToken;
import top.egon.cola.platform.tianquan.shoubing.admin.support.security.IdpAdminAuthorizationPort;
import top.egon.cola.platform.tianquan.shoubing.admin.support.security.IdpSecurityConfig;
import top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
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
    void bindsEveryAuditFilterWithoutDroppingItAtTheController() throws Exception {
        when(audits.list(any())).thenReturn(new IdentityAuditPageVO(List.of(), 0, 20, 0, 0));
        mockMvc.perform(get("/api/v1/tianquan-shoubing/audits").with(identityJwt())
                        .param("page", "0").param("size", "20")
                        .param("actorSub", "alice-sub")
                        .param("eventType", "IDENTITY_LOGIN_SUCCEEDED")
                        .param("result", "SUCCESS")
                        .param("traceId", "trace-1")
                        .param("from", "2026-09-06T07:00:00Z")
                        .param("to", "2026-09-06T08:00:00Z"))
                .andExpect(status().isOk());
        var criteria = ArgumentCaptor.forClass(IdentityAuditQueryDTO.class);
        verify(audits).list(criteria.capture());
        assertThat(criteria.getValue())
                .hasFieldOrPropertyWithValue("actorSub", "alice-sub")
                .hasFieldOrPropertyWithValue("eventType", "IDENTITY_LOGIN_SUCCEEDED")
                .hasFieldOrPropertyWithValue("result", "SUCCESS")
                .hasFieldOrPropertyWithValue("traceId", "trace-1")
                .hasFieldOrPropertyWithValue("from", Instant.parse("2026-09-06T07:00:00Z"))
                .hasFieldOrPropertyWithValue("to", Instant.parse("2026-09-06T08:00:00Z"));
        verify(authorization).require(any(IdentityPrincipal.class), eq("tianquan-shoubing:audit:read"));
    }

    @Test
    void rejectsInvalidAuditIntervalsInsteadOfIgnoringThem() throws Exception {
        mockMvc.perform(get("/api/v1/tianquan-shoubing/audits").with(identityJwt())
                        .param("from", "2026-09-06T08:00:00Z")
                        .param("to", "2026-09-06T07:00:00Z"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/tianquan-shoubing/audits").with(identityJwt())
                        .param("from", "2026-09-06T15:00"))
                .andExpect(status().isBadRequest());
    }

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

        mockMvc.perform(get("/api/v1/tianquan-shoubing/audits")
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
                eq("tianquan-shoubing:audit:read")
        );
    }

    @Test
    void returnsAuthenticatedIdentityFromMeAndUserinfo() throws Exception {
        mockMvc.perform(get("/api/v1/tianquan-shoubing/me").with(identityJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("admin-sub"))
                .andExpect(jsonPath("$.tenantId").value("tenant-a"))
                .andExpect(jsonPath("$.tokenId").value("token-a"))
                .andExpect(jsonPath("$.audience[0]").value("tianquan-shoubing-admin"));
        verify(authorization).require(
                any(IdentityPrincipal.class),
                eq("tianquan-shoubing:identity:self:read")
        );

        mockMvc.perform(get("/oauth2/userinfo").with(identityJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").value("admin-sub"))
                .andExpect(jsonPath("$.tid").value("tenant-a"))
                .andExpect(jsonPath("$.aud[0]").value("tianquan-shoubing-admin"));
    }

    @Test
    void invalidAuditPageReturnsSafeBadRequest() throws Exception {
        when(audits.list(any())).thenThrow(
                new IllegalArgumentException("invalid audit page request")
        );
        mockMvc.perform(get("/api/v1/tianquan-shoubing/audits")
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
                        "token-a",
                        Set.of("tianquan-shoubing-admin"),
                        Instant.parse("2026-08-02T00:00:00Z"),
                        Instant.parse("2026-08-02T00:15:00Z"),
                        AuthenticationContext.password()
                ),
                "raw-token"
        ));
    }
}
