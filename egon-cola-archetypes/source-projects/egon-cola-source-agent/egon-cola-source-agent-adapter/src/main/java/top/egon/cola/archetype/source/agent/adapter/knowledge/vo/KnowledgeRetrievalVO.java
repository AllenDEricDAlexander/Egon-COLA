package top.egon.cola.archetype.source.agent.adapter.knowledge.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Published result of one retrieval debug run (Spec B §9.2.11).
 *
 * <p>The carrier holds the chunks and the model the base froze, so the query and the correlation id
 * are added here: the caller owns both, and echoing them is what lets a client pair an answer with
 * the request that produced it. No answer is part of this representation — a retrieval never calls a
 * chat model, which is what keeps the endpoint free of generation cost.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "KnowledgeRetrievalVO")
public record KnowledgeRetrievalVO(
        List<KnowledgeRetrievedChunkVO> items,
        @Schema(example = "如何配置超时") String query,
        @Schema(example = "openai-small", description = "The logical model the base froze") String embeddingModel,
        @Schema(example = "4e9d6938") String traceId) {
}
