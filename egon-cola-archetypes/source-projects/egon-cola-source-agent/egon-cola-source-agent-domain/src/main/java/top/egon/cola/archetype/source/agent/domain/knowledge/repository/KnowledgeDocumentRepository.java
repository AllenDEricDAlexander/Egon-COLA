package top.egon.cola.archetype.source.agent.domain.knowledge.repository;

import top.egon.cola.archetype.source.agent.domain.knowledge.model.DocumentIngestStatusEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeDocumentBO;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Persistence port for documents and their ingest status.
 *
 * <p>No method takes a tenant: the scope is applied from the request tenant by the persistence
 * layer. The status writes are conditional by design — each one names the status it expects to
 * leave, so a delivery attempt that lost its document to a concurrent reprocess changes nothing
 * and reports {@code false} instead of overwriting the newer state.
 */
public interface KnowledgeDocumentRepository {

    /** Stores the document and returns it with the generated identifier and server timestamps. */
    KnowledgeDocumentBO insert(KnowledgeDocumentBO document);

    Optional<KnowledgeDocumentBO> findById(Long documentId);

    /** Documents of the given identifiers, for resolving the display names of retrieved chunks. */
    List<KnowledgeDocumentBO> findByIds(Collection<Long> documentIds);

    /** Page of one base's documents ordered by creation time descending; the filters are optional. */
    List<KnowledgeDocumentBO> page(Long knowledgeBaseId, int offset, int size,
                                   DocumentIngestStatusEnum status, String keyword);

    long count(Long knowledgeBaseId, DocumentIngestStatusEnum status, String keyword);

    /** Undeleted document count of one base, for the base detail and the per-base capacity check. */
    long countByKnowledgeBaseId(Long knowledgeBaseId);

    /** Moves a {@code PENDING} document to {@code PROCESSING}. */
    boolean markProcessing(Long documentId, int attemptCount);

    /** Moves a {@code PROCESSING} document to {@code SUCCEEDED} and records the chunk count. */
    boolean markSucceeded(Long documentId, int chunkCount);

    /** Moves a {@code PROCESSING} document back to {@code PENDING} for another attempt. */
    boolean markRetryPending(Long documentId, int attemptCount, String errorCode, String errorMessage);

    /** Moves a {@code PROCESSING} document to the exhausted {@code DEAD} status. */
    boolean markDead(Long documentId, String errorCode, String errorMessage);

    /** Moves a terminal document back to {@code PENDING}, clearing failure details and counters. */
    boolean resetForReingest(Long documentId);

    void softDelete(Long documentId);

    /** Soft deletes every document of one base, following the base deletion. */
    void softDeleteByKnowledgeBaseId(Long knowledgeBaseId);
}
