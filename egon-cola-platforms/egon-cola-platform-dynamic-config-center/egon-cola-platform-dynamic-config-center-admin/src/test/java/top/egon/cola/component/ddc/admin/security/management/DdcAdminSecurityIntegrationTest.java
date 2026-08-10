package top.egon.cola.component.ddc.admin.security.management;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.ddc.admin.controller.config.DdcCacheController;
import top.egon.cola.component.ddc.admin.controller.config.DdcConfigController;
import top.egon.cola.component.ddc.admin.controller.config.DdcPublishTaskController;
import top.egon.cola.component.ddc.admin.model.dto.DdcConfigCreateRequest;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.ddc.admin.service.cache.DdcCacheService;
import top.egon.cola.component.ddc.admin.service.config.DdcConfigService;
import top.egon.cola.component.ddc.admin.service.publish.DdcPublishService;
import top.egon.cola.component.ddc.admin.service.publish.DdcPublishTaskQueryService;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;
import top.egon.cola.platform.rbac3.starter.security.Rbac3AuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        DdcConfigController.class,
        DdcPublishTaskController.class,
        DdcCacheController.class,
        DdcAdminSecurityIntegrationTest.HealthInfoController.class,
        DdcAdminSecurityIntegrationTest.RegistryInfoController.class,
        DdcAdminSecurityIntegrationTest.BindingInfoController.class
})
@Import(DdcAdminSecurityConfiguration.class)
@TestPropertySource(properties = {
        "egon.cola.component.ddc.admin.security.local-dev=true"
})
class DdcAdminSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @MockBean
    private DdcConfigService configService;

    @MockBean
    private DdcPublishService publishService;

    @MockBean
    private DdcPublishTaskQueryService publishTaskQueryService;

    @MockBean
    private DdcPublishTaskRepository publishTaskRepository;

    @MockBean
    private DdcCacheService cacheService;

    @Test
    void permitsOnlyDeclaredAnonymousEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/ddc/configs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("DDC_ADMIN_AUTHENTICATION_REQUIRED"));
    }

    @Test
    void protectsRegistryAdminReadsWithReadCapability() throws Exception {
        mockMvc.perform(get("/api/v1/ddc/registry/services"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/ddc/registry/services")
                        .with(authority("CAP_DDC_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void protectsNamespaceBindingsWithReadAndWriteCapabilities()
            throws Exception {
        mockMvc.perform(get("/api/v1/ddc/namespace-env-app-bindings"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/ddc/namespace-env-app-bindings")
                        .with(authority("CAP_DDC_READ")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/ddc/namespace-env-app-bindings")
                        .with(authority("CAP_DDC_READ")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/ddc/namespace-env-app-bindings")
                        .with(authority("CAP_DDC_WRITE")))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsDdcAdminPageAfterWebExtraction() throws Exception {
        mockMvc.perform(get("/ddc-admin/index.html"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void machineOpenApiIsNotMapped() {
        assertThat(handlerMapping.getHandlerMethods().keySet())
                .flatExtracting(mapping -> mapping.getPatternValues())
                .noneMatch(path -> path.startsWith(
                        "/api/v1/ddc/" + "openapi"
                ));
    }

    @Test
    void enforcesReadWriteAndWildcardCapabilities() throws Exception {
        when(configService.list(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/ddc/configs")
                        .with(authority("CAP_DDC_READ")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/ddc/configs")
                        .with(authority("CAP_DDC_READ"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configBody()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/ddc/configs")
                        .with(authority("CAP_DDC_WRITE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configBody()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/ddc/configs")
                        .with(authority("CAP_*"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configBody()))
                .andExpect(status().isOk());
    }

    @Test
    void tokenAuthorizationClaimsDoNotGrantDdcPermission() throws Exception {
        mockMvc.perform(get("/api/v1/ddc/configs")
                        .with(jwt().jwt(token -> token.subject("limited-user")
                                .claim("roles", List.of("ADMIN"))
                                .claim("capabilities", List.of("DDC_READ")))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rbac3SnapshotGrantsDdcPermission() throws Exception {
        when(configService.list(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/ddc/configs")
                        .with(authentication(rbac3("DDC_READ"))))
                .andExpect(status().isOk());
    }

    @Test
    void rbac3PrincipalUsesTheStableIdentitySubjectAsOperator() throws Exception {
        mockMvc.perform(post("/api/v1/ddc/configs")
                        .with(authentication(rbac3("DDC_WRITE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configBody()))
                .andExpect(status().isOk());

        ArgumentCaptor<String> operator = ArgumentCaptor.forClass(String.class);
        verify(configService).create(any(DdcConfigCreateRequest.class), operator.capture());
        assertThat(operator.getValue()).isEqualTo("user:admin-sub [requested=system]");
    }

    @Test
    void reservesPublishAndCacheOperationsForExactCapabilities()
            throws Exception {
        mockMvc.perform(post("/api/v1/ddc/configs/config-1/publish")
                        .with(authority("CAP_DDC_WRITE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(
                        "/api/v1/ddc/publish-tasks/change-1/retry"
                ).with(authority("CAP_DDC_READ")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/ddc/cache/check")
                        .param("appCode", "app-a")
                        .param("env", "dev")
                        .param("bizCode", "biz-a")
                        .with(authority("CAP_DDC_READ")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/ddc/cache/check")
                        .param("appCode", "app-a")
                        .param("env", "dev")
                        .param("bizCode", "biz-a")
                        .with(authority("CAP_DDC_CACHE")))
                .andExpect(status().isOk());
    }

    @Test
    void deniesAuthenticatedRequestsOutsideDeclaredRoutes()
            throws Exception {
        mockMvc.perform(get("/api/v1/ddc/unknown")
                        .with(authority("CAP_*")))
                .andExpect(status().isForbidden());
    }

    @Test
    void usesJwtSubjectAsTrustedOperator() throws Exception {
        mockMvc.perform(post("/api/v1/ddc/configs")
                        .param("operator", "claimed-user")
                        .with(jwt()
                                .jwt(token -> token.subject("admin-42"))
                                .authorities(new SimpleGrantedAuthority(
                                        "CAP_DDC_WRITE"
                                )))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configBody()))
                .andExpect(status().isOk());

        ArgumentCaptor<String> operator =
                ArgumentCaptor.forClass(String.class);
        verify(configService).create(
                any(DdcConfigCreateRequest.class),
                operator.capture()
        );
        assertThat(operator.getValue()).isEqualTo(
                "user:admin-42 [requested=claimed-user]"
        );
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor
    authority(String value) {
        return jwt().jwt(token -> token.subject("test-user"))
                .authorities(new SimpleGrantedAuthority(value));
    }

    private Rbac3AuthenticationToken rbac3(String permission) {
        Instant now = Instant.parse("2026-08-02T04:00:00Z");
        IdentityPrincipal identity = new IdentityPrincipal(
                "admin-sub", "tenant-a", "sid-1", "ddc-admin-web",
                "token-1", 2, java.util.Set.of("ddc-admin-web"),
                now, now.plusSeconds(900));
        SystemAuthorizationSnapshot snapshot = new SystemAuthorizationSnapshot(
                "tenant-a", "admin-sub", "101", "sid-1", "ddc-admin",
                3, 4, 5, List.of("ddc-reader"), java.util.Set.of(permission),
                Map.of(), Map.of(), "sha256:ddc", now, now.plusSeconds(900));
        return new Rbac3AuthenticationToken(
                new AuthorizationService.RuntimeAuthorizationContext(
                        identity, snapshot, false));
    }

    private String configBody() {
        return """
                {
                  "bizCode":"biz-a",
                  "appCode":"app-a",
                  "env":"dev",
                  "resourceName":"application.yml",
                  "content":"feature:\\n  enabled: true\\n",
                  "format":"YAML"
                }
                """;
    }

    @RestController
    static class HealthInfoController {

        @GetMapping({
                "/actuator/health",
                "/actuator/health/readiness",
                "/actuator/info"
        })
        Map<String, String> status() {
            return Map.of("status", "UP");
        }
    }

    @RestController
    @RequestMapping("/api/v1/ddc/registry")
    static class RegistryInfoController {

        @GetMapping("/services")
        Map<String, Object> services() {
            return Map.of("services", List.of());
        }
    }

    @RestController
    @RequestMapping("/api/v1/ddc/namespace-env-app-bindings")
    static class BindingInfoController {

        @GetMapping
        Map<String, Object> bindings() {
            return Map.of("bindings", List.of());
        }

        @PostMapping
        Map<String, String> createBinding() {
            return Map.of("status", "created");
        }
    }
}
