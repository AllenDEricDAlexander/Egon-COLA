package top.egon.cola.archetype.source.light.start.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.archetype.source.light.application.user.manage.UserManage;
import top.egon.cola.archetype.source.light.application.user.result.UserResult;
import top.egon.cola.archetype.source.light.start.StudentManagementApplication;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(classes = StudentManagementApplication.class)
class NativeHttpCompatibilityTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private UserManage userManage;

    @Test
    void business_post_keeps_public_access_request_context_and_json_contract() throws Exception {
        when(userManage.create(any())).thenReturn(new UserResult(1001L, "Mario", "mario@example.com", "ACTIVE"));
        mockMvc.perform(post("/api/users").header("X-Operator-Id", "operator-1")
                        .header("X-Request-Id", "request-1").contentType("application/json")
                        .content("{\"externalId\":\"external-1\",\"name\":\"Mario\",\"email\":\"mario@example.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.id").value("1001"))
                .andExpect(jsonPath("$.data.name").value("Mario"));
        verify(userManage).create(new top.egon.cola.archetype.source.light.application.user.command.CreateUserCommand(
                "external-1", "Mario", "mario@example.com", "operator-1", "request-1"));
    }

    @Test
    void platform_starter_retains_openapi_info_and_business_paths() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Student Management"))
                .andExpect(jsonPath("$.paths['/api/users'].post").exists())
                .andExpect(jsonPath("$.paths['/api/courses'].post").exists())
                .andExpect(jsonPath("$.paths['/api/users/{userId}'].get").exists());
    }

    @org.junit.jupiter.api.Nested
    @SpringBootTest(classes = StudentManagementApplication.class, properties = {
            "egon.cola.component.yuheng.openapi.enabled=true",
            "egon.cola.component.yuheng.openapi.publish-to-tianshu=false",
            "egon.cola.component.yuheng.openapi.published-groups[0]=public",
            "egon.cola.component.yuheng.openapi.biz-code=student-management",
            "egon.cola.component.yuheng.openapi.application-code=light",
            "egon.cola.component.yuheng.openapi.resource-uri=https://light.example.test",
            "egon.cola.component.yuheng.openapi.artifact-version=1.0.0",
            "egon.cola.component.yuheng.openapi.build-id=contract-test",
            "springdoc.group-configs[0].group=public",
            "springdoc.group-configs[0].paths-to-match=/governed-probe",
            "egon.cola.component.yuheng.openapi.published-groups[1]=legacy",
            "springdoc.group-configs[1].group=legacy",
            "springdoc.group-configs[1].paths-to-match=/api/**"
    })
    @org.springframework.context.annotation.Import(JwtTestConfiguration.class)
    class DocumentGovernance {
        @Autowired
        private MockMvc governedMvc;

        @Test
        void documents_require_the_document_scope() throws Exception {
            governedMvc.perform(get("/v3/api-docs/public")).andExpect(status().isUnauthorized());
            governedMvc.perform(get("/v3/api-docs/public").header("Authorization", "Bearer other-token"))
                    .andExpect(status().isForbidden());
            governedMvc.perform(get("/v3/api-docs/public").header("Authorization", "Bearer docs-token"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.paths['/governed-probe'].get.operationId").value("governedProbe"));
        }

        @Test
        void uncatalogued_operations_require_explicit_operation_ids_before_publication() throws Exception {
            governedMvc.perform(get("/v3/api-docs/legacy").header("Authorization", "Bearer docs-token"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("catalogued OpenAPI operationId is required"));
        }

        @Test
        void document_governance_leaves_business_and_health_routes_accessible() throws Exception {
            governedMvc.perform(get("/actuator/health")).andExpect(status().isOk());
            governedMvc.perform(post("/api/users").contentType("application/json").content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    static class JwtTestConfiguration {
        @org.springframework.context.annotation.Bean("governedProbeController")
        GovernedProbeController governedProbeController() {
            return new GovernedProbeController();
        }

        @org.springframework.context.annotation.Bean("testJwtDecoder")
        org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder() {
            return token -> org.springframework.security.oauth2.jwt.Jwt.withTokenValue(token)
                    .header("alg", "none").subject("test-reader")
                    .issuedAt(java.time.Instant.now()).expiresAt(java.time.Instant.now().plusSeconds(60))
                    .claim("scope", "docs-token".equals(token) ? "yuheng.openapi.read" : "unrelated")
                    .build();
        }
    }

    @org.springframework.boot.test.context.TestComponent
    @org.springframework.web.bind.annotation.RestController
    static class GovernedProbeController {
        @org.springframework.web.bind.annotation.GetMapping("/governed-probe")
        @io.swagger.v3.oas.annotations.Operation(operationId = "governedProbe")
        java.util.Map<String, String> probe() {
            return java.util.Map.of("status", "ok");
        }
    }
}
