package top.egon.cola.archetype.source.agent.domain.knowledge.model;

import java.util.List;

/**
 * One bounded answer generation: the question, the references that ground it and the model to use.
 *
 * <p>The logical model name comes from the knowledge base and is never taken from a caller, which is
 * what keeps a base's documents and its questions answered by the same model. The references are the
 * chunks retrieval just produced, in the same order and with the same scores the caller was shown.
 */
public record KnowledgeAnswerTaskBO(
        String answerId,
        String question,
        String logicalModelName,
        List<KnowledgeRetrievedChunkBO> references,
        String traceId) {

    public KnowledgeAnswerTaskBO {
        answerId = requireText(answerId, "answerId");
        question = requireText(question, "question");
        logicalModelName = requireText(logicalModelName, "logicalModelName");
        references = references == null ? List.of() : List.copyOf(references);
        traceId = requireText(traceId, "traceId");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
