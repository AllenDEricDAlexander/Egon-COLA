package top.egon.cola.archetype.source.agent.domain.knowledge.repository;

import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeBaseBO;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for knowledge bases.
 *
 * <p>No method takes a tenant: the scope is applied from the request tenant by the persistence
 * layer, so a use case cannot read or write outside the tenant it runs in.
 */
public interface KnowledgeBaseRepository {

    /** Stores the base and returns it with the generated identifier and server timestamps. */
    KnowledgeBaseBO insert(KnowledgeBaseBO knowledgeBase);

    Optional<KnowledgeBaseBO> findById(Long knowledgeBaseId);

    /** Page of bases ordered by creation time descending; the filters are optional. */
    List<KnowledgeBaseBO> page(int offset, int size, String keyword, String embeddingModel);

    long count(String keyword, String embeddingModel);

    /** Updates the editable metadata only: the frozen indexing configuration never changes. */
    void updateNameAndDescription(Long knowledgeBaseId, String name, String description);

    /** Soft deletes the base; it stops being visible to queries and can no longer be written to. */
    void softDelete(Long knowledgeBaseId);
}
