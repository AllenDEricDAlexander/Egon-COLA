package top.egon.cola.archetype.source.agent.starter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import top.egon.cola.archetype.source.agent.adapter.filter.ResearchApiKeyFilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = DeepResearchApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(DeepResearchApplicationTest.FakeAgentDependencies.class)
class DeepResearchOpenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publishes_the_single_sse_command_security_and_error_contract() throws Exception {
        mockMvc.perform(get("/v3/api-docs")
                        .header(ResearchApiKeyFilter.API_KEY_HEADER, "test-research-key"))
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
                .andExpect(jsonPath("$.components.securitySchemes.researchApiKey.type").value("apiKey"))
                .andExpect(jsonPath("$.components.securitySchemes.researchApiKey.name")
                        .value("X-Research-Api-Key"))
                .andExpect(jsonPath("$.components.schemas.DeepResearchErrorResponse").exists());
    }
}
