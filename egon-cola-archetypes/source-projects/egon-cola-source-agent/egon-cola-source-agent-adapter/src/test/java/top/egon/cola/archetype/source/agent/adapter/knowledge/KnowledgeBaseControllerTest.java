package top.egon.cola.archetype.source.agent.adapter.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import top.egon.cola.archetype.source.agent.adapter.config.DeepResearchApiProperties;
import top.egon.cola.archetype.source.agent.adapter.filter.ResearchApiKeyFilter;
import top.egon.cola.archetype.source.agent.adapter.filter.ResearchTraceFilter;
import top.egon.cola.archetype.source.agent.adapter.knowledge.controller.KnowledgeBaseController;
import top.egon.cola.archetype.source.agent.adapter.knowledge.handler.KnowledgeGlobalExceptionHandler;
import top.egon.cola.archetype.source.agent.application.knowledge.command.CreateKnowledgeBaseCommand;
import top.egon.cola.archetype.source.agent.application.knowledge.command.UpdateKnowledgeBaseCommand;
import top.egon.cola.archetype.source.agent.application.knowledge.exception.KnowledgeApplicationException;
import top.egon.cola.archetype.source.agent.application.knowledge.manage.KnowledgeBaseManage;
import top.egon.cola.archetype.source.agent.common.error.KnowledgeErrorCodeEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.ChunkingStrategyEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeBaseBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeBaseStatusEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeChunkConfigBO;
import top.egon.cola.component.common.core.pojo.PageResultRecord;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boundary contract of API-001 to API-005: the status codes, the field validation and the refusal of
 * the create-only fields.
 *
 * <p>The use case is a hand-written stand-in, so what the assertions lock is the boundary's own work:
 * the request is bound and normalized into a command, the immutable probes reach the use case
 * instead of being dropped by the binder, and each knowledge code is rendered with its documented
 * status and error body.
 */
class KnowledgeBaseControllerTest {

    private static final String API_KEY = "test-key";
    private static final Instant NOW = Instant.parse("2026-09-10T06:00:00Z");
    private static final String TRACE_ID = "4e9d6938";
    private static final String BASES = "/api/v1/knowledge-bases";
    private static final String IMMUTABLE_FIELD_MESSAGE = "immutable after creation";

    private static ValidatorFactory validatorFactory;

    private final RecordingManage manage = new RecordingManage(base());

    private MockMvc mvc;

