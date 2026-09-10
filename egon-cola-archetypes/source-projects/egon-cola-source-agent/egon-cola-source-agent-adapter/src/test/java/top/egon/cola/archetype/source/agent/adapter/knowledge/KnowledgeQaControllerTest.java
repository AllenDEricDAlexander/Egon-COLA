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
import org.springframework.mock.web.MockAsyncContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import top.egon.cola.archetype.source.agent.adapter.config.DeepResearchApiProperties;
import top.egon.cola.archetype.source.agent.adapter.filter.ResearchApiKeyFilter;
import top.egon.cola.archetype.source.agent.adapter.filter.ResearchTraceFilter;
import top.egon.cola.archetype.source.agent.adapter.knowledge.controller.KnowledgeQaController;
import top.egon.cola.archetype.source.agent.adapter.knowledge.handler.KnowledgeGlobalExceptionHandler;
import top.egon.cola.archetype.source.agent.application.knowledge.command.AskKnowledgeBaseCommand;
import top.egon.cola.archetype.source.agent.application.knowledge.command.RetrieveKnowledgeCommand;
import top.egon.cola.archetype.source.agent.application.knowledge.config.KnowledgeRuntimeProperties;
import top.egon.cola.archetype.source.agent.application.knowledge.exception.KnowledgeApplicationException;
import top.egon.cola.archetype.source.agent.application.knowledge.manage.KnowledgeQaManage;
import top.egon.cola.archetype.source.agent.common.error.KnowledgeErrorCodeEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeQaEvent;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeRetrievalBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeRetrievedChunkBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.service.KnowledgeAnswerRunService;
import top.egon.cola.archetype.source.agent.domain.knowledge.service.KnowledgeQaEventObserverService;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boundary contract of API-011 and API-012: the retrieval envelope, the answer stream, and the
 * capacity answer that is decided before a stream exists.
 *
 * <p>The use case is a hand-written stand-in that scripts its own observer calls, so what the
 * assertions lock is the boundary's own work: which request fields reach the command, how a domain
 * event becomes a wire event, that a terminal event ends the stream, and that a caller going away
 * cancels the generation it started.
 */
class KnowledgeQaControllerTest {

    private static final String API_KEY = "test-key";
    private static final Instant NOW = Instant.parse("2026-09-10T06:00:00Z");
    private static final String TRACE_ID = "4e9d6938";
    private static final String BASES = "/api/v1/knowledge-bases";
    private static final long BASE_ID = 7L;
    private static final long DOCUMENT_ID = 9L;
    private static final int CHUNK_INDEX = 7;
    private static final double SCORE = 0.83d;
    private static final String DISPLAY_NAME = "report.pdf";
    private static final String ANSWER_ID = "qa-1";
    private static final String QUESTION = "超时怎么配置？";
    private static final String QUERY = "如何配置超时";
    private static final String CHUNK_TEXT = "第一章 概述\n正文";
    private static final String REFERENCE_TEXT = "超时由 max-duration 控制";
    private static final String DELTA = "超时通过 ";
    private static final String ANSWER = "超时通过 max-duration 配置，默认 PT5M。";

    private static ValidatorFactory validatorFactory;

