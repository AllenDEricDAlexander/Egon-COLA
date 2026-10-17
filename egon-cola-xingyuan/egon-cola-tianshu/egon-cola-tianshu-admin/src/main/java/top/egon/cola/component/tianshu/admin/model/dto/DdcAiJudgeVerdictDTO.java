package top.egon.cola.component.tianshu.admin.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 一次 LLM 评分的不可变裁决。
 * / Immutable verdict of one LLM judgement.
 */
public record DdcAiJudgeVerdictDTO(
        @Min(0) @Max(100) int score,
        @NotNull @Size(max = 512) String rationale) {

    public DdcAiJudgeVerdictDTO {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("AI judge score must be within 0..100");
        }
        rationale = rationale == null ? "" : rationale.length() > 512 ? rationale.substring(0, 512) : rationale;
    }
}
