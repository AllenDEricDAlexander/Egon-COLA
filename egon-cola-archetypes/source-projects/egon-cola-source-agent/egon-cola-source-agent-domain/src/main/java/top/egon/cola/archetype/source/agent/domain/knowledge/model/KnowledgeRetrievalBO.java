package top.egon.cola.archetype.source.agent.domain.knowledge.model;

import java.util.List;

/**
 * Outcome of one retrieval use case: the logical model the base froze, and the chunks it matched.
 *
 * <p>Carries neither the query nor the trace id — the caller owns both and echoes them itself. The
 * question and the answer are deliberately not part of this value: a retrieval never calls a chat
 * model, which is what keeps the debug endpoint free of generation cost.
 */
public record KnowledgeRetrievalBO(String embeddingModel, List<KnowledgeRetrievedChunkBO> items) {

    public KnowledgeRetrievalBO {
        if (embeddingModel == null || embeddingModel.isBlank()) {
            throw new IllegalArgumentException("embeddingModel must not be blank");
        }
        embeddingModel = embeddingModel.trim();
        items = items == null ? List.of() : List.copyOf(items);
    }
}
