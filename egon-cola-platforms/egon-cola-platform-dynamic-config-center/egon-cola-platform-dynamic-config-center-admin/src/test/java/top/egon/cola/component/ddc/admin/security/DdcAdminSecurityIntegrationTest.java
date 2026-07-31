package top.egon.cola.component.ddc.admin.security;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.ddc.admin.controller.DdcCacheController;
import top.egon.cola.component.ddc.admin.controller.DdcConfigController;
import top.egon.cola.component.ddc.admin.controller.DdcManifestController;
import top.egon.cola.component.ddc.admin.controller.DdcOpenApiController;
import top.egon.cola.component.ddc.admin.controller.DdcPublishTaskController;
import top.egon.cola.component.ddc.admin.model.dto.DdcConfigCreateRequest;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.ddc.admin.service.DdcCacheService;
import top.egon.cola.component.ddc.admin.service.DdcConfigService;
import top.egon.cola.component.ddc.admin.service.DdcInstanceAdminService;
import top.egon.cola.component.ddc.admin.service.DdcPublishService;
import top.egon.cola.component.ddc.security.DdcCanonicalRequest;
import top.egon.cola.component.ddc.security.DdcRequestSigner;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        DdcManifestController.class,
        DdcConfigController.class,
        DdcPublishTaskController.class,
        DdcCacheController.class,
        DdcOpenApiController.class,
        DdcAdminSecurityIntegrationTest.HealthInfoController.class,
        DdcAdminSecurityIntegrationTest.RegistryInfoController.class
})
@Import({
        DdcAdminSecurityConfiguration.class,
        DdcSecurityFilterRegistration.class,
        DdcAdminSecurityIntegrationTest.NonceConfiguration.class
})
@TestPropertySource(properties = {
        "egon.cola.component.ddc.admin.manifest.version=5.2.3-test",
        "egon.cola.component.ddc.admin.security.local-dev=true",
        "egon.cola.component.ddc.admin.openapi.signature-enabled=true",
        "egon.cola.component.ddc.admin.openapi.credentials[0].credential-id=sdk-a",
        "egon.cola.component.ddc.admin.openapi.credentials[0].access-key=sdk-access",
        "egon.cola.component.ddc.admin.openapi.credentials[0].secret=sdk-secret",
        "egon.cola.component.ddc.admin.openapi.credentials[0].client-type=SDK",
        "egon.cola.component.ddc.admin.openapi.credentials[0].app-code-patterns[0]=app-a",
        "egon.cola.component.ddc.admin.openapi.credentials[0].env-patterns[0]=dev",
        "egon.cola.component.ddc.admin.openapi.credentials[0].namespace-patterns[0]=ns-a",
        "egon.cola.component.ddc.admin.openapi.credentials[0].allowed-operations[0]=CONFIG_PULL"
})
class DdcAdminSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DdcConfigService configService;

    @MockBean
    private DdcPublishService publishService;

    @MockBean
    private DdcInstanceAdminService instanceAdminService;

    @MockBean
    private DdcPublishTaskRepository publishTaskRepository;

    @MockBean
    private DdcCacheService cacheService;

    @Test
    void permitsOnlyDeclaredAnonymousEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/ddc/manifest"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health"))
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
    void rejectsDdcAdminPageAfterWebExtraction() throws Exception {
        mockMvc.perform(get("/ddc-admin/index.html"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void acceptsValidHmacOpenApiRequestWithoutJwt() throws Exception {
        when(configService.pull("app-a", "dev", "ns-a"))
                .thenReturn(List.of());

        mockMvc.perform(signedPull())
                .andExpect(status().isOk());
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
                        .param("namespace", "ns-a")
                        .with(authority("CAP_DDC_READ")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/ddc/cache/check")
                        .param("appCode", "app-a")
                        .param("env", "dev")
                        .param("namespace", "ns-a")
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

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
    signedPull() {
        long timestamp = Instant.now().toEpochMilli();
        String path = "/api/v1/ddc/openapi/configs/pull";
        Map<String, List<String>> query = Map.of(
                "appCode", List.of("app-a"),
                "env", List.of("dev"),
                "namespace", List.of("ns-a")
        );
        DdcCanonicalRequest canonical = new DdcCanonicalRequest(
                "GET",
                path,
                query,
                timestamp,
                "nonce-security-integration",
                new byte[0]
        );
        DdcRequestSigner signer = new DdcRequestSigner();
        return get(path)
                .param("appCode", "app-a")
                .param("env", "dev")
                .param("namespace", "ns-a")
                .header(DdcRequestSigner.ACCESS_KEY_HEADER, "sdk-access")
                .header(
                        DdcRequestSigner.TIMESTAMP_HEADER,
                        Long.toString(timestamp)
                )
                .header(
                        DdcRequestSigner.NONCE_HEADER,
                        "nonce-security-integration"
                )
                .header(
                        DdcRequestSigner.CONTENT_SHA256_HEADER,
                        canonical.contentSha256()
                )
                .header(
                        DdcRequestSigner.SIGNATURE_HEADER,
                        signer.sign(canonical, "sdk-secret")
                );
    }

    private String configBody() {
        return """
                {
                  "appCode":"app-a",
                  "env":"dev",
                  "namespace":"ns-a",
                  "configKey":"feature.enabled",
                  "configValue":"true",
                  "valueType":"BOOLEAN"
                }
                """;
    }

    @RestController
    static class HealthInfoController {

        @GetMapping({"/actuator/health", "/actuator/info"})
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

    @TestConfiguration(proxyBeanMethods = false)
    static class NonceConfiguration {

        @Bean
        DdcNonceStore ddcNonceStore() {
            return new InMemoryDdcNonceStore(100);
        }
    }
}
