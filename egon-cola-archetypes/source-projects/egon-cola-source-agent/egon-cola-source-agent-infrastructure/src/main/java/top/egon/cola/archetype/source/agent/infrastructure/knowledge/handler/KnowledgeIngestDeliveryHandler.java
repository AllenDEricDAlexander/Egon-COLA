package top.egon.cola.archetype.source.agent.infrastructure.knowledge.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeBaseBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeChunkConfigBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeDocumentBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.repository.KnowledgeBaseRepository;
import top.egon.cola.archetype.source.agent.domain.knowledge.repository.KnowledgeDocumentRepository;
import top.egon.cola.archetype.source.agent.common.knowledge.KnowledgeIngestChannel;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.metadata.KnowledgeVectorMetadata;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryHandler;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import top.egon.cola.component.rag.api.RagIngestionService;
import top.egon.cola.component.rag.chunk.RagChunkingStrategyEnum;
import top.egon.cola.component.rag.exception.RagException;
import top.egon.cola.component.rag.model.ExtractedDocumentBO;
import top.egon.cola.component.rag.model.RagChunkingConfigDTO;
import top.egon.cola.component.rag.model.RagIngestionCommand;
import top.egon.cola.component.rag.model.RagIngestionResult;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;

/**
 * Consumes {@code rag-ingest} messages: restores the tenant, rebuilds the document from what was
 * stored and hands it to the RAG component, then writes the outcome back onto the document row.
 *
 * <p>The delivery thread carries no caller context, so the tenant is put in place from the payload
 * before the first read and removed again in a {@code finally} — the pool reuses the thread, and a
 * leftover tenant would scope the next task. The tenant stays the project's thread-scoped channel:
 * the payload field is data used to rebuild that channel, not a parameter, and no repository call
 * takes one.
 *
 * <p>The document text is rebuilt rather than re-extracted, and never re-read from storage, so a
 * retried delivery re-chunks and re-embeds what is already in the database.
 *
 * <p>Failures are thrown instead of classified here: the outbox turns them into retries and, once
 * the attempts are exhausted, into a dead letter. Whatever fails, the document is left in a status
 * the next attempt can act on — {@code PENDING} while attempts remain, {@code DEAD} on the last one
 * — so a delivery that dies mid-flight never strands a document in {@code PROCESSING}.
 */
@Slf4j
@Component("knowledgeIngestDeliveryHandler")
@RequiredArgsConstructor
public class KnowledgeIngestDeliveryHandler implements DeliveryHandler {

    /** Channel this handler owns, registered by name with the outbox registry. */
    public static final String CHANNEL = KnowledgeIngestChannel.NAME;

    private static final String CONTENT_MISSING = "KNOWLEDGE_CONTENT_MISSING";

    private static final String CONTENT_MISSING_SUMMARY = "the document has no stored text to embed";

    private static final String BASE_MISSING = "KNOWLEDGE_BASE_NOT_FOUND";

    private static final String BASE_MISSING_SUMMARY = "the document's knowledge base no longer exists";

    private static final String INTERNAL_FAILURE = "KNOWLEDGE_INTERNAL_ERROR";

    private static final String INTERNAL_FAILURE_SUMMARY = "document ingestion failed";

    /** Stable project code per component failure code; anything else stays unclassified. */
    private static final Map<String, String> COMPONENT_FAILURE_CODES = Map.of(
            "RAG_VECTOR_STORE", "KNOWLEDGE_EMBEDDING_FAILED",
            "RAG_MODEL_NOT_REGISTERED", "KNOWLEDGE_MODEL_NOT_REGISTERED",
            "RAG_VALIDATION", "KNOWLEDGE_VALIDATION_ERROR");

    private final KnowledgeDocumentRepository documentRepository;

    private final KnowledgeBaseRepository knowledgeBaseRepository;

    private final RagIngestionService ragIngestionService;

    private final EgonColaMybatisPlusProperties mybatisPlusProperties;

    private final ObjectMapper objectMapper;

    @Qualifier("agentClock")
    private final Clock clock;

    @Override
    public String channel() {
        return CHANNEL;
    }

