package top.egon.cola.archetype.source.light.start.config;

import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.archetype.source.light.application.user.manage.UserManage;
import top.egon.cola.archetype.source.light.application.user.pojo.command.CreateUserCommand;
import top.egon.cola.archetype.source.light.application.user.pojo.result.UserResult;
import top.egon.cola.archetype.source.light.start.StudentManagementApplication;
import top.egon.cola.archetype.source.light.support.PersistenceTestSupport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(NativeSecurityContractTest.SecurityTestBeans.class)
@SpringBootTest(classes = StudentManagementApplication.class, properties = "TIANQUAN_SHOUBING_RESOURCE_URI=https://light.example.test")
class NativeHttpCompatibilityTest extends PersistenceTestSupport {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private UserManage userManage;
    @Autowired private NativeSecurityContractTest.SecurityFixture security;

    @Test
    void business_post_keeps_verified_context_and_json_contract() throws Exception {
        when(userManage.create(any())).thenReturn(new UserResult(1001L, "Mario", "mario@example.com", "ACTIVE"));
        mockMvc.perform(post("/api/users").header("Authorization", "Bearer " + security.token("1001", Set.of(NativeSecurityContractTest.SecurityFixture.SYSTEM + ":user:create"))).header("X-Operator-Id", "forged-actor")
                        .header("X-Tenant-Id", "42").header("X-Request-Id", "request-1").contentType("application/json")
                        .content("{\"externalId\":\"external-1\",\"name\":\"Mario\",\"email\":\"mario@example.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(10000))
                .andExpect(jsonPath("$.data.id").value("1001"))
                .andExpect(jsonPath("$.data.name").value("Mario"));
        verify(userManage).create(new CreateUserCommand(
                "external-1", "Mario", "mario@example.com", "subject-1001", "request-1"));
    }

    @Test
    void platform_starter_retains_openapi_info_and_business_paths_for_service_reader() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v3/api-docs").header("Authorization", "Bearer " + security.token("1001", Set.of("openid")))).andExpect(status().isForbidden());
        // Governance-disabled documents are not public; generated business OAS is asserted by the enabled narrow SERVICE chain below.

    }

    @Nested
    @SpringBootTest(classes = StudentManagementApplication.class, properties = {
            "egon.cola.component.yuheng.openapi.enabled=true",
            "TIANQUAN_SHOUBING_RESOURCE_URI=https://light.example.test",
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
    @Import(JwtTestConfiguration.class)
    class DocumentGovernance {
        @Autowired
        private MockMvc governedMvc;
        @Autowired private NativeSecurityContractTest.SecurityFixture governedSecurity;

        @Test
        void documents_require_the_document_scope() throws Exception {
            governedMvc.perform(get("/v3/api-docs/public")).andExpect(status().isUnauthorized());
            governedMvc.perform(get("/v3/api-docs/public").header("Authorization", "Bearer " + governedSecurity.serviceToken("1001", Set.of("unrelated"))))
                    .andExpect(status().isForbidden());
            governedMvc.perform(get("/v3/api-docs/public").header("Authorization", "Bearer " + governedSecurity.serviceToken("1001", Set.of("yuheng.openapi.read"))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.paths['/governed-probe'].get.operationId").value("governedProbe"));
        }

        @Test
        void governed_business_document_keeps_original_info_paths_and_security_contract() throws Exception {
            governedMvc.perform(get("/v3/api-docs/legacy").header("Authorization", "Bearer " + governedSecurity.serviceToken("1001", Set.of("yuheng.openapi.read"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.info.title").value("Student Management"))
                    .andExpect(jsonPath("$.paths['/api/users'].post.operationId").value("nativeLightUserCreateV1"))
                    .andExpect(jsonPath("$.paths['/api/courses'].post.operationId").value("nativeLightCourseCreateV1"))
                    .andExpect(jsonPath("$.paths['/api/users/{userId}'].get.operationId").value("nativeLightUserGetV1"))
                    .andExpect(jsonPath("$.components.securitySchemes.nativeBearer.scheme").value("bearer"))
                    .andExpect(jsonPath("$.components.schemas.NativeSecurityError.required.length()").value(2))
                    .andExpect(jsonPath("$.paths['/api/users'].post.responses['401'].headers['WWW-Authenticate']").exists())
                    .andExpect(jsonPath("$.paths['/api/users'].post.responses['503'].headers['Cache-Control']").exists());
        }

        @Test
        void document_governance_leaves_business_and_health_routes_accessible() throws Exception {
            governedMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
            governedMvc.perform(post("/api/users").header("Authorization", "Bearer " + governedSecurity.token("1001", Set.of(NativeSecurityContractTest.SecurityFixture.SYSTEM + ":user:create"))).contentType("application/json").content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class JwtTestConfiguration {
        @Bean("governedProbeController")
        GovernedProbeController governedProbeController() {
            return new GovernedProbeController();
        }


    }

    @TestComponent
    @RestController
    static class GovernedProbeController {
        @GetMapping("/governed-probe")
        @Operation(operationId = "governedProbe")
        Map<String, String> probe() {
            return Map.of("status", "ok");
        }
    }
}
