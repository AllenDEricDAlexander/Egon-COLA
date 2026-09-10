package top.egon.cola.archetype.source.agent.starter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import top.egon.cola.archetype.source.agent.adapter.filter.ResearchApiKeyFilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The published document of the knowledge endpoints (Spec B §9.1, §9.2).
 *
 * <p>The twelve operations are the contract a caller reads before writing a line of code, so each
 * one is asserted by the pair of path and method it answers on: a renamed or dropped operation is a
 * breaking change to the document, and this is where it stops being quiet. The count of paths pins
 * the surface at that same size, so a new endpoint cannot appear without a deliberate edit here.
 *
 * <p>The answer endpoint carries a second mapping that exists only to refuse a caller whose
 * {@code Accept} cannot take a stream, and it is {@code @Hidden}. That annotation is not what keeps
 * it out of the document — two handler methods on one path and method collapse into one published
 * operation, and the streaming one wins, so the document is identical either way. The annotation
 * states the intent; the document is pinned by the two assertions above.
 *
 * <p>The last test is about the other half of the same document: the research operation this
 * archetype has always published must survive the knowledge domain's arrival unchanged.
 */
@SpringBootTest(classes = DeepResearchApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(DeepResearchApplicationTest.FakeAgentDependencies.class)
class KnowledgeOpenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposes_the_twelve_knowledge_operations() throws Exception {
        mockMvc.perform(apiDocs())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths.length()").value(8))
                .andExpect(jsonPath("$.paths['/api/v1/knowledge-bases'].post.operationId")
                        .value("createKnowledgeBase"))
                .andExpect(jsonPath("$.paths['/api/v1/knowledge-bases'].get.operationId")
                        .value("listKnowledgeBases"))
                .andExpect(jsonPath("$.paths['/api/v1/knowledge-bases/{knowledgeBaseId}'].get.operationId")
                        .value("getKnowledgeBase"))
                .andExpect(jsonPath("$.paths['/api/v1/knowledge-bases/{knowledgeBaseId}'].put.operationId")
                        .value("updateKnowledgeBase"))
                .andExpect(jsonPath("$.paths['/api/v1/knowledge-bases/{knowledgeBaseId}'].delete.operationId")
                        .value("deleteKnowledgeBase"))
                .andExpect(jsonPath("$.paths['/api/v1/knowledge-bases/{knowledgeBaseId}/documents'].post.operationId")
                        .value("uploadKnowledgeDocument"))
                .andExpect(jsonPath("$.paths['/api/v1/knowledge-bases/{knowledgeBaseId}/documents'].get.operationId")
                        .value("listKnowledgeDocuments"))
                .andExpect(jsonPath("$.paths['/api/v1/knowledge-documents/{documentId}'].get.operationId")
                        .value("getKnowledgeDocument"))
                .andExpect(jsonPath("$.paths['/api/v1/knowledge-documents/{documentId}'].delete.operationId")
                        .value("deleteKnowledgeDocument"))
                .andExpect(jsonPath("$.paths['/api/v1/knowledge-documents/{documentId}/reingest'].post.operationId")
                        .value("reingestKnowledgeDocument"))
                .andExpect(jsonPath("$.paths['/api/v1/knowledge-bases/{knowledgeBaseId}/retrieve'].post.operationId")
                        .value("retrieveKnowledgeChunks"))
                .andExpect(jsonPath("$.paths['/api/v1/knowledge-bases/{knowledgeBaseId}/chat'].post.operationId")
                        .value("chatWithKnowledgeBase"));
    }

    /**
     * The answer stream and the failures around it, which are two different representations.
     *
     * <p>The operation produces an event stream, so an error response that declares only a schema
     * inherits that media type and would document a failure as a stream frame. The failures of this
     * endpoint are answered with the shared JSON error body whatever the caller declared it accepts,
     * and the document has to say so for each of them.
     */
    @Test
    void publishes_the_shared_key_error_contract_and_the_answer_accept_requirement() throws Exception {
        mockMvc.perform(apiDocs())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/knowledge-bases/{knowledgeBaseId}/chat'].post"
                        + ".requestBody.content['application/json']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/knowledge-bases/{knowledgeBaseId}/chat'].post"
                        + ".responses['200'].content['text/event-stream']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/knowledge-bases/{knowledgeBaseId}/chat'].post"
                        + ".responses['406'].content['application/json'].schema.$ref")
                        .value("#/components/schemas/DeepResearchErrorResponse"))
                .andExpect(jsonPath("$.paths['/api/v1/knowledge-bases/{knowledgeBaseId}/chat'].post"
                        + ".responses['429'].content['application/json'].schema.$ref")
                        .value("#/components/schemas/DeepResearchErrorResponse"))
                .andExpect(jsonPath("$.components.schemas.KnowledgeQaEventVO").exists())
                .andExpect(jsonPath("$.components.schemas.DeepResearchErrorResponse").exists())
                .andExpect(jsonPath("$.components.securitySchemes.researchApiKey.name")
                        .value(ResearchApiKeyFilter.API_KEY_HEADER));
    }

    @Test
    void keeps_the_research_operation_unchanged() throws Exception {
        mockMvc.perform(apiDocs())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/deep-research/runs'].post.operationId")
                        .value("startDeepResearch"))
                .andExpect(jsonPath("$.paths['/api/v1/deep-research/runs'].post.responses['200']"
                        + ".content['text/event-stream']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/deep-research/runs'].post.responses['429']").exists());
    }

    private static MockHttpServletRequestBuilder apiDocs() {
        return get("/v3/api-docs").header(ResearchApiKeyFilter.API_KEY_HEADER, "test-research-key");
    }
}
