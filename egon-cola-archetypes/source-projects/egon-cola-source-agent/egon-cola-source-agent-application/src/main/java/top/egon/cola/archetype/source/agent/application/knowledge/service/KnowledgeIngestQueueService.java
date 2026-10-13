package top.egon.cola.archetype.source.agent.application.knowledge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.business.EgonColaUserIdProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.archetype.source.agent.common.exception.KnowledgeApplicationException;
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
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class KnowledgeIngestQueueService {

    private final @Qualifier("knowledgeDocumentRepository") KnowledgeDocumentRepository documentRepository;

    private final @Qualifier("transactionalOutbox") TransactionalOutbox transactionalOutbox;

    private final @Qualifier("egonColaMdcUserIdProvider") EgonColaUserIdProvider actorProvider;

    /** Stores the document and queues its first ingest; anything failing rolls both back. */
    @Transactional
    public KnowledgeDocumentBO storeAndEnqueue(KnowledgeDocumentBO document, String traceId) {
        Map<String, String> headers = auditHeaders(document, traceId);
        KnowledgeDocumentBO stored = documentRepository.insert(document);
        enqueue(stored, traceId, headers);
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
        Map<String, String> headers = auditHeaders(document, traceId);
        if (!documentRepository.resetForReingest(document.documentId())) {
            log.info("knowledge reingest documentId={} outcome=SKIPPED reason=status_changed",
                    document.documentId());
            return false;
        }
        enqueue(document, traceId, headers);
        log.info("knowledge reingest documentId={} knowledgeBaseId={} outcome=QUEUED",
                document.documentId(), document.knowledgeBaseId());
        return true;
    }

    /**
     * Queues one ingest. The payload carries the tenant because the delivery thread has no request
     * context to restore it from, and the handler rebuilds that thread's tenant scope from it.
     */
    private void enqueue(KnowledgeDocumentBO document, String traceId, Map<String, String> headers) {
        try {
            transactionalOutbox.enqueue(OutboxMessage.builder()
                    .channel(KnowledgeIngestChannel.NAME)
                    .destination(String.valueOf(document.documentId()))
                    .schemaVersion(KnowledgeIngestChannel.SCHEMA_VERSION)
                    .payload(payload(document))
                    .headers(headers)
                    .traceId(traceId)
                    .build());
        } catch (RuntimeException failure) {
            // The use case answers "queued", so a refused enqueue has to roll the row back with it
            // rather than surface as a document nothing will ever process.
            throw new KnowledgeApplicationException(KnowledgeErrorCodeEnum.KNOWLEDGE_INTERNAL_ERROR, traceId, failure);
        }
    }

    /** Captures server-owned facts before either document write; HTTP headers never enter this map. */
    private Map<String, String> auditHeaders(KnowledgeDocumentBO document, String traceId) {
        String actor;
        Long tenant;
        try {
            actor = actorProvider.currentUserId();
            tenant = EgonColaTenantIdProvider.currentTenantId();
        } catch (IllegalStateException missing) {
            throw new KnowledgeApplicationException(KnowledgeErrorCodeEnum.KNOWLEDGE_INTERNAL_ERROR, traceId, missing);
        }
        String kind = MDC.get("egon.auth.actor-kind");
        if (actor == null || actor.isBlank() || actor.length() > 128 || !actor.equals(actor.strip())
                || !("USER".equals(kind) || "SERVICE".equals(kind))
                || tenant == null || tenant <= 0 || !tenant.equals(document.tenantId())) {
            throw new KnowledgeApplicationException(KnowledgeErrorCodeEnum.KNOWLEDGE_INTERNAL_ERROR, traceId);
        }
        return Map.of("egon.auth.actor-id", actor, "egon.auth.actor-kind", kind, "egon.auth.tenant-id", tenant.toString());
    }

    private static Map<String, Object> payload(KnowledgeDocumentBO document) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(KnowledgeIngestChannel.SCHEMA_VERSION_FIELD, KnowledgeIngestChannel.SCHEMA_VERSION);
        payload.put(KnowledgeIngestChannel.DOCUMENT_ID_FIELD, document.documentId());
        payload.put(KnowledgeIngestChannel.TENANT_ID_FIELD, document.tenantId());
        return Map.copyOf(payload);
    }
}
