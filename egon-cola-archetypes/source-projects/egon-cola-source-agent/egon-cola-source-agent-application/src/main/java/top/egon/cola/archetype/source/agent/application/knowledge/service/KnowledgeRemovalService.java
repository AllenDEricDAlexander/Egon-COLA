package top.egon.cola.archetype.source.agent.application.knowledge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.archetype.source.agent.domain.knowledge.gateway.KnowledgeVectorGateway;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeDocumentBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.repository.KnowledgeBaseRepository;
import top.egon.cola.archetype.source.agent.domain.knowledge.repository.KnowledgeDocumentRepository;
import top.egon.cola.component.rag.storage.RagDocumentStorage;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The removal units of the knowledge use cases: an original file, one document, one base.
 *
 * <p>An own bean because {@code @Transactional} is applied by a proxy: a use case calling its own
 * annotated method would run with no transaction at all. Keeping the removal here also keeps the
 * slow part out of it — the use case deletes the files through {@link #deleteStoredFile} before the
 * database work starts, so no transaction is ever held open across file I/O.
 *
 * <p>Both row removals are atomic across the vector store and the database: chunks and rows
 * disappear together, so a document can neither stay retrievable after its row is gone nor vanish
 * from the listing while its chunks are still returned.
 */
@Slf4j
@Service("knowledgeRemovalService")
@RequiredArgsConstructor
public class KnowledgeRemovalService {

    private final @Qualifier("knowledgeVectorGateway") KnowledgeVectorGateway vectorGateway;

    private final @Qualifier("knowledgeDocumentRepository") KnowledgeDocumentRepository documentRepository;

    private final @Qualifier("knowledgeBaseRepository") KnowledgeBaseRepository knowledgeBaseRepository;

    private final @Qualifier("ragDocumentStorage") RagDocumentStorage documentStorage;

    /**
     * Deletes the original of one document, best effort: the row is what makes a document visible,
     * and an orphan file is housekeeping rather than a state the caller can observe. A storage
     * failure is logged and never fails the removal that follows.
     *
     * @param storageKey the row's own key, the one the upload composed — see
     *                   {@code KnowledgeDocumentManageImpl#upload}
     */
    public void deleteStoredFile(KnowledgeDocumentBO document) {
        Objects.requireNonNull(document, "document must not be null");
        storageDocumentId(document.storageKey()).ifPresentOrElse(
                storageDocumentId -> deleteStoredFile(String.valueOf(document.knowledgeBaseId()), storageDocumentId),
                () -> log.warn("knowledge file skipped documentId={} outcome=SKIPPED reason=unrecognized_storage_key",
                        document.documentId()));
    }

    /** Deletes one stored original by the identifiers it was written under. */
    public void deleteStoredFile(String collectionId, String storageDocumentId) {
        try {
            documentStorage.delete(collectionId, storageDocumentId);
        } catch (RuntimeException failure) {
            log.warn("knowledge file orphaned knowledgeBaseId={} outcome=ORPHANED reason={}",
                    collectionId, failure.getClass().getSimpleName());
        }
    }

    /** Removes one document's chunks and soft deletes its row. */
    @Transactional
    public void removeDocument(String collectionId, Long documentId) {
        Objects.requireNonNull(collectionId, "collectionId must not be null");
        Objects.requireNonNull(documentId, "documentId must not be null");
        vectorGateway.deleteDocument(collectionId, String.valueOf(documentId));
        documentRepository.softDelete(documentId);
        log.info("knowledge document removed documentId={} knowledgeBaseId={}", documentId, collectionId);
    }

    /** Removes every chunk of the base, then soft deletes its documents and the base itself. */
    @Transactional
    public void removeBase(String collectionId, Long knowledgeBaseId, List<Long> documentIds) {
        Objects.requireNonNull(collectionId, "collectionId must not be null");
        Objects.requireNonNull(knowledgeBaseId, "knowledgeBaseId must not be null");
        List<Long> documents = documentIds == null ? List.of() : List.copyOf(documentIds);
        documents.forEach(documentId -> vectorGateway.deleteDocument(collectionId, String.valueOf(documentId)));
        documentRepository.softDeleteByKnowledgeBaseId(knowledgeBaseId);
        knowledgeBaseRepository.softDelete(knowledgeBaseId);
        log.info("knowledge base removed knowledgeBaseId={} documentCount={}",
                knowledgeBaseId, documents.size());
    }

    /** The second segment of a stored key, the identifier the storage was written under. */
    private static Optional<String> storageDocumentId(String storageKey) {
        if (storageKey == null) {
            return Optional.empty();
        }
        String[] segments = storageKey.split("/");
        return segments.length == 3 && !segments[1].isBlank() ? Optional.of(segments[1]) : Optional.empty();
    }
}
