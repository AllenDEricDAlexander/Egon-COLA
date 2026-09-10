package top.egon.cola.archetype.source.agent.application.knowledge;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import top.egon.cola.archetype.source.agent.application.knowledge.command.AskKnowledgeBaseCommand;
import top.egon.cola.archetype.source.agent.application.knowledge.command.CreateKnowledgeBaseCommand;
import top.egon.cola.archetype.source.agent.application.knowledge.command.RetrieveKnowledgeCommand;
import top.egon.cola.archetype.source.agent.application.knowledge.command.UpdateKnowledgeBaseCommand;
import top.egon.cola.archetype.source.agent.application.knowledge.command.UploadKnowledgeDocumentCommand;
import top.egon.cola.archetype.source.agent.application.knowledge.config.KnowledgeRuntimeProperties;
import top.egon.cola.archetype.source.agent.application.knowledge.exception.KnowledgeApplicationException;
import top.egon.cola.archetype.source.agent.application.knowledge.manage.impl.KnowledgeBaseManageImpl;
import top.egon.cola.archetype.source.agent.application.knowledge.manage.impl.KnowledgeDocumentManageImpl;
import top.egon.cola.archetype.source.agent.application.knowledge.manage.impl.KnowledgeQaManageImpl;
import top.egon.cola.archetype.source.agent.application.knowledge.service.KnowledgeIngestQueueService;
import top.egon.cola.archetype.source.agent.application.knowledge.service.KnowledgeQaCapacityService;
import top.egon.cola.archetype.source.agent.application.knowledge.service.KnowledgeRemovalService;
import top.egon.cola.archetype.source.agent.common.error.KnowledgeErrorCodeEnum;
import top.egon.cola.archetype.source.agent.common.knowledge.KnowledgeIngestChannel;
import top.egon.cola.archetype.source.agent.domain.knowledge.gateway.KnowledgeAnswerGateway;
import top.egon.cola.archetype.source.agent.domain.knowledge.gateway.KnowledgeVectorGateway;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.ChunkingStrategyEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.DocumentIngestStatusEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeAnswerTaskBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeBaseBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeChunkBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeDocumentBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeQaEvent;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeRetrievedChunkBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.repository.KnowledgeBaseRepository;
import top.egon.cola.archetype.source.agent.domain.knowledge.repository.KnowledgeDocumentRepository;
import top.egon.cola.archetype.source.agent.domain.knowledge.service.KnowledgeAnswerRunService;
import top.egon.cola.archetype.source.agent.domain.knowledge.service.KnowledgeQaEventObserverService;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.OutboxReceipt;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.component.rag.api.RagExtractionService;
import top.egon.cola.component.rag.exception.RagExtractionException;
import top.egon.cola.component.rag.extract.RagDocumentExtractor;
import top.egon.cola.component.rag.extract.RagDocumentExtractorRegistry;
import top.egon.cola.component.rag.model.ExtractedDocumentBO;
import top.egon.cola.component.rag.model.RagExtractionCommand;
import top.egon.cola.component.rag.storage.RagDocumentStorage;
import top.egon.cola.component.rag.storage.RagDocumentStorageTypeEnum;
import top.egon.cola.component.rag.storage.RagStoredObjectBO;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Use-case tests for the three knowledge manages, wired by hand in the style of
 * {@code DeepResearchManageImplTest}.
 *
 * <p>The upload transaction itself is a proxy concern and is not exercised here. What these tests
 * pin down is the contract around it: the row and the queue message are written through one
 * collaborator, a failing write leaves the stored original compensated and no message behind, and
 * the format routing and the parse both happen before either.
 */
class KnowledgeManageTest {

    private static final Instant NOW = Instant.parse("2026-09-10T06:00:00Z");

    private static final String TRACE_ID = "4e9d6938";

    private static final String FILE_NAME = "handbook.txt";

    private static final String MIME_TYPE = "text/plain";

    private static final String EMBEDDING_MODEL = "text-embedding-3-small";

    private static final String TEXT = "hello world";

    private static final Long TENANT_ID = 1L;