    @Override
    public void validateDestination(String destination) {
        if (destination == null || destination.isBlank()) {
            throw new IllegalArgumentException("the ingest destination must be a document identifier");
        }
        try {
            Long.parseLong(destination.trim());
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException("the ingest destination must be a document identifier", invalid);
        }
    }

    @Override
    public DeliveryResult deliver(DeliveryContext context) {
        IngestTask task = parseTask(context.payload());
        String tenantKey = mybatisPlusProperties.getTenantId().getMdcKey();
        MDC.put(tenantKey, String.valueOf(task.tenant()));
        try {
            return ingest(task, context, clock.millis());
        } finally {
            MDC.remove(tenantKey);
        }
    }

    private DeliveryResult ingest(IngestTask task, DeliveryContext context, long startedAt) {
        Optional<KnowledgeDocumentBO> stored = documentRepository.findById(task.documentId());
        if (stored.isEmpty()) {
            // Deleted, or another tenant's: there is nothing left to index and a retry cannot
            // change that, so the message is done.
            log.info("knowledge ingest documentId={} attemptCount={} outcome=SKIPPED reason=document_not_visible",
                    task.documentId(), context.attempt());
            return DeliveryResult.success();
        }
        KnowledgeDocumentBO document = stored.get();
        if (!documentRepository.markProcessing(document.documentId(), context.attempt())) {
            // Another attempt moved the row on; the newer delivery owns the status from here.
            log.info("knowledge ingest documentId={} attemptCount={} outcome=SKIPPED reason=already_claimed",
                    document.documentId(), context.attempt());
            return DeliveryResult.success();
        }
        if (document.content() == null || document.content().isBlank()) {
            return fail(document, null, context, CONTENT_MISSING, CONTENT_MISSING_SUMMARY, startedAt,
                    new IllegalStateException("the stored document carries no text"));
        }
        Optional<KnowledgeBaseBO> base = knowledgeBaseRepository.findById(document.knowledgeBaseId());
        if (base.isEmpty()) {
            // Settled: no retry will bring the base back, and leaving the document in PROCESSING
            // would strand it with no delivery left to move it.
            documentRepository.markDead(document.documentId(), BASE_MISSING, BASE_MISSING_SUMMARY);
            log.warn("knowledge ingest documentId={} knowledgeBaseId={} attemptCount={}/{} outcome=DEAD code={}",
                    document.documentId(), document.knowledgeBaseId(), context.attempt(),
                    context.maxAttempts(), BASE_MISSING);
            return DeliveryResult.permanentFailure(BASE_MISSING, BASE_MISSING_SUMMARY);
        }
        return ingest(document, base.get(), context, startedAt);
    }

    private DeliveryResult ingest(KnowledgeDocumentBO document, KnowledgeBaseBO knowledgeBase,
                                  DeliveryContext context, long startedAt) {
        try {
            RagIngestionResult result = ragIngestionService.ingest(assemble(document, knowledgeBase));
            documentRepository.markSucceeded(document.documentId(), result.chunkCount());
            log.info("knowledge ingest documentId={} knowledgeBaseId={} logicalModelName={} chunkCount={}"
                            + " attemptCount={} outcome=SUCCEEDED elapsedMs={}",
                    document.documentId(), knowledgeBase.knowledgeBaseId(), knowledgeBase.embeddingModel(),
                    result.chunkCount(), context.attempt(), clock.millis() - startedAt);
            return DeliveryResult.success();
        } catch (RagException failure) {
            // The component's own message is safe by contract: it never carries content, vectors,
            // provider payloads, endpoints or keys.
            return fail(document, knowledgeBase, context, failureCode(failure.code()), failure.safeMessage(),
                    startedAt, failure);
        } catch (RuntimeException failure) {
            // Anything else is stored as a fixed code and summary: the document fields are rendered
            // to clients, so an arbitrary exception message must not reach them.
            return fail(document, knowledgeBase, context, INTERNAL_FAILURE, INTERNAL_FAILURE_SUMMARY,
                    startedAt, failure);
        }
    }

