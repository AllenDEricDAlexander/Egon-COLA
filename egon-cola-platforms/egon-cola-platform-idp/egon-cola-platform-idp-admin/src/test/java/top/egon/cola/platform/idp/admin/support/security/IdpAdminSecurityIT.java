package top.egon.cola.platform.idp.admin.support.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.platform.idp.admin.identity.controller.IdentityUserController;
import top.egon.cola.platform.idp.admin.identity.domain.vo.CreatedIdentityUserVO;
import top.egon.cola.platform.idp.admin.identity.domain.vo.IdentityUserVO;
import top.egon.cola.platform.idp.admin.identity.service.IdentityUserService;
import top.egon.cola.platform.idp.admin.support.security.IdpAdminAuthorizationPort;
import top.egon.cola.platform.idp.admin.support.security.IdpSecurityConfig;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IdentityUserController.class)
@Import(IdpSecurityConfig.class)
class IdpAdminSecurityIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private IdentityUserService users;

    @MockitoBean
    private IdpAdminAuthorizationPort authorization;

    @Test
    void rejectsUnauthenticatedAdminCalls() throws Exception {
        mockMvc.perform(get("/api/v1/identity/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedMutationRequiresRbac3Permission() throws Exception {
        when(users.create(any())).thenReturn(new CreatedIdentityUserVO(
                "1001",
                "alice",
                "Alice",
                "ACTIVE",
                "one-time-password"
        ));

        mockMvc.perform(post("/api/v1/identity/users")
                        .with(identityJwt())
                        .contentType("application/json")
                        .content("""
                                {"username":"alice","displayName":"Alice"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subject").value("1001"));

        verify(authorization).require(
                any(IdentityPrincipal.class),
                eq("idp:identity-user:create")
        );
    }

    @Test
    void permissionDenialReturnsForbidden() throws Exception {
        doThrow(new AccessDeniedException("denied"))
                .when(authorization)
                .require(any(), eq("idp:identity-user:read"));

        mockMvc.perform(get("/api/v1/identity/users").with(identityJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void bindsIdentitySubjectForUserUpdateWithoutCompilerParameterMetadata()
            throws Exception {
        when(users.update(eq("1001"), any())).thenReturn(
                new IdentityUserVO(
                        "1001", "alice", "Alice", "DISABLED",
                        1L, 0, null, null, 2L
                )
        );

        mockMvc.perform(patch("/api/v1/identity/users/1001")
                        .with(identityJwt())
                        .contentType("application/json")
                        .content("""
                                {
                                  "displayName":"Alice",
                                  "status":"DISABLED",
                                  "expectedVersion":1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("1001"))
                .andExpect(jsonPath("$.status").value("DISABLED"));

        verify(authorization).require(
                any(IdentityPrincipal.class),
                eq("idp:identity-user:update")
        );
    }

    @Test
    void domainConflictReturnsSafeConflict() throws Exception {
        when(users.create(any())).thenThrow(new IllegalStateException(
                "identity username already exists"
        ));

        mockMvc.perform(post("/api/v1/identity/users")
                        .with(identityJwt())
                        .contentType("application/json")
                        .content("""
                                {"username":"alice","displayName":"Alice"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message")
                        .value("request conflicts with current state"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor identityJwt() {
        return authentication(new top.egon.cola.platform.idp.admin.support.security
                .IdpAdminAuthenticationToken(new IdentityPrincipal(
                "admin-sub",
                "tenant-a",
                "session-a",
                "idp-admin-web",
                "token-a",
                3L,
                java.util.Set.of("idp-admin"),
                Instant.parse("2026-08-02T00:00:00Z"),
                Instant.parse("2026-08-02T00:15:00Z")
        ), "raw-token"));
    }
}