    private static ValidatorFactory validatorFactory;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void writes_document_and_outbox_in_one_transaction() {
        Fixture fixture = new Fixture();

        KnowledgeDocumentBO uploaded = fixture.documents()
                .upload(upload(fixture.createBase(), FILE_NAME, TEXT));

        assertEquals(DocumentIngestStatusEnum.PENDING, uploaded.status());
        assertEquals(TEXT, uploaded.content());
        assertEquals(TEXT.length(), uploaded.sizeBytes());
        assertEquals(TENANT_ID, uploaded.tenantId());
        assertEquals(1, fixture.documentStorage.stored.size());
        assertEquals(1, fixture.documentRepository.stored.size());
        assertEquals(1, fixture.outbox.messages.size());
        OutboxMessage message = fixture.outbox.messages.getFirst();
        assertEquals(KnowledgeIngestChannel.NAME, message.channel());
        assertEquals(KnowledgeIngestChannel.SCHEMA_VERSION, message.schemaVersion());
        assertEquals(String.valueOf(uploaded.documentId()), message.destination());
        assertEquals(Map.of(
                KnowledgeIngestChannel.SCHEMA_VERSION_FIELD, KnowledgeIngestChannel.SCHEMA_VERSION,
                KnowledgeIngestChannel.DOCUMENT_ID_FIELD, uploaded.documentId(),
                KnowledgeIngestChannel.TENANT_ID_FIELD, TENANT_ID), message.payload());
    }

    @Test
    void compensates_the_stored_file_when_the_transaction_fails() {
        Fixture fixture = new Fixture();
        Long knowledgeBaseId = fixture.createBase();
        fixture.documentRepository.failInsert = true;

        KnowledgeApplicationException failure = assertThrows(KnowledgeApplicationException.class,
                () -> fixture.documents().upload(upload(knowledgeBaseId, FILE_NAME, TEXT)));

        assertEquals(KnowledgeErrorCodeEnum.KNOWLEDGE_INTERNAL_ERROR, failure.code());
        assertEquals(1, fixture.documentStorage.deleted.size());
        assertTrue(fixture.documentStorage.stored.isEmpty());
        assertTrue(fixture.documentRepository.stored.isEmpty());
        assertTrue(fixture.outbox.messages.isEmpty());
    }

    @Test
    void rejects_reingest_while_processing() {
        Fixture fixture = new Fixture();
        Long knowledgeBaseId = fixture.createBase();
        KnowledgeDocumentBO document = fixture.documentRepository
                .save(document(knowledgeBaseId, DocumentIngestStatusEnum.PROCESSING, TEXT));

        KnowledgeApplicationException busy = assertThrows(KnowledgeApplicationException.class,
                () -> fixture.documents().reingest(document.documentId()));

        assertEquals(KnowledgeErrorCodeEnum.KNOWLEDGE_DOCUMENT_BUSY, busy.code());
        assertEquals(DocumentIngestStatusEnum.PROCESSING,
                fixture.documentRepository.stored.get(document.documentId()).status());
        assertTrue(fixture.outbox.messages.isEmpty());
    }

    @Test
    void rejects_upload_when_capacity_is_reached() {
        Fixture fixture = new Fixture();
        fixture.maxDocumentsPerBase = 2;
        Long knowledgeBaseId = fixture.createBase();
        fixture.documentRepository.save(document(knowledgeBaseId, DocumentIngestStatusEnum.SUCCEEDED, TEXT));
        fixture.documentRepository.save(document(knowledgeBaseId, DocumentIngestStatusEnum.SUCCEEDED, TEXT));

        KnowledgeApplicationException exhausted = assertThrows(KnowledgeApplicationException.class,
                () -> fixture.documents().upload(upload(knowledgeBaseId, FILE_NAME, TEXT)));

        assertEquals(KnowledgeErrorCodeEnum.KNOWLEDGE_CAPACITY_EXHAUSTED, exhausted.code());
        assertTrue(fixture.documentStorage.stored.isEmpty());
        assertEquals(2, fixture.documentRepository.stored.size());
    }

    @Test
    void rejects_an_upload_of_an_unroutable_format_before_writing_anything() {
        Fixture fixture = new Fixture();
        Long knowledgeBaseId = fixture.createBase();

        KnowledgeApplicationException missing = assertThrows(KnowledgeApplicationException.class,
                () -> fixture.documents().upload(new UploadKnowledgeDocumentCommand(knowledgeBaseId,
                        "handbook.pdf", "application/pdf", null, TEXT.getBytes(StandardCharsets.UTF_8),
                        TRACE_ID)));

        assertEquals(KnowledgeErrorCodeEnum.KNOWLEDGE_EXTRACTOR_MISSING, missing.code());
        assertTrue(missing.fieldErrors().containsKey("file"));
        assertTrue(fixture.documentStorage.stored.isEmpty());
        assertTrue(fixture.documentRepository.stored.isEmpty());
        assertTrue(fixture.outbox.messages.isEmpty());
    }

