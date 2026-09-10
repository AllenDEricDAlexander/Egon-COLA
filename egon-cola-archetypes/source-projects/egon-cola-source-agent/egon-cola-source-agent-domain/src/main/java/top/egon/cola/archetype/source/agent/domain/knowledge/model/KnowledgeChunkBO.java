package top.egon.cola.archetype.source.agent.domain.knowledge.model;

/**
 * One retrieved chunk of a document, in the vocabulary of the knowledge domain.
 *
 * <p>Isolates the retrieval engine's own result carriers. The score is nullable because the vector
 * store is not required to return one, and no range is asserted here: it is a similarity reported
 * by the store, not a probability this domain computes.
 */
public record KnowledgeChunkBO(
        Long documentId,
        int chunkIndex,
        String content,
        Double score) {

    public KnowledgeChunkBO {
        if (documentId == null || documentId <= 0) {
            throw new IllegalArgumentException("documentId must be positive");
        }
        if (chunkIndex < 0) {
            throw new IllegalArgumentException("chunkIndex must not be negative");
        }
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
    }
}
