package top.egon.cola.archetype.source.agent.infrastructure.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.ChunkingStrategyEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.DocumentIngestStatusEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeBaseBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeBaseStatusEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeChunkConfigBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeDocumentBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.repository.KnowledgeBaseRepository;
import top.egon.cola.archetype.source.agent.domain.knowledge.repository.KnowledgeDocumentRepository;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.handler.KnowledgeIngestDeliveryHandler;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.metadata.KnowledgeVectorMetadata;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import top.egon.cola.component.rag.api.RagIngestionService;
import top.egon.cola.component.rag.chunk.RagChunkingStrategyEnum;
import top.egon.cola.component.rag.exception.RagModelNotRegisteredException;
import top.egon.cola.component.rag.exception.RagValidationException;
import top.egon.cola.component.rag.exception.RagVectorStoreException;
import top.egon.cola.component.rag.model.RagIngestionCommand;
import top.egon.cola.component.rag.model.RagIngestionResult;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Locks the asynchronous half of ingestion: the tenant is rebuilt from the payload before any data
 * access, the document is rebuilt from its stored text instead of being extracted again, and every
 * delivery writes a status the next attempt can act on.
 *
 * <p>Everything runs offline: the repositories and the ingestion service are doubles, and the
 * delivery thread starts without a caller's MDC context — which is exactly the situation the
 * handler exists to repair.
 */
class KnowledgeIngestDeliveryHandlerTest {

    private static final long TENANT = 42L;

    private static final long DOCUMENT_ID = 2001L;

    private static final long KNOWLEDGE_BASE_ID = 1001L;

    private static final String LOGICAL_MODEL_NAME = "openai-compatible";

    private static final String CONTENT = "the stored text of the document";

    private static final Instant STAMP = Instant.parse("2026-09-10T05:30:00Z");

    private static final EgonColaMybatisPlusProperties MYBATIS_PLUS_PROPERTIES =
            new EgonColaMybatisPlusProperties();

    private final KnowledgeDocumentRepository documentRepository = mock(KnowledgeDocumentRepository.class);