    @Test
    void rejects_an_upload_above_the_size_limit_before_writing_anything() {
        Fixture fixture = new Fixture();
        fixture.maxUploadBytes = TEXT.length() - 1;
        Long knowledgeBaseId = fixture.createBase();

        KnowledgeApplicationException tooLarge = assertThrows(KnowledgeApplicationException.class,
                () -> fixture.documents().upload(upload(knowledgeBaseId, FILE_NAME, TEXT)));

        assertEquals(KnowledgeErrorCodeEnum.KNOWLEDGE_FILE_TOO_LARGE, tooLarge.code());
        assertTrue(fixture.documentStorage.stored.isEmpty());
        assertTrue(fixture.documentRepository.stored.isEmpty());
        assertTrue(fixture.outbox.messages.isEmpty());
    }

    @Test
    void compensates_the_stored_file_when_the_parse_fails() {
        Fixture fixture = new Fixture();
        Long knowledgeBaseId = fixture.createBase();
        fixture.textExtractor.failOnExtract = true;

        KnowledgeApplicationException failure = assertThrows(KnowledgeApplicationException.class,
                () -> fixture.documents().upload(upload(knowledgeBaseId, FILE_NAME, TEXT)));

        assertEquals(KnowledgeErrorCodeEnum.KNOWLEDGE_INTERNAL_ERROR, failure.code());
        assertEquals(1, fixture.documentStorage.deleted.size());
        assertTrue(fixture.documentStorage.stored.isEmpty());
        assertTrue(fixture.documentRepository.stored.isEmpty());
        assertTrue(fixture.outbox.messages.isEmpty());
    }

    @Test
    void requeues_a_terminal_document_and_refuses_one_without_stored_text() {
        Fixture fixture = new Fixture();
        Long knowledgeBaseId = fixture.createBase();
        KnowledgeDocumentBO failed = fixture.documentRepository
                .save(document(knowledgeBaseId, DocumentIngestStatusEnum.FAILED, TEXT));
        KnowledgeDocumentBO withoutText = fixture.documentRepository
                .save(document(knowledgeBaseId, DocumentIngestStatusEnum.DEAD, null));

        KnowledgeDocumentBO requeued = fixture.documents().reingest(failed.documentId());

        assertEquals(DocumentIngestStatusEnum.PENDING, requeued.status());
        assertEquals(1, fixture.outbox.messages.size());

        KnowledgeApplicationException missing = assertThrows(KnowledgeApplicationException.class,
                () -> fixture.documents().reingest(withoutText.documentId()));
        assertEquals(KnowledgeErrorCodeEnum.KNOWLEDGE_CONTENT_MISSING, missing.code());
    }

    @Test
    void deletes_a_document_with_its_file_and_chunks() {
        Fixture fixture = new Fixture();
        Long knowledgeBaseId = fixture.createBase();
        KnowledgeDocumentBO document = fixture.documentRepository
                .save(document(knowledgeBaseId, DocumentIngestStatusEnum.SUCCEEDED, TEXT));

        fixture.documents().delete(document.documentId());

        assertTrue(fixture.documentRepository.stored.isEmpty());
        assertEquals(1, fixture.documentStorage.deleted.size());
        assertEquals(List.of(String.valueOf(document.documentId())), fixture.vectorGateway.deleted);
    }

    @Test
    void refuses_to_delete_a_base_that_still_has_a_document_in_progress() {
        Fixture fixture = new Fixture();
        Long knowledgeBaseId = fixture.createBase();
        fixture.documentRepository.save(document(knowledgeBaseId, DocumentIngestStatusEnum.PENDING, TEXT));

        KnowledgeApplicationException busy = assertThrows(KnowledgeApplicationException.class,
                () -> fixture.bases().delete(knowledgeBaseId));

        assertEquals(KnowledgeErrorCodeEnum.KNOWLEDGE_BASE_BUSY, busy.code());
        assertTrue(fixture.baseRepository.findById(knowledgeBaseId).isPresent());
        assertTrue(fixture.documentStorage.deleted.isEmpty());
    }

    @Test
    void deletes_a_base_with_its_documents_files_and_chunks() {
        Fixture fixture = new Fixture();
        Long knowledgeBaseId = fixture.createBase();
        KnowledgeDocumentBO first = fixture.documentRepository
                .save(document(knowledgeBaseId, DocumentIngestStatusEnum.SUCCEEDED, TEXT));
        KnowledgeDocumentBO second = fixture.documentRepository
                .save(document(knowledgeBaseId, DocumentIngestStatusEnum.DEAD, TEXT));

        fixture.bases().delete(knowledgeBaseId);

        assertTrue(fixture.baseRepository.findById(knowledgeBaseId).isEmpty());
        assertTrue(fixture.documentRepository.stored.isEmpty());
        assertEquals(2, fixture.documentStorage.deleted.size());
        assertEquals(List.of(String.valueOf(first.documentId()), String.valueOf(second.documentId())),
                fixture.vectorGateway.deleted);
    }

