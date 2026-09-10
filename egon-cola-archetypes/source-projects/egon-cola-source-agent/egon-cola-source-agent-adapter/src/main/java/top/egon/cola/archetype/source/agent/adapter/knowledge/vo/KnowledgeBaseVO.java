package top.egon.cola.archetype.source.agent.adapter.knowledge.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.ChunkingStrategyEnum;

import java.time.Instant;
import java.util.Map;

/**
 * Public representation of one knowledge base (Spec B §9.2.1, §9.2.3, §9.2.4).
 *
 * <p>The frozen indexing configuration is rendered per strategy: only a heading strategy shows
 * {@code headingLevels}, so a client can render what was actually frozen instead of guessing. Absent
 * fields are omitted rather than sent as {@code null}, which is what makes an unset description
 * indistinguishable from a cleared one — exactly the contract's intent.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "KnowledgeBaseVO")
public record KnowledgeBaseVO(
        @Schema(example = "7") Long knowledgeBaseId,
        @Schema(example = "product-docs") String code,
        @Schema(example = "产品文档库") String name,
        @Schema(example = "内部产品手册") String description,
        @Schema(example = "openai-small") String embeddingModel,
        @Schema(allowableValues = {"TOKEN", "MARKDOWN_HEADING", "RECURSIVE"}, example = "TOKEN")
        ChunkingStrategyEnum chunkStrategy,
        Map<String, Object> chunkConfig,
        @Schema(example = "12") long documentCount,
        @Schema(allowableValues = {"ACTIVE", "DELETED"}, example = "ACTIVE") String status,
        @Schema(format = "date-time") Instant createdAt,
        @Schema(format = "date-time") Instant updatedAt,
        @Schema(example = "4e9d6938") String traceId) {
}