    private final KnowledgeBaseRepository knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);

    private final RagIngestionService ragIngestionService = mock(RagIngestionService.class);

    private final KnowledgeIngestDeliveryHandler handler = new KnowledgeIngestDeliveryHandler(
            documentRepository, knowledgeBaseRepository, ragIngestionService,
            MYBATIS_PLUS_PROPERTIES, new ObjectMapper(), Clock.systemUTC());

    @AfterEach
    void clearsMdc() {
        MDC.clear();
    }

    @Test
    void rebuilds_the_document_without_reparsing() {
        givenVisibleDocument();
        given(documentRepository.markProcessing(DOCUMENT_ID, 1)).willReturn(true);
        given(ragIngestionService.ingest(any())).willReturn(ingestionResult(7));

        DeliveryResult result = handler.deliver(delivery(payload(TENANT, DOCUMENT_ID), 1, 5));

        assertThat(result.kind()).isEqualTo(DeliveryResult.Kind.SUCCESS);
        ArgumentCaptor<RagIngestionCommand> command = ArgumentCaptor.forClass(RagIngestionCommand.class);
        verify(ragIngestionService).ingest(command.capture());
        assertThat(command.getValue().collectionId()).isEqualTo(String.valueOf(KNOWLEDGE_BASE_ID));
        assertThat(command.getValue().documentId()).isEqualTo(String.valueOf(DOCUMENT_ID));
        assertThat(command.getValue().logicalModelName()).isEqualTo(LOGICAL_MODEL_NAME);
        // The text comes from the stored row and nothing on this path reads the original file, so a
        // retried delivery re-chunks and re-embeds without extracting again.
        assertThat(command.getValue().document().text()).isEqualTo(CONTENT);
        assertThat(command.getValue().document().mimeType()).isEqualTo("text/plain");
        assertThat(command.getValue().chunkingConfig().strategy())
                .isEqualTo(RagChunkingStrategyEnum.RECURSIVE);
        assertThat(command.getValue().chunkingConfig().maxTokensPerChunk()).isEqualTo(256);
        // One call provides both copies of the tenant: the row was read under it, and the chunk
        // metadata carries it for the retrieval filter.
        assertThat(command.getValue().attributes())
                .containsEntry(KnowledgeVectorMetadata.TENANT_ID, String.valueOf(TENANT));
        verify(documentRepository).markSucceeded(DOCUMENT_ID, 7);
    }

    @Test
    void maps_delivery_failures_to_document_status() {
        givenVisibleDocument();
        given(documentRepository.markProcessing(DOCUMENT_ID, 1)).willReturn(true);
        given(documentRepository.markProcessing(DOCUMENT_ID, 5)).willReturn(true);
        RagVectorStoreException unavailable = new RagVectorStoreException("vector store is unavailable");
        given(ragIngestionService.ingest(any())).willThrow(unavailable);

        assertThatThrownBy(() -> handler.deliver(delivery(payload(TENANT, DOCUMENT_ID), 1, 5)))
                .isSameAs(unavailable);
        verify(documentRepository).markRetryPending(DOCUMENT_ID, 1, "KNOWLEDGE_EMBEDDING_FAILED",
                "vector store is unavailable");
        verify(documentRepository, never()).markDead(anyLong(), any(), any());

        // The last attempt ends the document instead of returning it to PENDING, because no further
        // delivery will be made.
        assertThatThrownBy(() -> handler.deliver(delivery(payload(TENANT, DOCUMENT_ID), 5, 5)))
                .isSameAs(unavailable);
        verify(documentRepository).markDead(DOCUMENT_ID, "KNOWLEDGE_EMBEDDING_FAILED",
                "vector store is unavailable");
    }

    @Test
    void maps_component_codes_and_keeps_foreign_messages_out_of_the_document() {
        givenVisibleDocument();
        for (int attempt = 1; attempt <= 3; attempt++) {
            given(documentRepository.markProcessing(DOCUMENT_ID, attempt)).willReturn(true);
        }
        given(ragIngestionService.ingest(any()))
                .willThrow(new RagValidationException("chunkingConfig is invalid"))
                .willThrow(new RagModelNotRegisteredException("logical model is not registered"))
                .willThrow(new IllegalStateException("jdbc:postgresql://10.0.0.7 content=" + CONTENT));

        deliverExpectingFailure(1);
        verify(documentRepository).markRetryPending(DOCUMENT_ID, 1, "KNOWLEDGE_VALIDATION_ERROR",
                "chunkingConfig is invalid");
        deliverExpectingFailure(2);
        verify(documentRepository).markRetryPending(DOCUMENT_ID, 2, "KNOWLEDGE_MODEL_NOT_REGISTERED",
                "logical model is not registered");
        // A failure the component did not classify is stored as a fixed code and summary: both
        // fields are rendered to clients, so no arbitrary exception text may reach them.
        deliverExpectingFailure(3);
        verify(documentRepository).markRetryPending(DOCUMENT_ID, 3, "KNOWLEDGE_INTERNAL_ERROR",
                "document ingestion failed");
    }

    @Test
    void restores_and_clears_the_tenant_mdc() {
        String mdcKey = MYBATIS_PLUS_PROPERTIES.getTenantId().getMdcKey();
        assertThat(MDC.get(mdcKey)).isNull();
        List<String> tenantsSeenByRepository = new ArrayList<>();
        given(documentRepository.findById(DOCUMENT_ID)).willAnswer(invocation -> {
            tenantsSeenByRepository.add(MDC.get(mdcKey));
            return Optional.of(document());
        });
        given(knowledgeBaseRepository.findById(KNOWLEDGE_BASE_ID))
                .willReturn(Optional.of(knowledgeBase()));
        given(documentRepository.markProcessing(DOCUMENT_ID, 1)).willReturn(true);
        given(ragIngestionService.ingest(any())).willReturn(ingestionResult(3));

        handler.deliver(delivery(payload(TENANT, DOCUMENT_ID), 1, 5));

        // The delivery thread had no context of its own: the payload put the tenant in place before
        // the first read, and the handler removed it again so the next task cannot inherit it.
        assertThat(tenantsSeenByRepository).containsExactly(String.valueOf(TENANT));
        assertThat(MDC.get(mdcKey)).isNull();
    }

    @Test
    void clears_the_tenant_mdc_when_the_delivery_throws() {
        String mdcKey = MYBATIS_PLUS_PROPERTIES.getTenantId().getMdcKey();
        given(documentRepository.findById(DOCUMENT_ID)).willReturn(Optional.empty());

        handler.deliver(delivery(payload(TENANT, DOCUMENT_ID), 1, 5));

        assertThat(MDC.get(mdcKey)).isNull();
    }

    @Test
    void treats_a_missing_document_as_success() {
        given(documentRepository.findById(DOCUMENT_ID)).willReturn(Optional.empty());

        DeliveryResult result = handler.deliver(delivery(payload(TENANT, DOCUMENT_ID), 1, 5));

        // The tenant filter also hides another tenant's document, so a miss means "deleted or never
        // ours": there is nothing to index and a retry would not change that.
        assertThat(result.kind()).isEqualTo(DeliveryResult.Kind.SUCCESS);
        verifyNoInteractions(ragIngestionService);
        verify(documentRepository, never()).markProcessing(anyLong(), anyInt());
    }

    @Test
    void stops_when_another_attempt_already_owns_the_document() {
        givenVisibleDocument();
        given(documentRepository.markProcessing(DOCUMENT_ID, 1)).willReturn(false);

        DeliveryResult result = handler.deliver(delivery(payload(TENANT, DOCUMENT_ID), 1, 5));

        assertThat(result.kind()).isEqualTo(DeliveryResult.Kind.SUCCESS);
        verifyNoInteractions(ragIngestionService);
    }

    @Test
    void fails_a_document_that_has_no_stored_text() {
        given(documentRepository.findById(DOCUMENT_ID))
                .willReturn(Optional.of(documentWithoutContent()));
        given(documentRepository.markProcessing(DOCUMENT_ID, 1)).willReturn(true);

        deliverExpectingFailure(1);

        verify(documentRepository).markRetryPending(DOCUMENT_ID, 1, "KNOWLEDGE_CONTENT_MISSING",
                "the document has no stored text to embed");
        verifyNoInteractions(ragIngestionService, knowledgeBaseRepository);
    }

    @Test
    void ends_the_delivery_when_the_knowledge_base_is_gone() {
        givenVisibleDocument();
        given(documentRepository.markProcessing(DOCUMENT_ID, 1)).willReturn(true);
        given(knowledgeBaseRepository.findById(KNOWLEDGE_BASE_ID)).willReturn(Optional.empty());

        DeliveryResult result = handler.deliver(delivery(payload(TENANT, DOCUMENT_ID), 1, 5));

        // No retry will bring the base back, so the message is answered now: the document reaches a
        // terminal status instead of being stranded in PROCESSING with nothing left to move it.
        assertThat(result.kind()).isEqualTo(DeliveryResult.Kind.PERMANENT_FAILURE);
        assertThat(result.code()).isEqualTo("KNOWLEDGE_BASE_NOT_FOUND");
        verify(documentRepository).markDead(DOCUMENT_ID, "KNOWLEDGE_BASE_NOT_FOUND",
                "the document's knowledge base no longer exists");
        verifyNoInteractions(ragIngestionService);
    }

    @Test
    void fails_without_touching_the_database_when_the_payload_has_no_tenant() {
        DeliveryContext withoutTenant = delivery(payloadWithoutTenant(DOCUMENT_ID), 1, 5);

        assertThatThrownBy(() -> handler.deliver(withoutTenant)).isInstanceOf(IllegalArgumentException.class);

        // Never run against an empty tenant: the delivery fails and the outbox retries or
        // dead-letters it instead.
        verifyNoInteractions(documentRepository, knowledgeBaseRepository, ragIngestionService);
        assertThat(MDC.get(MYBATIS_PLUS_PROPERTIES.getTenantId().getMdcKey())).isNull();
    }

    @Test
    void rejects_without_touching_the_database_when_the_envelope_is_not_the_expected_version() {
        String payload = "{\"schemaVersion\":\"2\",\"documentId\":" + DOCUMENT_ID
                + ",\"tenantId\":" + TENANT + "}";

        assertThatThrownBy(() -> handler.deliver(delivery(payload, 1, 5)))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(documentRepository, knowledgeBaseRepository, ragIngestionService);
    }

    private void deliverExpectingFailure(int attempt) {
        assertThatThrownBy(() -> handler.deliver(delivery(payload(TENANT, DOCUMENT_ID), attempt, 5)))
                .isInstanceOf(RuntimeException.class);
    }

    private void givenVisibleDocument() {
        given(documentRepository.findById(DOCUMENT_ID)).willReturn(Optional.of(document()));
        given(knowledgeBaseRepository.findById(KNOWLEDGE_BASE_ID))
                .willReturn(Optional.of(knowledgeBase()));
    }

    private static KnowledgeDocumentBO document() {
        return new KnowledgeDocumentBO(DOCUMENT_ID, TENANT, KNOWLEDGE_BASE_ID, "report.txt", "report.txt",
                "text/plain", CONTENT.length(), "a".repeat(64), "LOCAL", "local/report.txt", CONTENT,
                DocumentIngestStatusEnum.PENDING, 0, 0, null, null, STAMP, STAMP);
    }

    private static KnowledgeDocumentBO documentWithoutContent() {
        return new KnowledgeDocumentBO(DOCUMENT_ID, TENANT, KNOWLEDGE_BASE_ID, "report.txt", "report.txt",
                "text/plain", 0, "a".repeat(64), "LOCAL", "local/report.txt", null,
                DocumentIngestStatusEnum.PENDING, 0, 0, null, null, STAMP, STAMP);
    }

    private static KnowledgeBaseBO knowledgeBase() {
        return new KnowledgeBaseBO(KNOWLEDGE_BASE_ID, TENANT, "internal-notes", "Internal notes", null,
                LOGICAL_MODEL_NAME, ChunkingStrategyEnum.RECURSIVE,
                new KnowledgeChunkConfigBO(256, 32, 16, List.of()),
                KnowledgeBaseStatusEnum.ACTIVE, STAMP, STAMP);
    }

    private static RagIngestionResult ingestionResult(int chunkCount) {
        return new RagIngestionResult(String.valueOf(KNOWLEDGE_BASE_ID), String.valueOf(DOCUMENT_ID),
                LOGICAL_MODEL_NAME, 1536, chunkCount, Duration.ofMillis(12));
    }

    private static DeliveryContext delivery(String payload, int attempt, int maxAttempts) {
        return new DeliveryContext("message-1", KnowledgeIngestDeliveryHandler.CHANNEL,
                String.valueOf(DOCUMENT_ID), payload, "application/json", "1",
                Map.of(), "trace-1", attempt, maxAttempts, STAMP.plusSeconds(30));
    }

    private static String payload(long tenant, long documentId) {
        return "{\"schemaVersion\":\"1\",\"documentId\":" + documentId + ",\"tenantId\":" + tenant + "}";
    }

    private static String payloadWithoutTenant(long documentId) {
        return "{\"schemaVersion\":\"1\",\"documentId\":" + documentId + "}";
    }
}
