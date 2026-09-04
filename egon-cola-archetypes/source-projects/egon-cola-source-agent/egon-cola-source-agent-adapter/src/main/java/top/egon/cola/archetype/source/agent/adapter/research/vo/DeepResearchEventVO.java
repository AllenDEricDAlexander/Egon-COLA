package top.egon.cola.archetype.source.agent.adapter.research.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import top.egon.cola.archetype.source.agent.common.error.ResearchErrorCodeEnum;
import top.egon.cola.archetype.source.agent.domain.research.model.ResearchEventTypeEnum;
import top.egon.cola.archetype.source.agent.domain.research.model.ResearchStageEnum;

import java.time.Instant;

/** Public SSE data allowlist for one Deep Research lifecycle event. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "DeepResearchEventVO", description = "One JSON data line within a research SSE event.")
public record DeepResearchEventVO(
        @Schema(example = "run-1") String runId,
        @Schema(example = "2") long sequence,
        @Schema(example = "PROGRESS") ResearchEventTypeEnum type,
        @Schema(example = "EVIDENCE_RESEARCH") ResearchStageEnum stage,
        @Schema(format = "date-time") Instant occurredAt,
        @Schema(example = "EvidenceResearcher") String agentName,
        String delta,
        String reportMarkdown,
        ResearchErrorCodeEnum errorCode,
        String errorMessage,
        Boolean retryable,
        @Schema(example = "trace-1") String traceId) {
}
