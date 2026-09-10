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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import top.egon.cola.archetype.source.agent.adapter.config.DeepResearchApiProperties;
import top.egon.cola.archetype.source.agent.adapter.filter.ResearchApiKeyFilter;
import top.egon.cola.archetype.source.agent.adapter.filter.ResearchTraceFilter;
import top.egon.cola.archetype.source.agent.adapter.knowledge.controller.KnowledgeDocumentController;
import top.egon.cola.archetype.source.agent.adapter.knowledge.handler.KnowledgeGlobalExceptionHandler;
import top.egon.cola.archetype.source.agent.application.knowledge.command.UploadKnowledgeDocumentCommand;
import top.egon.cola.archetype.source.agent.application.knowledge.exception.KnowledgeApplicationException;
import top.egon.cola.archetype.source.agent.application.knowledge.manage.KnowledgeDocumentManage;
import top.egon.cola.archetype.source.agent.common.error.KnowledgeErrorCodeEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.DocumentIngestStatusEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeDocumentBO;
import top.egon.cola.component.common.core.pojo.PageResultRecord;

import java.nio.charset.StandardCharsets;
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
 * Boundary contract of API-006 to API-010: the upload handover, the status codes, and the rule that a
 * document representation states how much text was stored without transporting it.
 *
 * <p>The use case is a hand-written stand-in, so what the assertions lock is the boundary's own work:
 * the multipart parts are bound and normalized into a command, the identifier is parsed into a lookup
 * the use case can perform, and each document code is rendered with its documented status.
 */
class KnowledgeDocumentControllerTest {

    private static final String API_KEY = "test-key";
    private static final Instant NOW = Instant.parse("2026-09-10T06:00:00Z");
    private static final String TRACE_ID = "4e9d6938";
    private static final String BASES = "/api/v1/knowledge-bases";
    private static final String DOCUMENTS = "/api/v1/knowledge-documents";
    private static final long DOCUMENT_ID = 9L;
    private static final String CONTENT = "第一章 概述\n正文";
    private static final String CONTENT_HASH =
            "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";
    private static final String FILE_PART = "file";

    private static ValidatorFactory validatorFactory;

