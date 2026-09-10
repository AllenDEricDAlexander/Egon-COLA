package top.egon.cola.archetype.source.agent.application.knowledge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.archetype.source.agent.application.knowledge.exception.KnowledgeApplicationException;
import top.egon.cola.archetype.source.agent.common.error.KnowledgeErrorCodeEnum;
import top.egon.cola.archetype.source.agent.common.knowledge.KnowledgeIngestChannel;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeDocumentBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.repository.KnowledgeDocumentRepository;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.TransactionalOutbox;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes the document row and announces its ingest in one local transaction.
 *
 * <p>An own bean rather than a method on the use case on purpose: {@code @Transactional} is applied
 * by a proxy, so a use case calling its own annotated method would run outside any transaction and
 * the outbox would refuse the enqueue — the row and the message would no longer be atomic, and the
 * failure would only show up as a missing document much later.
 *
 * <p>The transaction is the "a stored document is always announced" guarantee: either both rows
 * exist or neither does, so a crash between them can neither strand an unprocessed document nor
 * announce one that was never stored.
 */
@Slf4j
@Service("knowledgeIngestQueueService")
@RequiredArgsConstructor
public class KnowledgeIngestQueueService {

    private final @Qualifier("knowledgeDocumentRepository") KnowledgeDocumentRepository documentRepository;

    private final @Qualifier("transactionalOutbox") TransactionalOutbox transactionalOutbox;

    /** Stores the document and queues its first ingest; anything failing rolls both back. */
    @Transactional
    public KnowledgeDocumentBO storeAndEnqueue(KnowledgeDocumentBO document, String traceId) {
        KnowledgeDocumentBO stored = documentRepository.insert(document);
        enqueue(stored, traceId);
        log.info("knowledge upload documentId={} knowledgeBaseId={} sizeBytes={} outcome=QUEUED",
                stored.documentId(), stored.knowledgeBaseId(), stored.sizeBytes());
        return stored;
    }

    /**
     * Resets a terminal document and queues another ingest of the text it already carries.
     *
     * @return {@code false} when the document was no longer terminal — a concurrent reprocess won
     *         the race — in which case nothing was written and no message was queued
     */
    @Transactional
    public boolean resetAndEnqueue(KnowledgeDocumentBO document, String traceId) {
        if (!documentRepository.resetForReingest(document.documentId())) {
            log.info("knowledge reingest documentId={} outcome=SKIPPED reason=status_changed",
                    document.documentId());
            return false;
        }
        enqueue(document, traceId);
        log.info("knowledge reingest documentId={} knowledgeBaseId={} outcome=QUEUED",
                document.documentId(), document.knowledgeBaseId());
        return true;
    }

    /**
     * Queues one ingest. The payload carries the tenant because the delivery thread has no request
     * context to restore it from, and the handler rebuilds that thread's tenant scope from it.
     */
    private void enqueue(KnowledgeDocumentBO document, String traceId) {
        try {
            transactionalOutbox.enqueue(OutboxMessage.builder()
                    .channel(KnowledgeIngestChannel.NAME)
                    .destination(String.valueOf(document.documentId()))
                    .schemaVersion(KnowledgeIngestChannel.SCHEMA_VERSION)
                    .payload(payload(document))
                    .traceId(traceId)
                    .build());
        } catch (RuntimeException failure) {
            // The use case answers "queued", so a refused enqueue has to roll the row back with it
            // rather than surface as a document nothing will ever process.
            throw new KnowledgeApplicationException(KnowledgeErrorCodeEnum.KNOWLEDGE_INTERNAL_ERROR, traceId, failure);
        }
    }

    private static Map<String, Object> payload(KnowledgeDocumentBO document) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(KnowledgeIngestChannel.SCHEMA_VERSION_FIELD, KnowledgeIngestChannel.SCHEMA_VERSION);
        payload.put(KnowledgeIngestChannel.DOCUMENT_ID_FIELD, document.documentId());
        payload.put(KnowledgeIngestChannel.TENANT_ID_FIELD, document.tenantId());
        return Map.copyOf(payload);
    }
}
