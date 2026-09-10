package top.egon.cola.archetype.source.agent.adapter.knowledge.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import top.egon.cola.archetype.source.agent.common.error.KnowledgeErrorCodeEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeQaEventTypeEnum;

import java.time.Instant;
import java.util.List;

/**
 * Public payload of one knowledge answer event (Spec B §9.2.12).
 *
 * <p>One shape covers all four event types and the fields a variant does not use are left out rather
 * than sent as {@code null}: a client reads a progress event for its delta and a failure for its
 * code, and an explicitly empty object on either one would only invite a client to look for meaning
 * where the contract defines none.
 *
 * <p>Only fields the contract publishes are here. What a provider or a prompt produced beyond the
 * answer text — reasoning, token counts, vendor identifiers — has no place on this record, and the
 * type is the allowlist that keeps it from being added without a contract change.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "KnowledgeQaEventVO")
public record KnowledgeQaEventVO(
        @Schema(example = "qa-01J5K9") String answerId,
        @Schema(example = "1", minimum = "1") long sequence,
        @Schema(allowableValues = {"STARTED", "PROGRESS", "COMPLETED", "FAILED"}, example = "PROGRESS")
        KnowledgeQaEventTypeEnum type,
        @Schema(description = "Present on STARTED and COMPLETED: the chunks the answer was written from")
        List<KnowledgeQaReferenceVO> retrievals,
        @Schema(description = "Present on PROGRESS: one increment of the answer text") String delta,
        @Schema(description = "Present on COMPLETED: the whole answer, to be rendered as text")
        String answer,
        @Schema(description = "Present on FAILED: the stable knowledge error code") KnowledgeErrorCodeEnum code,
        @Schema(description = "Present on FAILED: the code's safe summary") String message,
        @Schema(description = "Present on FAILED: whether another attempt can succeed") Boolean retryable,
        @Schema(format = "date-time") Instant occurredAt,
        @Schema(example = "4e9d6938") String traceId) {
}