    private final RecordingQaManage manage = new RecordingQaManage();

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
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        mvc = MockMvcBuilders.standaloneSetup(new KnowledgeQaController(manage, runtimeProperties()))
                .setValidator(validator)
                .addFilters(new ResearchTraceFilter(clock, mapper),
                        new ResearchApiKeyFilter(new DeepResearchApiProperties(API_KEY), mapper, clock))
                .setControllerAdvice(new KnowledgeGlobalExceptionHandler(clock))
                .build();
    }

    @Test
    void retrieves_without_calling_the_chat_model() throws Exception {
        manage.retrieval = new KnowledgeRetrievalBO("openai-small",
                List.of(new KnowledgeRetrievedChunkBO(DOCUMENT_ID, CHUNK_INDEX, DISPLAY_NAME, SCORE, CHUNK_TEXT)));

        mvc.perform(authenticated(MockMvcRequestBuilders.post(BASES + "/" + BASE_ID + "/retrieve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\" " + QUERY + " \",\"topK\":3,\"similarityThreshold\":0.2}")))
                .andExpect(status().isOk())
                .andExpect(header().string(ResearchTraceFilter.TRACE_HEADER, TRACE_ID))
                .andExpect(jsonPath("$.items[0].documentId").value(DOCUMENT_ID))
                .andExpect(jsonPath("$.items[0].chunkIndex").value(CHUNK_INDEX))
                .andExpect(jsonPath("$.items[0].score").value(SCORE))
                .andExpect(jsonPath("$.items[0].content").value(CHUNK_TEXT))
                .andExpect(jsonPath("$.items[0].displayName").value(DISPLAY_NAME))
                .andExpect(jsonPath("$.query").value(QUERY))
                .andExpect(jsonPath("$.embeddingModel").value("openai-small"))
                .andExpect(jsonPath("$.traceId").value(TRACE_ID));

        assertEquals(BASE_ID, manage.retrieved.knowledgeBaseId());
        assertEquals(QUERY, manage.retrieved.query());
        assertEquals(3, manage.retrieved.topK().intValue());
        assertEquals(0.2d, manage.retrieved.similarityThreshold().doubleValue());
        assertEquals(0, manage.askCalls);
    }

    @Test
    void returns_an_empty_result_when_nothing_matches() throws Exception {
        manage.retrieval = new KnowledgeRetrievalBO("openai-small", List.of());

        mvc.perform(authenticated(MockMvcRequestBuilders.post(BASES + "/" + BASE_ID + "/retrieve")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"query\":\"" + QUERY + "\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.embeddingModel").value("openai-small"));

        assertNull(manage.retrieved.topK());
        assertNull(manage.retrieved.similarityThreshold());
    }

    @Test
    void rejects_a_blank_query() throws Exception {
        mvc.perform(authenticated(retrieve("{\"query\":\"   \"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.query[0]").exists());

        assertNull(manage.retrieved);
    }

    @Test
    void rejects_a_top_k_above_the_component_cap() throws Exception {
        mvc.perform(authenticated(retrieve("{\"query\":\"" + QUERY + "\",\"topK\":51}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.topK[0]").exists());

        assertNull(manage.retrieved);
    }

    @Test
    void rejects_a_threshold_outside_the_unit_range() throws Exception {
        mvc.perform(authenticated(retrieve("{\"query\":\"" + QUERY + "\",\"similarityThreshold\":1.5}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.similarityThreshold[0]").exists());

        assertNull(manage.retrieved);
    }

    @Test
    void refuses_a_retrieval_that_names_a_collection() throws Exception {
        mvc.perform(authenticated(retrieve("{\"query\":\"" + QUERY + "\",\"collectionId\":\"other-base\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_VALIDATION_ERROR"));

        assertNull(manage.retrieved);
    }

    @Test
    void reports_a_missing_base_on_retrieval() throws Exception {
        manage.failure = new KnowledgeApplicationException(KnowledgeErrorCodeEnum.KNOWLEDGE_BASE_NOT_FOUND, TRACE_ID);

        mvc.perform(authenticated(retrieve("{\"query\":\"" + QUERY + "\"}")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_BASE_NOT_FOUND"));
    }

    @Test
    void reports_an_unavailable_vector_store() throws Exception {
        manage.failure = new KnowledgeApplicationException(
                KnowledgeErrorCodeEnum.KNOWLEDGE_DEPENDENCY_UNAVAILABLE, TRACE_ID);

        mvc.perform(authenticated(retrieve("{\"query\":\"" + QUERY + "\"}")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_DEPENDENCY_UNAVAILABLE"));
    }

    @Test
    void emits_started_progress_completed_in_order() throws Exception {
        List<KnowledgeRetrievedChunkBO> references = references();
        manage.script = observer -> {
            observer.onEvent(KnowledgeQaEvent.started(ANSWER_ID, 1, references, NOW, TRACE_ID));
            observer.onEvent(KnowledgeQaEvent.progress(ANSWER_ID, 2, DELTA, NOW));
            observer.onEvent(KnowledgeQaEvent.completed(ANSWER_ID, 3, ANSWER, references, NOW, TRACE_ID));
        };

        String stream = chat();

        assertEquals(List.of("knowledge.started", "knowledge.progress", "knowledge.completed"), eventNames(stream));
        assertTrue(stream.contains("id:qa-1:1"));
        assertTrue(stream.contains("id:qa-1:3"));
        assertTrue(stream.contains("\"type\":\"STARTED\""));
        assertTrue(stream.contains("\"delta\":\"" + DELTA + "\""));
        assertTrue(stream.contains("\"answer\":\"" + ANSWER + "\""));
        assertTrue(stream.contains("\"traceId\":\"" + TRACE_ID + "\""));
        assertEquals(2, occurrences(stream, "\"retrievals\":["));
        assertTrue(stream.contains("{\"documentId\":9,\"chunkIndex\":7,\"displayName\":\"report.pdf\",\"score\":0.83}"));
        assertFalse(stream.contains(REFERENCE_TEXT));
    }

    @Test
    void emits_one_failed_on_dependency_failure() throws Exception {
        manage.script = observer -> {
            observer.onEvent(KnowledgeQaEvent.started(ANSWER_ID, 1, references(), NOW, TRACE_ID));
            observer.onEvent(KnowledgeQaEvent.failed(ANSWER_ID, 2,
                    KnowledgeErrorCodeEnum.KNOWLEDGE_DEPENDENCY_UNAVAILABLE, NOW, TRACE_ID));
        };

        String stream = chat();

        assertEquals(List.of("knowledge.started", "knowledge.failed"), eventNames(stream));
        assertEquals(1, occurrences(stream, "knowledge.failed"));
        assertTrue(stream.contains("\"code\":\"KNOWLEDGE_DEPENDENCY_UNAVAILABLE\""));
        assertTrue(stream.contains("\"message\":\"knowledge dependency is unavailable\""));
        assertTrue(stream.contains("\"retryable\":true"));
    }

    @Test
    void ignores_an_event_after_the_terminal_one() throws Exception {
        manage.script = observer -> {
            observer.onEvent(KnowledgeQaEvent.completed(ANSWER_ID, 1, ANSWER, references(), NOW, TRACE_ID));
            observer.onEvent(KnowledgeQaEvent.progress(ANSWER_ID, 2, "迟到的增量", NOW));
        };

        String stream = chat();

        assertEquals(List.of("knowledge.completed"), eventNames(stream));
        assertFalse(stream.contains("迟到的增量"));
    }

    @Test
    void returns_429_when_saturated() throws Exception {
        manage.failure = new KnowledgeApplicationException(
                KnowledgeErrorCodeEnum.KNOWLEDGE_CAPACITY_EXHAUSTED, TRACE_ID);

        mvc.perform(authenticated(chatRequest()))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "5"))
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_CAPACITY_EXHAUSTED"));

        assertEquals(0, manage.run.cancellations);
    }

    @Test
    void answers_the_knowledge_code_when_the_client_cannot_take_an_event_stream() throws Exception {
        mvc.perform(authenticated(MockMvcRequestBuilders.post(BASES + "/" + BASE_ID + "/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(question(QUESTION))))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_NOT_ACCEPTABLE"));

        assertNull(manage.asked);
    }

    @Test
    void releases_the_permit_on_disconnect() throws Exception {
        manage.script = observer ->
                observer.onEvent(KnowledgeQaEvent.started(ANSWER_ID, 1, references(), NOW, TRACE_ID));

        MvcResult result = mvc.perform(authenticated(chatRequest())).andReturn();

        assertTrue(result.getRequest().isAsyncStarted());
        assertEquals(0, manage.run.cancellations);

        ((MockAsyncContext) result.getRequest().getAsyncContext()).complete();

        assertEquals(1, manage.run.cancellations);
    }

    @Test
    void rejects_a_blank_question() throws Exception {
        mvc.perform(authenticated(chatRequestOf("{\"question\":\"   \"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.question[0]").exists());

        assertNull(manage.asked);
    }

    @Test
    void rejects_a_question_with_control_characters() throws Exception {
        mvc.perform(authenticated(chatRequestOf("{\"question\":\"超时\\n怎么配置\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.question[0]").exists());

        assertNull(manage.asked);
    }

    @Test
    void refuses_a_chat_that_names_an_embedding_model() throws Exception {
        mvc.perform(authenticated(chatRequestOf(
                        "{\"question\":\"" + QUESTION + "\",\"embeddingModel\":\"other-model\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_VALIDATION_ERROR"));

        assertNull(manage.asked);
    }

    @Test
    void refuses_a_chat_without_the_api_key() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post(BASES + "/" + BASE_ID + "/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content(question(QUESTION)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "ApiKey"))
                .andExpect(jsonPath("$.code").value("RESEARCH_UNAUTHORIZED"));

        assertNull(manage.asked);
    }

    private String chat() throws Exception {
        return chat(question(QUESTION));
    }

    /**
     * Runs one chat request to the end of its stream and returns the raw event frames.
     *
     * <p>The bytes are decoded as UTF-8 explicitly: an event stream is UTF-8 by specification and its
     * content type therefore carries no charset parameter, which leaves the response reading as the
     * container default instead.
     */
    private String chat(String body) throws Exception {
        MvcResult result = mvc.perform(authenticated(chatRequestOf(body))).andReturn();
        if (result.getRequest().isAsyncStarted()) {
            result = mvc.perform(MockMvcRequestBuilders.asyncDispatch(result)).andReturn();
        }
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static MockHttpServletRequestBuilder chatRequest() {
        return chatRequestOf(question(QUESTION));
    }

    private static MockHttpServletRequestBuilder chatRequestOf(String body) {
        return MockMvcRequestBuilders.post(BASES + "/" + BASE_ID + "/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .content(body);
    }

    private static MockHttpServletRequestBuilder retrieve(String body) {
        return MockMvcRequestBuilders.post(BASES + "/" + BASE_ID + "/retrieve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private static MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
        return request.header(ResearchApiKeyFilter.API_KEY_HEADER, API_KEY)
                .header(ResearchTraceFilter.TRACE_HEADER, TRACE_ID);
    }

    private static String question(String value) {
        return "{\"question\":\"" + value + "\"}";
    }

    private static List<KnowledgeRetrievedChunkBO> references() {
        return List.of(new KnowledgeRetrievedChunkBO(DOCUMENT_ID, CHUNK_INDEX, DISPLAY_NAME, SCORE, REFERENCE_TEXT));
    }

    private static List<String> eventNames(String stream) {
        return stream.lines()
                .filter(line -> line.startsWith("event:"))
                .map(line -> line.substring("event:".length()))
                .toList();
    }

    private static int occurrences(String stream, String token) {
        return (stream.length() - stream.replace(token, "").length()) / token.length();
    }

    private static KnowledgeRuntimeProperties runtimeProperties() {
        return new KnowledgeRuntimeProperties(
                new KnowledgeRuntimeProperties.Runtime(4, 20L * 1024 * 1024, 10_000, Duration.ofMinutes(2)),
                new KnowledgeRuntimeProperties.Tenant(0L));
    }

    /** Stand-in for the use case: records the commands it received and scripts the event stream. */
    private static final class RecordingQaManage implements KnowledgeQaManage {

        private final RecordingRun run = new RecordingRun();

        private KnowledgeApplicationException failure;
        private KnowledgeRetrievalBO retrieval = new KnowledgeRetrievalBO("openai-small", List.of());
        private Consumer<KnowledgeQaEventObserverService> script = observer -> {
        };
        private RetrieveKnowledgeCommand retrieved;
        private AskKnowledgeBaseCommand asked;
        private int askCalls;

        @Override
        public KnowledgeRetrievalBO retrieve(RetrieveKnowledgeCommand command) {
            retrieved = command;
            throwIfFailed();
            return retrieval;
        }

        @Override
        public KnowledgeAnswerRunService ask(AskKnowledgeBaseCommand command, KnowledgeQaEventObserverService observer) {
            asked = command;
            askCalls++;
            throwIfFailed();
            script.accept(observer);
            return run;
        }

        private void throwIfFailed() {
            if (failure != null) {
                throw failure;
            }
        }
    }

    /** The cancel handle of one streamed answer, which the boundary must exercise exactly once. */
    private static final class RecordingRun implements KnowledgeAnswerRunService {

        private int cancellations;

        @Override
        public void cancel() {
            cancellations++;
        }
    }
}