    /**
     * Writes the failure onto the document, then rethrows so the outbox retries the message or
     * dead-letters it once the attempts are exhausted.
     *
     * @param knowledgeBase the base being ingested, or {@code null} when the delivery failed before
     *                      the base was read
     */
    private DeliveryResult fail(KnowledgeDocumentBO document, KnowledgeBaseBO knowledgeBase,
                                DeliveryContext context, String code, String summary, long startedAt,
                                RuntimeException failure) {
        String message = summarize(summary);
        boolean exhausted = context.attempt() >= context.maxAttempts();
        if (exhausted) {
            documentRepository.markDead(document.documentId(), code, message);
        } else {
            documentRepository.markRetryPending(document.documentId(), context.attempt(), code, message);
        }
        log.warn("knowledge ingest documentId={} knowledgeBaseId={} logicalModelName={} attemptCount={}/{}"
                        + " outcome={} code={} elapsedMs={}",
                document.documentId(), document.knowledgeBaseId(),
                knowledgeBase == null ? null : knowledgeBase.embeddingModel(), context.attempt(),
                context.maxAttempts(), exhausted ? "DEAD" : "RETRY_PENDING", code,
                clock.millis() - startedAt);
        throw failure;
    }

    /** Turns the stored document and its base into the component's ingest command. */
    private static RagIngestionCommand assemble(KnowledgeDocumentBO document, KnowledgeBaseBO knowledgeBase) {
        return new RagIngestionCommand(
                String.valueOf(knowledgeBase.knowledgeBaseId()),
                String.valueOf(document.documentId()),
                knowledgeBase.embeddingModel(),
                chunkingConfig(knowledgeBase),
                new ExtractedDocumentBO(document.content(), document.displayName(), document.mimeType(), Map.of()),
                // The tenant is written into the chunk metadata by this same call, which is what the
                // retrieval filter later matches on.
                Map.of(KnowledgeVectorMetadata.TENANT_ID, String.valueOf(document.tenantId())));
    }

    /** Rebuilds the component's chunking configuration from the base's frozen parameters. */
    private static RagChunkingConfigDTO chunkingConfig(KnowledgeBaseBO knowledgeBase) {
        KnowledgeChunkConfigBO config = knowledgeBase.chunkConfig();
        return new RagChunkingConfigDTO(
                RagChunkingStrategyEnum.valueOf(knowledgeBase.chunkStrategy().name()),
                config.maxTokensPerChunk(),
                config.overlapTokens(),
                config.minChunkChars(),
                config.headingLevels());
    }

    private static String failureCode(String componentCode) {
        return COMPONENT_FAILURE_CODES.getOrDefault(componentCode, INTERNAL_FAILURE);
    }

    /** Caps what is stored on the document row; the column holds 512 characters. */
    private static String summarize(String summary) {
        String candidate = summary == null || summary.isBlank() ? INTERNAL_FAILURE_SUMMARY : summary.trim();
        return candidate.length() <= 512 ? candidate : candidate.substring(0, 512);
    }

    private IngestTask parseTask(String payload) {
        JsonNode message;
        try {
            message = objectMapper.readTree(payload);
        } catch (JsonProcessingException invalid) {
            throw new IllegalArgumentException("the ingest payload is not valid JSON", invalid);
        }
        JsonNode version = message.path(KnowledgeIngestChannel.SCHEMA_VERSION_FIELD);
        if (!version.isTextual() || !KnowledgeIngestChannel.SCHEMA_VERSION.equals(version.asText())) {
            throw new IllegalArgumentException("unsupported ingest payload schema version");
        }
        return new IngestTask(requiredLong(message, KnowledgeIngestChannel.DOCUMENT_ID_FIELD),
                requiredLong(message, KnowledgeIngestChannel.TENANT_ID_FIELD));
    }

    private static long requiredLong(JsonNode message, String field) {
        JsonNode node = message.path(field);
        if (node.isMissingNode() || node.isNull()) {
            throw new IllegalArgumentException(field + " is required in the ingest payload");
        }
        if (node.isIntegralNumber()) {
            return node.longValue();
        }
        try {
            return Long.parseLong(node.asText().trim());
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException(field + " must be a number in the ingest payload", invalid);
        }
    }

    /** The two identifiers the payload carries, read before anything is touched. */
    private record IngestTask(long documentId, long tenant) {
    }
}
