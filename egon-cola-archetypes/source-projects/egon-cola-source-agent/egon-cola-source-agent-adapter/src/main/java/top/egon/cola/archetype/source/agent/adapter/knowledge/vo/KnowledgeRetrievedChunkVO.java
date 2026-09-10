package top.egon.cola.archetype.source.agent.adapter.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One matched chunk of a retrieval debug response (Spec B §9.2.11).
 *
 * <p>The text is included: judging what a query recalls is the whole point of the endpoint, and the
 * caller already owns the base the text was indexed from. The score is the vector store's similarity
 * and stays nullable — a store that ranks without scoring is still a valid answer.
 */
@Schema(name = "KnowledgeRetrievedChunkVO")
public record KnowledgeRetrievedChunkVO(
        @Schema(example = "9") Long documentId,
        @Schema(example = "7", minimum = "0") int chunkIndex,
        @Schema(example = "0.83", nullable = true,
                description = "Similarity as the vector store reported it; absent when it ranks only")
        Double score,
        @Schema(example = "第一章 概述") String content,
        @Schema(example = "report.pdf") String displayName) {
}
