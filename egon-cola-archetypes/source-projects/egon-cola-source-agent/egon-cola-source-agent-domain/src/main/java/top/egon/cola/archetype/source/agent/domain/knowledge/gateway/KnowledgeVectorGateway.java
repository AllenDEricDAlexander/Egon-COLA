package top.egon.cola.archetype.source.agent.domain.knowledge.gateway;

import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeChunkBO;

import java.util.List;
import java.util.Map;

/**
 * Domain-owned port for retrieving document chunks from the vector store.
 *
 * <p>Keeps the retrieval engine's types out of the use cases: the host supplies the collection, the
 * logical model and the tenant scope, and the adapter decides how they are enforced.
 */
public interface KnowledgeVectorGateway {

    /**
     * @param attributes equality conditions the caller needs on top of the collection and model
     *                   scope, such as the tenant the chunks must belong to
     * @return matching chunks ordered by descending score, with an unknown score last; an empty
     *         list when nothing matches
     */
    List<KnowledgeChunkBO> retrieve(String collectionId, String logicalModelName, String query,
                                    int topK, Map<String, String> attributes);

    /**
     * Removes every chunk of one document, so a deleted document can never be retrieved again.
     *
     * <p>Idempotent: a document with no chunks left is a success, which is what lets the delete use
     * case run for a document whose ingestion never succeeded.
     */
    void deleteDocument(String collectionId, String documentId);
}