    private final RecordingManage manage = new RecordingManage(document());

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
        mvc = MockMvcBuilders.standaloneSetup(new KnowledgeDocumentController(manage))
                .setValidator(validator)
                .addFilters(
                        new ResearchTraceFilter(Clock.fixed(NOW, ZoneOffset.UTC), mapper),
                        new ResearchApiKeyFilter(new DeepResearchApiProperties(API_KEY), mapper,
                                Clock.fixed(NOW, ZoneOffset.UTC)))
                .setControllerAdvice(new KnowledgeGlobalExceptionHandler(Clock.fixed(NOW, ZoneOffset.UTC)))
                .build();
    }

    @Test
    void accepts_a_multipart_upload() throws Exception {
        mvc.perform(authenticated(MockMvcRequestBuilders.multipart(BASES + "/7/documents")
                        .file(part("report.txt", CONTENT)).param("displayName", "2026 手册")))
                .andExpect(status().isAccepted())
                .andExpect(header().string(ResearchTraceFilter.TRACE_HEADER, TRACE_ID))
                .andExpect(jsonPath("$.documentId").value(DOCUMENT_ID))
                .andExpect(jsonPath("$.knowledgeBaseId").value(7))
                .andExpect(jsonPath("$.displayName").value("2026 手册"))
                .andExpect(jsonPath("$.fileName").value("report.txt"))
                .andExpect(jsonPath("$.mimeType").value(MediaType.TEXT_PLAIN_VALUE))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.chunkCount").value(0))
                .andExpect(jsonPath("$.contentChars").value(CONTENT.length()))
                .andExpect(jsonPath("$.traceId").value(TRACE_ID));

        assertEquals(7L, manage.uploaded.knowledgeBaseId());
        assertEquals("2026 手册", manage.uploaded.displayName());
        assertEquals(CONTENT, new String(manage.uploaded.content(), StandardCharsets.UTF_8));
        assertEquals(TRACE_ID, manage.uploaded.traceId());
    }

    @Test
    void defaults_the_display_name_to_the_file_name() throws Exception {
        mvc.perform(authenticated(MockMvcRequestBuilders.multipart(BASES + "/7/documents")
                        .file(part("report.txt", CONTENT))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.displayName").value("report.txt"));

        assertEquals("report.txt", manage.uploaded.displayName());
    }

    @Test
    void rejects_an_upload_without_a_file() throws Exception {
        mvc.perform(authenticated(MockMvcRequestBuilders.multipart(BASES + "/7/documents")
                        .param("displayName", "2026 手册")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.file").exists())
                .andExpect(jsonPath("$.traceId").value(TRACE_ID));

        assertNull(manage.uploaded);
    }

    @Test
    void rejects_an_empty_file() throws Exception {
        mvc.perform(authenticated(MockMvcRequestBuilders.multipart(BASES + "/7/documents")
                        .file(part("report.txt", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.file").exists());
    }

    @Test
    void rejects_a_blank_display_name() throws Exception {
        mvc.perform(authenticated(MockMvcRequestBuilders.multipart(BASES + "/7/documents")
                        .file(part("report.txt", CONTENT)).param("displayName", "   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.displayName").exists());
    }

    @Test
    void reports_a_base_that_does_not_exist() throws Exception {
        manage.failure = new KnowledgeApplicationException(
                KnowledgeErrorCodeEnum.KNOWLEDGE_BASE_NOT_FOUND, TRACE_ID);

        mvc.perform(authenticated(MockMvcRequestBuilders.multipart(BASES + "/7/documents")
                        .file(part("report.txt", CONTENT))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_BASE_NOT_FOUND"));
    }

    @Test
    void returns_413_when_the_file_exceeds_the_limit() throws Exception {
        manage.failure = KnowledgeApplicationException.onField(
                KnowledgeErrorCodeEnum.KNOWLEDGE_FILE_TOO_LARGE, TRACE_ID, FILE_PART,
                "the upload exceeds 20971520 bytes", null);

        mvc.perform(authenticated(MockMvcRequestBuilders.multipart(BASES + "/7/documents")
                        .file(part("report.txt", CONTENT))))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_FILE_TOO_LARGE"))
                .andExpect(jsonPath("$.fieldErrors.file").exists());
    }

    @Test
    void returns_content_length_not_content() throws Exception {
        mvc.perform(authenticated(MockMvcRequestBuilders.get(DOCUMENTS + "/" + DOCUMENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(DOCUMENT_ID))
                .andExpect(jsonPath("$.knowledgeBaseId").value(7))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.chunkCount").value(5))
                .andExpect(jsonPath("$.attemptCount").value(3))
                .andExpect(jsonPath("$.contentChars").value(CONTENT.length()))
                .andExpect(jsonPath("$.content").doesNotExist())
                .andExpect(jsonPath("$.errorCode").doesNotExist())
                .andExpect(jsonPath("$.traceId").value(TRACE_ID));
    }

    @Test
    void lists_documents_with_the_page_envelope() throws Exception {
        mvc.perform(authenticated(MockMvcRequestBuilders.get(BASES + "/7/documents"))
                        .param("page", "1").param("size", "20")
                        .param("status", "SUCCEEDED").param("keyword", "report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].documentId").value(DOCUMENT_ID))
                .andExpect(jsonPath("$.records[0].contentChars").value(CONTENT.length()))
                .andExpect(jsonPath("$.page.total").value(1))
                .andExpect(jsonPath("$.page.pageNo").value(1))
                .andExpect(jsonPath("$.page.hasNext").value(false));

        assertEquals(DocumentIngestStatusEnum.SUCCEEDED, manage.filteredStatus);
        assertEquals("report", manage.searched);
        assertEquals(1, manage.pageNo);
        assertEquals(20, manage.pageSize);
    }

    @Test
    void rejects_an_unsupported_status_filter() throws Exception {
        mvc.perform(authenticated(MockMvcRequestBuilders.get(BASES + "/7/documents"))
                        .param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.status").exists());
    }

    @Test
    void locks_the_paging_range() throws Exception {
        mvc.perform(authenticated(MockMvcRequestBuilders.get(BASES + "/7/documents")).param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.size").exists());
    }

    @Test
    void returns_404_for_an_unknown_document() throws Exception {
        mvc.perform(authenticated(MockMvcRequestBuilders.get(DOCUMENTS + "/404")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_DOCUMENT_NOT_FOUND"));
    }

    @Test
    void rejects_a_malformed_identifier() throws Exception {
        mvc.perform(authenticated(MockMvcRequestBuilders.get(DOCUMENTS + "/doc-01J5K9")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.documentId").exists());
    }

    @Test
    void reingests_a_terminal_document() throws Exception {
        mvc.perform(authenticated(MockMvcRequestBuilders.post(DOCUMENTS + "/" + DOCUMENT_ID + "/reingest")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.attemptCount").value(0))
                .andExpect(jsonPath("$.chunkCount").value(0))
                .andExpect(jsonPath("$.errorCode").doesNotExist())
                .andExpect(jsonPath("$.contentChars").value(CONTENT.length()));

        assertEquals(DOCUMENT_ID, manage.reingested);
    }

    @Test
    void rejects_reingest_while_processing() throws Exception {
        manage.failure = new KnowledgeApplicationException(
                KnowledgeErrorCodeEnum.KNOWLEDGE_DOCUMENT_BUSY, TRACE_ID);

        mvc.perform(authenticated(MockMvcRequestBuilders.post(DOCUMENTS + "/" + DOCUMENT_ID + "/reingest")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_DOCUMENT_BUSY"))
                .andExpect(jsonPath("$.traceId").value(TRACE_ID));
    }

    @Test
    void rejects_a_reingest_body() throws Exception {
        mvc.perform(authenticated(MockMvcRequestBuilders.post(DOCUMENTS + "/" + DOCUMENT_ID + "/reingest"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"force\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.body").exists());
    }

    @Test
    void deletes_the_document() throws Exception {
        mvc.perform(authenticated(MockMvcRequestBuilders.delete(DOCUMENTS + "/" + DOCUMENT_ID)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mvc.perform(authenticated(MockMvcRequestBuilders.get(DOCUMENTS + "/" + DOCUMENT_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("KNOWLEDGE_DOCUMENT_NOT_FOUND"));

        assertEquals(DOCUMENT_ID, manage.deleted);
    }

    @Test
    void refuses_a_request_without_the_api_key() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(DOCUMENTS + "/" + DOCUMENT_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "ApiKey"))
                .andExpect(jsonPath("$.code").value("RESEARCH_UNAUTHORIZED"));
    }

    private static MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
        return request.header(ResearchApiKeyFilter.API_KEY_HEADER, API_KEY)
                .header(ResearchTraceFilter.TRACE_HEADER, TRACE_ID);
    }

    private static MockMultipartFile part(String fileName, String content) {
        return new MockMultipartFile(FILE_PART, fileName, MediaType.TEXT_PLAIN_VALUE,
                content.getBytes(StandardCharsets.UTF_8));
    }

    private static KnowledgeDocumentBO document() {
        return new KnowledgeDocumentBO(DOCUMENT_ID, 0L, 7L, "report.txt", "report.txt",
                MediaType.TEXT_PLAIN_VALUE, CONTENT.length(), CONTENT_HASH, "LOCAL", "7/original/report.txt",
                CONTENT, DocumentIngestStatusEnum.SUCCEEDED, 5, 3, null, null, NOW, NOW);
    }

    /** Stand-in for the use case: keeps the stored documents, records what the boundary handed over. */
    private static final class RecordingManage implements KnowledgeDocumentManage {

        private final Map<Long, KnowledgeDocumentBO> documents = new LinkedHashMap<>();

        private KnowledgeApplicationException failure;
        private UploadKnowledgeDocumentCommand uploaded;
        private Long reingested;
        private Long deleted;
        private int pageNo;
        private int pageSize;
        private DocumentIngestStatusEnum filteredStatus;
        private String searched;

        private RecordingManage(KnowledgeDocumentBO document) {
            documents.put(document.documentId(), document);
        }

        @Override
        public KnowledgeDocumentBO upload(UploadKnowledgeDocumentCommand command) {
            uploaded = command;
            throwIfFailed();
            KnowledgeDocumentBO stored = new KnowledgeDocumentBO(DOCUMENT_ID, 0L, command.knowledgeBaseId(),
                    command.effectiveDisplayName(), command.fileName(), command.mimeType(),
                    command.content().length, CONTENT_HASH, "LOCAL", "7/original/report.txt",
                    new String(command.content(), StandardCharsets.UTF_8), DocumentIngestStatusEnum.PENDING,
                    0, 0, null, null, NOW, NOW);
            documents.put(DOCUMENT_ID, stored);
            return stored;
        }

        @Override
        public KnowledgeDocumentBO get(Long documentId) {
            throwIfFailed();
            return required(documentId);
        }

        @Override
        public PageResultRecord<KnowledgeDocumentBO> page(Long knowledgeBaseId, int page, int size,
                                                          DocumentIngestStatusEnum status, String keyword) {
            throwIfFailed();
            pageNo = page;
            pageSize = size;
            filteredStatus = status;
            searched = keyword;
            return PageResultRecord.success(List.copyOf(documents.values()), documents.size(), page, size);
        }

        @Override
        public KnowledgeDocumentBO reingest(Long documentId) {
            reingested = documentId;
            throwIfFailed();
            KnowledgeDocumentBO current = required(documentId);
            KnowledgeDocumentBO reset = new KnowledgeDocumentBO(current.documentId(), current.tenantId(),
                    current.knowledgeBaseId(), current.displayName(), current.fileName(), current.mimeType(),
                    current.sizeBytes(), current.contentHash(), current.storageType(), current.storageKey(),
                    current.content(), DocumentIngestStatusEnum.PENDING, 0, 0, null, null,
                    current.createdAt(), NOW);
            documents.put(documentId, reset);
            return reset;
        }

        @Override
        public void delete(Long documentId) {
            deleted = documentId;
            throwIfFailed();
            documents.remove(documentId);
        }

        private KnowledgeDocumentBO required(Long documentId) {
            KnowledgeDocumentBO found = documentId == null ? null : documents.get(documentId);
            if (found == null) {
                throw new KnowledgeApplicationException(
                        KnowledgeErrorCodeEnum.KNOWLEDGE_DOCUMENT_NOT_FOUND, TRACE_ID);
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
