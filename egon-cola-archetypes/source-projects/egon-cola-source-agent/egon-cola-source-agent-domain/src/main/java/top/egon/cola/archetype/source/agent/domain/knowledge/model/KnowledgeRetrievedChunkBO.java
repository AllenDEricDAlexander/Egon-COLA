package top.egon.cola.archetype.source.agent.domain.knowledge.model;

/**
 * One retrieved chunk with the display name of the document it belongs to.
 *
 * <p>Extends {@link KnowledgeChunkBO} with the one thing the vector store does not carry: the
 * document's display name, resolved from the database so that neither a retrieval response nor an
 * answer's references force the caller into a second lookup. The score keeps the vector store's
 * semantics — nullable, and a similarity rather than a probability this domain computes.
 */
public record KnowledgeRetrievedChunkBO(
        Long documentId,
        int chunkIndex,
        String displayName,
        Double score,
        String content) {

    public KnowledgeRetrievedChunkBO {
        if (documentId == null || documentId <= 0) {
            throw new IllegalArgumentException("documentId must be positive");
        }
        if (chunkIndex < 0) {
            throw new IllegalArgumentException("chunkIndex must not be negative");
        }
        displayName = displayName == null || displayName.isBlank() ? null : displayName.trim();
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
    }

    /** The same chunk without its text, which is all an answer's reference list carries. */
    public KnowledgeRetrievedChunkBO withoutContent() {
        return new KnowledgeRetrievedChunkBO(documentId, chunkIndex, displayName, score, "");
    }
}