    @Test
    void rejects_an_update_that_carries_a_create_only_field() {
        Fixture fixture = new Fixture();
        Long knowledgeBaseId = fixture.createBase();

        KnowledgeApplicationException immutable = assertThrows(KnowledgeApplicationException.class,
                () -> fixture.bases().update(new UpdateKnowledgeBaseCommand(knowledgeBaseId, "renamed", null,
                        null, "another-model", null, null, TRACE_ID)));

        assertEquals(KnowledgeErrorCodeEnum.KNOWLEDGE_IMMUTABLE_FIELD, immutable.code());
        assertTrue(immutable.fieldErrors().containsKey("embeddingModel"));
        assertEquals("Handbook", fixture.baseRepository.findById(knowledgeBaseId).orElseThrow().name());
    }

    @Test
    void retrieves_scored_chunks_with_their_display_names() {
        Fixture fixture = new Fixture();
        Long knowledgeBaseId = fixture.createBase();
        KnowledgeDocumentBO document = fixture.documentRepository
                .save(document(knowledgeBaseId, DocumentIngestStatusEnum.SUCCEEDED, TEXT));
        fixture.vectorGateway.chunks = List.of(
                new KnowledgeChunkBO(document.documentId(), 0, "hello", 0.9d),
                new KnowledgeChunkBO(document.documentId(), 1, "world", null));

        var retrieval = fixture.qa()
                .retrieve(new RetrieveKnowledgeCommand(knowledgeBaseId, "hello world", 5, null, TRACE_ID));

        assertEquals(EMBEDDING_MODEL, retrieval.embeddingModel());
        assertEquals(List.of("hello", "world"),
                retrieval.items().stream().map(KnowledgeRetrievedChunkBO::content).toList());
        assertEquals(List.of(FILE_NAME, FILE_NAME),
                retrieval.items().stream().map(KnowledgeRetrievedChunkBO::displayName).toList());
        assertEquals(Arrays.asList(0.9d, null),
                retrieval.items().stream().map(KnowledgeRetrievedChunkBO::score).toList());
        assertEquals(5, fixture.vectorGateway.topK);
        assertTrue(fixture.vectorGateway.attributes.isEmpty());
    }

    @Test
    void drops_chunks_below_the_score_floor_and_keeps_unknown_scores_without_one() {
        Fixture fixture = new Fixture();
        Long knowledgeBaseId = fixture.createBase();
        KnowledgeDocumentBO document = fixture.documentRepository
                .save(document(knowledgeBaseId, DocumentIngestStatusEnum.SUCCEEDED, TEXT));
        fixture.vectorGateway.chunks = List.of(
                new KnowledgeChunkBO(document.documentId(), 0, "high", 0.9d),
                new KnowledgeChunkBO(document.documentId(), 1, "low", 0.2d),
                new KnowledgeChunkBO(document.documentId(), 2, "unknown", null));

        var floored = fixture.qa()
                .retrieve(new RetrieveKnowledgeCommand(knowledgeBaseId, "hello", null, 0.5d, TRACE_ID));
        var unfloored = fixture.qa()
                .retrieve(new RetrieveKnowledgeCommand(knowledgeBaseId, "hello", null, null, TRACE_ID));

        assertEquals(List.of("high"), floored.items().stream().map(KnowledgeRetrievedChunkBO::content).toList());
        assertEquals(List.of("high", "low", "unknown"),
                unfloored.items().stream().map(KnowledgeRetrievedChunkBO::content).toList());
        assertEquals(0, fixture.vectorGateway.topK);
    }

