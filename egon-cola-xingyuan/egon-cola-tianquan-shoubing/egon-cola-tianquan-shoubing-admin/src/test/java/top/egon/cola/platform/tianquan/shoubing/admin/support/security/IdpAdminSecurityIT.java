package top.egon.cola.platform.tianquan.shoubing.admin.support.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.FilterType;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.controller.IdentityUserController;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.vo.CreatedIdentityUserVO;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.vo.IdentityUserVO;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.service.IdentityUserService;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller.filter.TenantLoginAuthenticationFilter;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.controller.ResourceServerController;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.service.ResourceServerService;
import top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;

import java.time.Instant;
import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = {IdentityUserController.class, ResourceServerController.class}, excludeFilters =
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TenantLoginAuthenticationFilter.class))
@Import(IdpSecurityConfig.class)
class IdpAdminSecurityIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private IdentityUserService users;

    @MockitoBean
    @org.springframework.beans.factory.annotation.Qualifier("resourceServerServiceImpl")
    private ResourceServerService resources;

    @MockitoBean
    @org.springframework.beans.factory.annotation.Qualifier("idpAdminAuthorizationPort")
    private IdpAdminAuthorizationPort authorization;

    @MockitoBean
    @org.springframework.beans.factory.annotation.Qualifier("oauthManagementRealmResolver")
    private OAuthManagementRealmResolver realms;

    @org.junit.jupiter.api.BeforeEach
    void resolveTheGlobalResourceDirectoryToThePlatformRealm() {
        when(realms.resolve(any(IdentityPrincipal.class), nullable(String.class))).thenReturn(
                TrustedRealmBO.builder()
                        .issuer(URI.create("https://idp.example.test/platform"))
                        .realmKey("platform")
                        .realmType(RealmTypeEnum.PLATFORM)
                        .storagePartitionId(1L)
                        .trustVersion(1L)
                        .build());
    }

    @Test
    void rejectsUnauthenticatedAdminCalls() throws Exception {
        mockMvc.perform(get("/api/v1/tianquan-shoubing/users"))
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

        mockMvc.perform(post("/api/v1/tianquan-shoubing/users")
                        .with(identityJwt())
                        .contentType("application/json")
                        .content("""
                                {"username":"alice","displayName":"Alice"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subject").value("1001"));

        verify(authorization).require(
                any(IdentityPrincipal.class),
                eq("tianquan-shoubing:identity-user:create")
        );
    }

    @Test
    void resourceMutationRequiresDedicatedRbac3Permission() throws Exception {
        mockMvc.perform(post(
                        "/api/v1/tianquan-shoubing/resource-servers/{resourceServerId}/enable",
                        "permission-tianquan-shoubing-prod"
                ).with(identityJwt())
                        .contentType("application/json")
                        .content("{\"expectedVersion\":0}"))
                .andExpect(status().isOk());

        verify(authorization).require(
                any(IdentityPrincipal.class),
                eq("tianquan-shoubing:resource-server:status")
        );
        verify(resources).enable(any(TrustedRealmBO.class), eq("permission-tianquan-shoubing-prod"), any());
    }

    @Test
    void permissionDenialReturnsForbidden() throws Exception {
        doThrow(new AccessDeniedException("denied"))
                .when(authorization)
                .require(any(), eq("tianquan-shoubing:identity-user:read"));

        mockMvc.perform(get("/api/v1/tianquan-shoubing/users").with(identityJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void bindsIdentitySubjectForUserUpdateWithoutCompilerParameterMetadata()
            throws Exception {
        when(users.update(eq("1001"), any())).thenReturn(
                new IdentityUserVO(
                        "1001", "alice", "Alice", "DISABLED",
                        0, null, null, 2L
                )
        );

        mockMvc.perform(patch("/api/v1/tianquan-shoubing/users/1001")
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
                eq("tianquan-shoubing:identity-user:update")
        );
    }

    @Test
    void domainConflictReturnsSafeConflict() throws Exception {
        when(users.create(any())).thenThrow(new IllegalStateException(
                "identity username already exists"
        ));

        mockMvc.perform(post("/api/v1/tianquan-shoubing/users")
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
        return authentication(new top.egon.cola.platform.tianquan.shoubing.admin.support.security
                .IdpAdminAuthenticationToken(new IdentityPrincipal(
                "admin-sub",
                "tenant-a",
                "token-a",
                java.util.Set.of("tianquan-shoubing-admin"),
                Instant.parse("2026-08-02T00:00:00Z"),
                Instant.parse("2026-08-02T00:15:00Z"),
                AuthenticationContext.password()
        ), "raw-token"));
    }
}
