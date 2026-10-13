package top.egon.cola.archetype.source.agent.starter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import top.egon.cola.archetype.source.agent.starter.config.NativeSecurityContractTest;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = DeepResearchApplication.class, properties = {
        "springdoc.api-docs.enabled=true",
        "egon.cola.component.yuheng.openapi.enabled=true",
        "egon.cola.component.yuheng.openapi.publish-to-tianshu=false",
        "egon.cola.component.yuheng.openapi.published-groups[0]=public",
        "egon.cola.component.yuheng.openapi.biz-code=deep-research",
        "egon.cola.component.yuheng.openapi.application-code=egon-cola-source-agent",
        "egon.cola.component.yuheng.openapi.resource-uri=https://agent.example.test",
        "egon.cola.component.yuheng.openapi.artifact-version=1.0.0",
        "egon.cola.component.yuheng.openapi.build-id=contract-test",
        "springdoc.group-configs[0].group=public",
        "springdoc.group-configs[0].paths-to-match=/api/**"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(DeepResearchApplicationTest.FakeAgentDependencies.class)
class DeepResearchOpenApiTest extends PersistenceTestSupport {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private NativeSecurityContractTest.SecurityFixture security;
    @Autowired
    private org.springframework.context.ApplicationContext context;

    @Test
    void publishes_the_single_sse_command_security_and_error_contract() throws Exception {
        org.junit.jupiter.api.Assertions.assertTrue(context.containsBean("gatewayOpenApiWebMvcSecurityFilterChain"), "Narrow SERVICE document chain is required");
        mockMvc.perform(get("/v3/api-docs/public")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v3/api-docs/public").header("Authorization", "Bearer " + security.serviceToken("1001", Set.of("unrelated"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/v3/api-docs/public").header("Authorization", "Bearer " + security.token("1001", Set.of("yuheng.openapi.read"))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v3/api-docs/public")
                        .header("Authorization", "Bearer " + security.serviceToken("1001", Set.of("yuheng.openapi.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Deep Research Agent API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.paths['/api/v1/deep-research/runs'].post.operationId")
                        .value("startDeepResearch"))
                .andExpect(jsonPath("$.paths['/api/v1/deep-research/runs'].post.requestBody.content['application/json']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/deep-research/runs'].post.responses['200'].content['text/event-stream']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/deep-research/runs'].post.responses['429']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.nativeBearer.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.nativeBearer.scheme")
                        .value("bearer"))
                .andExpect(jsonPath("$.components.schemas.DeepResearchErrorResponse").exists());
    }
}
