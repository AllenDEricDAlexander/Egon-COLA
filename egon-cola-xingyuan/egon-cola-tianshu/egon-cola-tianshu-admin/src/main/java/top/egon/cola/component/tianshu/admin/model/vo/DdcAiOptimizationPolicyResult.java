package top.egon.cola.component.tianshu.admin.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

/** API-012/013 优化策略与评估快照。 / API-012/013 optimization policy with its evaluation snapshot. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class DdcAiOptimizationPolicyResult {

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", pattern = "^[1-9][0-9]{0,18}$")
    private Long resourceId;

    @NotNull
    private Boolean persisted;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", pattern = "^(0|[1-9][0-9]{0,18})$")
    private Long revision;

    private Boolean enabled;

    private Integer scoreThreshold;

    private Integer minSamples;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", format = "duration", example = "PT24H")
    private Duration evaluationWindow;

    private Integer maxIterations;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", format = "duration", example = "PT1H")
    private Duration cooldown;

    private Boolean autoPublish;

    private Boolean judgeEnabled;

    private String optimizerModelAlias;

    private String judgeModelAlias;

    private String optimizerInstruction;

    @Schema(allowableValues = {"ARMED", "AWAITING_PUBLISH", "CONVERGED", "EXHAUSTED", "PAUSED"})
    private String state;

    private Integer iterationsDone;

    private Integer consecutiveFailures;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", nullable = true, pattern = "^[1-9][0-9]{0,18}$")
    private Long activeRunId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", format = "date-time", nullable = true)
    private Instant convergedAt;

    private EvaluationResult evaluation;

    /** 最近一次评估快照与实时待评分数。 / Last evaluation snapshot plus the live pending-judge count. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class EvaluationResult {

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(type = "string", format = "date-time", nullable = true)
        private Instant evaluatedAt;

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(type = "string", nullable = true, pattern = "^[1-9][0-9]{0,18}$")
        private Long versionId;

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(type = "string", nullable = true, pattern = "^[1-9][0-9]{0,18}$")
        private Long versionNo;

        private Integer sampleCount;

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(type = "string", nullable = true, example = "71.50")
        private BigDecimal averageScore;

        private Integer pendingJudgeCount;
    }
}