    @Test
    void streams_an_answer_and_releases_the_permit_on_the_terminal_event() {
        Fixture fixture = new Fixture();
        Long knowledgeBaseId = fixture.createBase();
        KnowledgeDocumentBO document = fixture.documentRepository
                .save(document(knowledgeBaseId, DocumentIngestStatusEnum.SUCCEEDED, TEXT));
        fixture.vectorGateway.chunks = List.of(new KnowledgeChunkBO(document.documentId(), 0, "hello", 0.9d));
        List<KnowledgeQaEvent> received = new ArrayList<>();

        fixture.qa().ask(new AskKnowledgeBaseCommand(knowledgeBaseId, "question", null, TRACE_ID), received::add);

        assertEquals(1, fixture.answerGateway.tasks.size());
        KnowledgeAnswerTaskBO task = fixture.answerGateway.tasks.getFirst();
        assertEquals("question", task.question());
        assertEquals(EMBEDDING_MODEL, task.logicalModelName());
        // The model answers from the chunk text, so the task carries it even though the events do not.
        assertEquals(List.of("hello"),
                task.references().stream().map(KnowledgeRetrievedChunkBO::content).toList());
        assertEquals(3, fixture.capacity.availablePermits());

        fixture.answerGateway.emit(KnowledgeQaEvent.started(task.answerId(), 1,
                task.references().stream().map(KnowledgeRetrievedChunkBO::withoutContent).toList(), NOW, TRACE_ID));
        fixture.answerGateway.emit(KnowledgeQaEvent.progress(task.answerId(), 2, "an ", NOW));
        fixture.answerGateway.emit(KnowledgeQaEvent.completed(task.answerId(), 3, "an answer", List.of(), NOW,
                TRACE_ID));
        fixture.answerGateway.emit(KnowledgeQaEvent.failed(task.answerId(), 4,
                KnowledgeErrorCodeEnum.KNOWLEDGE_INTERNAL_ERROR, NOW, TRACE_ID));

        assertEquals(3, received.size());
        assertEquals("COMPLETED", received.getLast().type().name());
        assertEquals(4, fixture.capacity.availablePermits());
    }

    @Test
    void refuses_a_question_when_every_permit_is_taken() {
        Fixture fixture = new Fixture(1);
        Long knowledgeBaseId = fixture.createBase();

        fixture.qa().ask(new AskKnowledgeBaseCommand(knowledgeBaseId, "first", null, TRACE_ID), event -> { });
        KnowledgeApplicationException exhausted = assertThrows(KnowledgeApplicationException.class,
                () -> fixture.qa().ask(new AskKnowledgeBaseCommand(knowledgeBaseId, "second", null, TRACE_ID),
                        event -> { }));

        assertEquals(KnowledgeErrorCodeEnum.KNOWLEDGE_CAPACITY_EXHAUSTED, exhausted.code());
        assertEquals(0, fixture.capacity.availablePermits());
        assertEquals(1, fixture.answerGateway.tasks.size());
    }

    @Test
    void releases_the_permit_once_when_the_caller_cancels() {
        Fixture fixture = new Fixture(1);
        Long knowledgeBaseId = fixture.createBase();

        KnowledgeAnswerRunService run = fixture.qa()
                .ask(new AskKnowledgeBaseCommand(knowledgeBaseId, "cancel", null, TRACE_ID), event -> { });
        assertEquals(0, fixture.capacity.availablePermits());

        run.cancel();
        run.cancel();

        assertEquals(1, fixture.answerGateway.cancellations);
        assertEquals(1, fixture.capacity.availablePermits());
    }

    @Test
    void releases_the_permit_when_the_observer_fails() {
        Fixture fixture = new Fixture(1);
        Long knowledgeBaseId = fixture.createBase();

        fixture.qa().ask(new AskKnowledgeBaseCommand(knowledgeBaseId, "observer-failure", null, TRACE_ID),
                event -> {
                    throw new IllegalStateException("the observer is gone");
                });

        String answerId = fixture.answerGateway.tasks.getFirst().answerId();
        assertThrows(IllegalStateException.class,
                () -> fixture.answerGateway.emit(KnowledgeQaEvent.progress(answerId, 2, "delta", NOW)));

        assertEquals(1, fixture.answerGateway.cancellations);
        assertEquals(1, fixture.capacity.availablePermits());
    }

    @Test
    void fails_the_question_with_a_dependency_error_when_the_gateway_refuses_before_the_stream() {
        Fixture fixture = new Fixture(1);
        Long knowledgeBaseId = fixture.createBase();
        fixture.answerGateway.failOnGenerate = true;

        KnowledgeApplicationException failure = assertThrows(KnowledgeApplicationException.class,
                () -> fixture.qa().ask(new AskKnowledgeBaseCommand(knowledgeBaseId, "question", null, TRACE_ID),
                        event -> { }));

        assertEquals(KnowledgeErrorCodeEnum.KNOWLEDGE_DEPENDENCY_UNAVAILABLE, failure.code());
        assertEquals(1, fixture.capacity.availablePermits());
    }

    /** One knowledge base's worth of use cases over hand-written fakes and their recorded calls. */
    private static final class Fixture {

        private final FakeKnowledgeBaseRepository baseRepository = new FakeKnowledgeBaseRepository();

        private final FakeKnowledgeDocumentRepository documentRepository = new FakeKnowledgeDocumentRepository();

        private final RecordingVectorGateway vectorGateway = new RecordingVectorGateway();

        private final RecordingAnswerGateway answerGateway = new RecordingAnswerGateway();

