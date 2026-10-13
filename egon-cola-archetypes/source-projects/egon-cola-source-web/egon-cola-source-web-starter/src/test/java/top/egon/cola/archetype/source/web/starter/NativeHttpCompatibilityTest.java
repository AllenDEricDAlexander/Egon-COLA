package top.egon.cola.archetype.source.web.starter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.archetype.source.web.application.user.manage.UserManage;
import top.egon.cola.archetype.source.web.application.user.pojo.result.UserDetailResult;
import java.util.List;
import java.util.Set;
import org.springframework.context.annotation.Import;
import top.egon.cola.archetype.source.web.starter.config.NativeSecurityContractTest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(NativeSecurityContractTest.SecurityTestBeans.class)
@SpringBootTest(classes = OrganizationApplication.class)
class NativeHttpCompatibilityTest extends top.egon.cola.archetype.source.web.support.PersistenceTestSupport {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private UserManage userManage;
    @Autowired
    private NativeSecurityContractTest.SecurityFixture security;

    @Test
    void retains_created_status_location_and_response_fields_with_verified_identity() throws Exception {
        when(userManage.createUser(any())).thenReturn(new UserDetailResult(1001L, "Mario", "mario@example.com", "ACTIVE", List.of("STUDENT")));
        mockMvc.perform(post("/api/v1/users").header("Authorization", "Bearer " + security.token("1001", Set.of(NativeSecurityContractTest.SecurityFixture.SYSTEM + ":user:create"))).header("Idempotency-Key", "native-http-check")
                        .contentType("application/json").content("{\"name\":\"Mario\",\"email\":\"mario@example.com\"}"))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/v1/users/1001"))
                .andExpect(jsonPath("$.id").value("1001")).andExpect(jsonPath("$.roleCodes[0]").value("STUDENT"));
        mockMvc.perform(post("/api/v1/users").header("Authorization", "Bearer " + security.token("1001", Set.of(NativeSecurityContractTest.SecurityFixture.SYSTEM + ":user:create"))).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void disabled_document_governance_does_not_make_documents_public() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
    }

    @org.junit.jupiter.api.Nested
    @SpringBootTest(classes = OrganizationApplication.class, properties = {
            "egon.cola.component.yuheng.openapi.enabled=true",
            "egon.cola.component.yuheng.openapi.publish-to-tianshu=false",
            "egon.cola.component.yuheng.openapi.published-groups[0]=public",
            "egon.cola.component.yuheng.openapi.biz-code=student-management",
            "egon.cola.component.yuheng.openapi.application-code=egon-cola-source-web",
            "egon.cola.component.yuheng.openapi.resource-uri=https://organization.example.test",
            "egon.cola.component.yuheng.openapi.artifact-version=1.0.0",
            "egon.cola.component.yuheng.openapi.build-id=contract-test",
            "springdoc.group-configs[0].group=public",
            "springdoc.group-configs[0].paths-to-match=/api/**"})
    class GovernedDocuments {
        @Autowired private MockMvc documents;
        @Autowired private NativeSecurityContractTest.SecurityFixture documentSecurity;

        @Test
        void real_generated_oas_retains_business_info_paths_and_adds_security_contract() throws Exception {
            documents.perform(get("/v3/api-docs/public")).andExpect(status().isUnauthorized());
            documents.perform(get("/v3/api-docs/public").header("Authorization", "Bearer " + documentSecurity.serviceToken("1001", Set.of("unrelated"))))
                    .andExpect(status().isForbidden());
            documents.perform(get("/v3/api-docs/public").header("Authorization", "Bearer " + documentSecurity.serviceToken("1001", Set.of("yuheng.openapi.read"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.info.title").value("Student Management Organization API"))
                    .andExpect(jsonPath("$.info.version").value("v1"))
                    .andExpect(jsonPath("$.paths['/api/v1/users'].post.operationId").value("nativeWebUserCreateV1"))
                    .andExpect(jsonPath("$.paths['/api/v1/grades'].post.operationId").value("nativeWebGradeCreateV1"))
                    .andExpect(jsonPath("$.components.securitySchemes.nativeBearer.scheme").value("bearer"))
                    .andExpect(jsonPath("$.components.schemas.NativeSecurityError.required.length()").value(2))
                    .andExpect(jsonPath("$.paths['/api/v1/users'].post.responses['401'].headers['WWW-Authenticate']").exists());
        }
    }
}