    @BeforeAll
    static void startValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void stopValidator() {
        validatorFactory.close();
    }

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        mvc = MockMvcBuilders.standaloneSetup(new KnowledgeBaseController(manage))
                .setValidator(validator)
                .addFilters(
                        new ResearchTraceFilter(Clock.fixed(NOW, ZoneOffset.UTC), mapper),
                        new ResearchApiKeyFilter(new DeepResearchApiProperties(API_KEY), mapper,
                                Clock.fixed(NOW, ZoneOffset.UTC)))
                .setControllerAdvice(new KnowledgeGlobalExceptionHandler(Clock.fixed(NOW, ZoneOffset.UTC)))
                .build();
    }

    @Test
    void creates_a_knowledge_base() throws Exception {
        mvc.perform(authenticated(MockMvcRequestBuilders.post(BASES)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":" product-docs ","name":"产品文档库","embeddingModel":"openai-small",
                                 "chunkStrategy":"TOKEN",
                                 "chunkConfig":{"maxTokensPerChunk":512,"overlapTokens":64,"minChunkChars":1}}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(ResearchTraceFilter.TRACE_HEADER, TRACE_ID))
                .andExpect(jsonPath("$.knowledgeBaseId").value(7))
                .andExpect(jsonPath("$.code").value("product-docs"))
                .andExpect(jsonPath("$.name").value("产品文档库"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.chunkStrategy").value("TOKEN"))
                .andExpect(jsonPath("$.chunkConfig.maxTokensPerChunk").value(512))
                .andExpect(jsonPath("$.chunkConfig.headingLevels").doesNotExist())
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.documentCount").value(3))
                .andExpect(jsonPath("$.traceId").value(TRACE_ID));

        assertEquals(TRACE_ID, manage.created.traceId());
        assertEquals("product-docs", manage.created.code());
        assertEquals(512, manage.created.chunkConfig().maxTokensPerChunk());
        assertEquals("openai-small", manage.created.embeddingModel());
    }

    @Test
    void lists_bases_with_the_page_envelope() throws Exception {
        mvc.perform(authenticated(MockMvcRequestBuilders.get(BASES)).param("page", "1").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].knowledgeBaseId").value(7))
                .andExpect(jsonPath("$.records[0].documentCount").value(3))
                .andExpect(jsonPath("$.page.total").value(1))
                .andExpect(jsonPath("$.page.pageNo").value(1))
                .andExpect(jsonPath("$.page.hasNext").value(false));
    }

    @Test
    void returns_the_full_representation_of_one_base() throws Exception {
        mvc.perform(authenticated(MockMvcRequestBuilders.get(BASES + "/7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.knowledgeBaseId").value(7))
                .andExpect(jsonPath("$.chunkConfig.overlapTokens").value(64))
                .andExpect(jsonPath("$.embeddingModel").value("openai-small"))
                .andExpect(jsonPath("$.traceId").value(TRACE_ID));
    }

    @Test
    void updates_the_editable_metadata_and_keeps_the_frozen_configuration() throws Exception {
        mvc.perform(authenticated(MockMvcRequestBuilders.put(BASES + "/7")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"内网产品手册\",\"description\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("产品文档库"))
                .andExpect(jsonPath("$.chunkStrategy").value("TOKEN"));

        assertEquals(7L, manage.updated.knowledgeBaseId());
        assertEquals(TRACE_ID, manage.updated.traceId());
        assertNull(manage.updated.description());
    }

    @Test
    void rejects_unknown_keys_on_update() throws Exception {
        mvc.perform(authenticated(MockMvcRequestBuilders.put(BASES + "/7")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"n\",\"embeddingModel\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_IMMUTABLE_FIELD"))
                .andExpect(jsonPath("$.fieldErrors.embeddingModel[0]").value(IMMUTABLE_FIELD_MESSAGE))
                .andExpect(jsonPath("$.traceId").value(TRACE_ID));

        assertEquals(List.of("embeddingModel"), manage.updated.immutableFields());
    }

    @Test
    void rejects_a_body_the_contract_does_not_define() throws Exception {
        mvc.perform(authenticated(MockMvcRequestBuilders.put(BASES + "/7")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"n\",\"frozen\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_VALIDATION_ERROR"));
    }

    @Test
    void rejects_a_body_that_violates_the_field_constraints() throws Exception {
        mvc.perform(authenticated(MockMvcRequestBuilders.post(BASES)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"product docs","name":"","embeddingModel":"openai-small",
                                 "chunkStrategy":"TOKEN","chunkConfig":{"maxTokensPerChunk":1}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.code").exists())
                .andExpect(jsonPath("$.fieldErrors['chunkConfig.maxTokensPerChunk']").exists());

        assertNull(manage.created);
    }

    @Test
    void locks_the_paging_range() throws Exception {
        mvc.perform(authenticated(MockMvcRequestBuilders.get(BASES)).param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.size").exists());
    }

    @Test
    void rejects_a_malformed_identifier() throws Exception {
        mvc.perform(authenticated(MockMvcRequestBuilders.get(BASES + "/not-an-identifier!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.knowledgeBaseId").exists());
    }

    @Test
    void returns_conflict_on_duplicate_code() throws Exception {
        manage.failure = new KnowledgeApplicationException(
                KnowledgeErrorCodeEnum.KNOWLEDGE_BASE_CODE_CONFLICT, TRACE_ID);

        mvc.perform(authenticated(MockMvcRequestBuilders.post(BASES)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"product-docs","name":"产品文档库","embeddingModel":"openai-small",
                                 "chunkStrategy":"TOKEN","chunkConfig":{"maxTokensPerChunk":512}}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_BASE_CODE_CONFLICT"))
                .andExpect(jsonPath("$.message").value("knowledge base code already exists"));
    }

    @Test
    void returns_not_found_after_deletion() throws Exception {
        mvc.perform(authenticated(MockMvcRequestBuilders.delete(BASES + "/7")))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mvc.perform(authenticated(MockMvcRequestBuilders.get(BASES + "/7")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_BASE_NOT_FOUND"));

        assertEquals(7L, manage.deleted);
    }

    @Test
    void refuses_to_delete_a_base_that_still_has_a_document_in_progress() throws Exception {
        manage.failure = new KnowledgeApplicationException(
                KnowledgeErrorCodeEnum.KNOWLEDGE_BASE_BUSY, TRACE_ID);

        mvc.perform(authenticated(MockMvcRequestBuilders.delete(BASES + "/7")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_BASE_BUSY"))
                .andExpect(jsonPath("$.traceId").value(TRACE_ID));
    }

    @Test
    void refuses_a_request_without_the_api_key() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(BASES + "/7"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "ApiKey"))
                .andExpect(jsonPath("$.code").value("RESEARCH_UNAUTHORIZED"));
    }

    private static MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
        return request.header(ResearchApiKeyFilter.API_KEY_HEADER, API_KEY)
                .header(ResearchTraceFilter.TRACE_HEADER, TRACE_ID);
    }

    private static KnowledgeBaseBO base() {
        return new KnowledgeBaseBO(7L, 1L, "product-docs", "产品文档库", null, "openai-small",
                ChunkingStrategyEnum.TOKEN, new KnowledgeChunkConfigBO(512, 64, 1, List.of()),
                KnowledgeBaseStatusEnum.ACTIVE, NOW, NOW);
    }

    /** Stand-in for the use case: keeps the stored bases, records what the boundary handed over. */
    private static final class RecordingManage implements KnowledgeBaseManage {

        private final Map<Long, KnowledgeBaseBO> bases = new LinkedHashMap<>();

        private KnowledgeApplicationException failure;
        private CreateKnowledgeBaseCommand created;
        private UpdateKnowledgeBaseCommand updated;
        private Long deleted;

        private RecordingManage(KnowledgeBaseBO base) {
            bases.put(base.knowledgeBaseId(), base);
        }

        @Override
        public KnowledgeBaseBO create(CreateKnowledgeBaseCommand command) {
            created = command;
            throwIfFailed();
            return bases.get(7L);
        }

        @Override
        public KnowledgeBaseBO get(Long knowledgeBaseId) {
            throwIfFailed();
            return required(knowledgeBaseId);
        }

        @Override
        public PageResultRecord<KnowledgeBaseBO> page(int page, int size, String keyword, String embeddingModel) {
            throwIfFailed();
            return PageResultRecord.success(List.copyOf(bases.values()), bases.size(), page, size);
        }

        @Override
        public KnowledgeBaseBO update(UpdateKnowledgeBaseCommand command) {
            updated = command;
            List<String> immutable = command.immutableFields();
            if (!immutable.isEmpty()) {
                // The boundary's job is to hand the probe over; the rule itself belongs to the use
                // case (KnowledgeManageTest locks it) and is mirrored here so the status is testable.
                Map<String, List<String>> errors = new LinkedHashMap<>();
                immutable.forEach(field -> errors.put(field, List.of(IMMUTABLE_FIELD_MESSAGE)));
                throw new KnowledgeApplicationException(KnowledgeErrorCodeEnum.KNOWLEDGE_IMMUTABLE_FIELD,
                        command.traceId(), errors, null);
            }
            throwIfFailed();
            return required(command.knowledgeBaseId());
        }

        @Override
        public void delete(Long knowledgeBaseId) {
            deleted = knowledgeBaseId;
            throwIfFailed();
            bases.remove(knowledgeBaseId);
        }

        @Override
        public long documentCount(Long knowledgeBaseId) {
            throwIfFailed();
            required(knowledgeBaseId);
            return 3L;
        }

        private KnowledgeBaseBO required(Long knowledgeBaseId) {
            KnowledgeBaseBO found = knowledgeBaseId == null ? null : bases.get(knowledgeBaseId);
            if (found == null) {
                throw new KnowledgeApplicationException(
                        KnowledgeErrorCodeEnum.KNOWLEDGE_BASE_NOT_FOUND, TRACE_ID);
            }
            return found;
        }

        private void throwIfFailed() {
            if (failure != null) {
                throw failure;
            }
        }
    }
}