        private final RecordingOutbox outbox = new RecordingOutbox();

        private final FakeDocumentStorage documentStorage = new FakeDocumentStorage();

        private final FakeTextExtractor textExtractor = new FakeTextExtractor();

        private final RagDocumentExtractorRegistry extractors =
                new RagDocumentExtractorRegistry(List.of(textExtractor));

        private final KnowledgeQaCapacityService capacity;

        private final KnowledgeIngestQueueService ingestQueueService;

        private final KnowledgeRemovalService removalService;

        private final int qaMaxConcurrent;

        private int maxUploadBytes = 1024;

        private int maxDocumentsPerBase = 10;

        private Fixture() {
            this(4);
        }

        private Fixture(int qaMaxConcurrent) {
            this.qaMaxConcurrent = qaMaxConcurrent;
            this.capacity = new KnowledgeQaCapacityService(qaMaxConcurrent);
            this.ingestQueueService = new KnowledgeIngestQueueService(documentRepository, outbox);
            this.removalService = new KnowledgeRemovalService(vectorGateway, documentRepository, baseRepository,
                    documentStorage);
        }

        private KnowledgeBaseManageImpl bases() {
            return new KnowledgeBaseManageImpl(baseRepository, documentRepository, removalService, properties(),
                    validationUtils(), () -> TENANT_ID);
        }

        private KnowledgeDocumentManageImpl documents() {
            return new KnowledgeDocumentManageImpl(baseRepository, documentRepository, ingestQueueService,
                    removalService, documentStorage, new RoutingExtractionService(extractors), extractors,
                    properties(), validationUtils(), () -> TENANT_ID);
        }

        private KnowledgeQaManageImpl qa() {
            return new KnowledgeQaManageImpl(baseRepository, documentRepository, vectorGateway, answerGateway,
                    capacity, validationUtils(), Clock.fixed(NOW, ZoneOffset.UTC));
        }

        private Long createBase() {
            return bases().create(new CreateKnowledgeBaseCommand("handbook", "Handbook", null, EMBEDDING_MODEL,
                    ChunkingStrategyEnum.TOKEN,
                    new CreateKnowledgeBaseCommand.ChunkingConfigCommand(128, 16, 32, List.of()),
                    TRACE_ID)).knowledgeBaseId();
        }

        private KnowledgeRuntimeProperties properties() {
            return new KnowledgeRuntimeProperties(
                    new KnowledgeRuntimeProperties.Runtime(qaMaxConcurrent, maxUploadBytes, maxDocumentsPerBase,
                            Duration.ofMinutes(2)),
                    new KnowledgeRuntimeProperties.Tenant(TENANT_ID));
        }

        private static ValidationUtils validationUtils() {
            return new ValidationUtils(validatorFactory.getValidator());
        }
    }

    private static UploadKnowledgeDocumentCommand upload(Long knowledgeBaseId, String fileName, String text) {
        return new UploadKnowledgeDocumentCommand(knowledgeBaseId, fileName, MIME_TYPE, null,
                text.getBytes(StandardCharsets.UTF_8), TRACE_ID);
    }

    private static KnowledgeDocumentBO document(Long knowledgeBaseId, DocumentIngestStatusEnum status,
                                                String content) {
        return new KnowledgeDocumentBO(null, TENANT_ID, knowledgeBaseId, FILE_NAME, FILE_NAME, MIME_TYPE,
                content == null ? 0L : content.length(), "0".repeat(64),
                RagDocumentStorageTypeEnum.LOCAL.name(), storageKey(knowledgeBaseId), content,
                status, 0, 0, null, null, NOW, NOW);
    }

    /** The key shape the upload composes: base, stored object and file name. */
    private static String storageKey(Long knowledgeBaseId) {
        return knowledgeBaseId + "/3f1c0d02-0000-0000-0000-000000000000/" + FILE_NAME;
    }

    private static final class FakeKnowledgeBaseRepository implements KnowledgeBaseRepository {

        private final Map<Long, KnowledgeBaseBO> stored = new LinkedHashMap<>();

        private final AtomicLong nextId = new AtomicLong();

        @Override
        public KnowledgeBaseBO insert(KnowledgeBaseBO knowledgeBase) {
            long id = nextId.incrementAndGet();
            KnowledgeBaseBO saved = new KnowledgeBaseBO(id, knowledgeBase.tenantId(), knowledgeBase.code(),
                    knowledgeBase.name(), knowledgeBase.description(), knowledgeBase.embeddingModel(),
                    knowledgeBase.chunkStrategy(), knowledgeBase.chunkConfig(), knowledgeBase.status(), NOW, NOW);
            stored.put(id, saved);
            return saved;
        }

