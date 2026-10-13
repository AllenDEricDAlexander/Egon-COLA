package top.egon.cola.archetype.source.web.starter;

import org.springframework.context.annotation.Import;
import top.egon.cola.archetype.source.web.starter.config.NativeSecurityContractTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(NativeSecurityContractTest.SecurityTestBeans.class)
@SpringBootTest(
        classes = OrganizationApplication.class,
        properties = {"spring.profiles.active=test", "egon.cola.component.yuheng.openapi.enabled=true",
                "egon.cola.component.yuheng.openapi.publish-to-tianshu=false",
                "egon.cola.component.yuheng.openapi.published-groups[0]=public",
                "egon.cola.component.yuheng.openapi.biz-code=student-management",
                "egon.cola.component.yuheng.openapi.application-code=egon-cola-source-web",
                "egon.cola.component.yuheng.openapi.resource-uri=https://organization.example.test",
                "egon.cola.component.yuheng.openapi.artifact-version=1.0.0",
                "egon.cola.component.yuheng.openapi.build-id=contract-test",
                "springdoc.group-configs[0].group=public", "springdoc.group-configs[0].paths-to-match=/api/**"})
@AutoConfigureMockMvc
class OrganizationOpenApiTest extends top.egon.cola.archetype.source.web.support.PersistenceTestSupport {
    @Autowired
    private NativeSecurityContractTest.SecurityFixture security;


    @Autowired
    private MockMvc mockMvc;

    @Test
    void publishesBothDomainOperations() throws Exception {
        mockMvc.perform(get("/v3/api-docs/public").header("Authorization", "Bearer " + security.serviceToken("1001", java.util.Set.of("yuheng.openapi.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Student Management Organization API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.paths['/api/v1/users']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/grades']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/school-classes']").exists());
    }
}