        @Override
        public Optional<KnowledgeBaseBO> findById(Long knowledgeBaseId) {
            return Optional.ofNullable(stored.get(knowledgeBaseId));
        }

        @Override
        public List<KnowledgeBaseBO> page(int offset, int size, String keyword, String embeddingModel) {
            return stored.values().stream().skip(offset).limit(size).toList();
        }

        @Override
        public long count(String keyword, String embeddingModel) {
            return stored.size();
        }

        @Override
        public void updateNameAndDescription(Long knowledgeBaseId, String name, String description) {
            KnowledgeBaseBO current = stored.get(knowledgeBaseId);
            stored.put(knowledgeBaseId, new KnowledgeBaseBO(current.knowledgeBaseId(), current.tenantId(),
                    current.code(), name, description, current.embeddingModel(), current.chunkStrategy(),
                    current.chunkConfig(), current.status(), current.createdAt(), NOW));
        }

        @Override
        public void softDelete(Long knowledgeBaseId) {
            stored.remove(knowledgeBaseId);
        }
    }

    private static final class FakeKnowledgeDocumentRepository implements KnowledgeDocumentRepository {

        private final Map<Long, KnowledgeDocumentBO> stored = new LinkedHashMap<>();

        private final AtomicLong nextId = new AtomicLong();

        private boolean failInsert;

        private KnowledgeDocumentBO save(KnowledgeDocumentBO document) {
            long id = nextId.incrementAndGet();
            KnowledgeDocumentBO saved = new KnowledgeDocumentBO(id, document.tenantId(), document.knowledgeBaseId(),
                    document.displayName(), document.fileName(), document.mimeType(), document.sizeBytes(),
                    document.contentHash(), document.storageType(), document.storageKey(), document.content(),
                    document.status(), document.chunkCount(), document.attemptCount(), document.errorCode(),
                    document.errorMessage(), NOW, NOW);
            stored.put(id, saved);
            return saved;
        }

        @Override
        public KnowledgeDocumentBO insert(KnowledgeDocumentBO document) {
            if (failInsert) {
                throw new IllegalStateException("the document insert failed");
            }
            return save(document);
        }

        @Override
        public Optional<KnowledgeDocumentBO> findById(Long documentId) {
            return Optional.ofNullable(stored.get(documentId));
        }

        @Override
        public List<KnowledgeDocumentBO> findByIds(Collection<Long> documentIds) {
            return stored.values().stream().filter(document -> documentIds.contains(document.documentId())).toList();
        }

        @Override
        public List<KnowledgeDocumentBO> page(Long knowledgeBaseId, int offset, int size,
                                              DocumentIngestStatusEnum status, String keyword) {
            return filtered(knowledgeBaseId, status).stream().skip(offset).limit(size).toList();
        }

        @Override
        public long count(Long knowledgeBaseId, DocumentIngestStatusEnum status, String keyword) {
            return filtered(knowledgeBaseId, status).size();
        }

        @Override
        public long countByKnowledgeBaseId(Long knowledgeBaseId) {
            return filtered(knowledgeBaseId, null).size();
        }

        @Override
        public boolean markProcessing(Long documentId, int attemptCount) {
            return false;
        }

        @Override
        public boolean markSucceeded(Long documentId, int chunkCount) {
            return false;
        }

        @Override
        public boolean markRetryPending(Long documentId, int attemptCount, String errorCode, String errorMessage) {
            return false;
        }

        @Override
        public boolean markDead(Long documentId, String errorCode, String errorMessage) {
            return false;
        }

        @Override
        public boolean resetForReingest(Long documentId) {
            KnowledgeDocumentBO current = stored.get(documentId);
            if (current == null || !current.status().isTerminal()) {
                return false;
            }
            stored.put(documentId, new KnowledgeDocumentBO(current.documentId(), current.tenantId(),
                    current.knowledgeBaseId(), current.displayName(), current.fileName(), current.mimeType(),
                    current.sizeBytes(), current.contentHash(), current.storageType(), current.storageKey(),
                    current.content(), DocumentIngestStatusEnum.PENDING, 0, 0, null, null, current.createdAt(),
                    NOW));
            return true;
        }

        @Override
        public void softDelete(Long documentId) {
            stored.remove(documentId);
        }

        @Override
        public void softDeleteByKnowledgeBaseId(Long knowledgeBaseId) {
            stored.values().removeIf(document -> document.knowledgeBaseId().equals(knowledgeBaseId));
        }

        private List<KnowledgeDocumentBO> filtered(Long knowledgeBaseId, DocumentIngestStatusEnum status) {
            return stored.values().stream()
                    .filter(document -> document.knowledgeBaseId().equals(knowledgeBaseId))
                    .filter(document -> status == null || document.status() == status)
                    .toList();
        }
    }

    private static final class RecordingVectorGateway implements KnowledgeVectorGateway {

        private final List<String> deleted = new ArrayList<>();

        private List<KnowledgeChunkBO> chunks = List.of();

        private int topK = -1;

        private Map<String, String> attributes = Map.of();

        @Override
        public List<KnowledgeChunkBO> retrieve(String collectionId, String logicalModelName, String query,
                                               int topK, Map<String, String> attributes) {
            this.topK = topK;
            this.attributes = attributes;
            return chunks;
        }

        @Override
        public void deleteDocument(String collectionId, String documentId) {
            deleted.add(documentId);
        }
    }

    private static final class RecordingAnswerGateway implements KnowledgeAnswerGateway {

        private final List<KnowledgeAnswerTaskBO> tasks = new ArrayList<>();

        private final List<KnowledgeQaEventObserverService> observers = new ArrayList<>();

        private int cancellations;

        private boolean failOnGenerate;

        @Override
        public KnowledgeAnswerRunService generate(KnowledgeAnswerTaskBO task,
                                                  KnowledgeQaEventObserverService observer) {
            if (failOnGenerate) {
                throw new IllegalStateException("the model is unavailable");
            }
            tasks.add(task);
            observers.add(observer);
            return () -> cancellations++;
        }

        private void emit(KnowledgeQaEvent event) {
            observers.getLast().onEvent(event);
        }
    }

    private static final class RecordingOutbox implements TransactionalOutbox {

        private final List<OutboxMessage> messages = new ArrayList<>();

        @Override
        public OutboxReceipt enqueue(OutboxMessage message) {
            messages.add(message);
            return new OutboxReceipt("message-" + messages.size(), "idempotency-" + messages.size(), true);
        }
    }

    /**
     * In-memory storage that reads the stream the way a real implementation does: the bytes are
     * only ever available once, so a use case that reads them twice would see an empty document.
     */
    private static final class FakeDocumentStorage implements RagDocumentStorage {

        private final Map<String, byte[]> stored = new LinkedHashMap<>();

        private final List<String> deleted = new ArrayList<>();

        @Override
        public RagDocumentStorageTypeEnum type() {
            return RagDocumentStorageTypeEnum.LOCAL;
        }

        @Override
        public RagStoredObjectBO store(String collectionId, String documentId, String fileName,
                                       InputStream content) {
            byte[] bytes = readAll(content);
            stored.put(key(collectionId, documentId), bytes);
            return new RagStoredObjectBO(collectionId, documentId, RagDocumentStorageTypeEnum.LOCAL, bytes.length,
                    NOW);
        }

        @Override
        public InputStream open(String collectionId, String documentId) {
            byte[] bytes = stored.get(key(collectionId, documentId));
            if (bytes == null) {
                throw new IllegalStateException("no object stored for " + documentId);
            }
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void delete(String collectionId, String documentId) {
            stored.remove(key(collectionId, documentId));
            deleted.add(documentId);
        }

        private static String key(String collectionId, String documentId) {
            return collectionId + "/" + documentId;
        }

        private static byte[] readAll(InputStream content) {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                content.transferTo(bytes);
                return bytes.toByteArray();
            } catch (IOException failure) {
                throw new UncheckedIOException(failure);
            }
        }
    }

    /** A plain text extractor: the only format this fixture routes. */
    private static final class FakeTextExtractor implements RagDocumentExtractor {

        private boolean failOnExtract;

        @Override
        public boolean supports(String mimeType, String fileName) {
            return MIME_TYPE.equals(mimeType);
        }

        @Override
        public ExtractedDocumentBO extract(InputStream content, String mimeType, String fileName) {
            if (failOnExtract) {
                throw new RagExtractionException("the document could not be parsed");
            }
            return new ExtractedDocumentBO(
                    new String(FakeDocumentStorage.readAll(content), StandardCharsets.UTF_8), fileName, mimeType,
                    Map.of());
        }

        @Override
        public Set<String> declaredMimeTypes() {
            return Set.of(MIME_TYPE);
        }
    }

    /** The component's routing, stood in for so the use case talks to an ordinary service object. */
    private record RoutingExtractionService(RagDocumentExtractorRegistry registry) implements RagExtractionService {

        @Override
        public ExtractedDocumentBO extract(RagExtractionCommand command) {
            return registry.route(command.mimeType(), command.fileName())
                    .extract(command.content(), command.mimeType(), command.fileName());
        }
    }
}
