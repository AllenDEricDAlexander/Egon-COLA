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

import java.time.Instant;

/** API-017 使用样本视图。 / API-017 usage-sample view. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class DdcAiUsageFeedbackResult {

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", pattern = "^[1-9][0-9]{0,18}$")
    private Long id;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", pattern = "^[1-9][0-9]{0,18}$")
    private Long resourceId;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", pattern = "^[1-9][0-9]{0,18}$")
    private Long versionId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", nullable = true, pattern = "^[1-9][0-9]{0,18}$")
    private Long versionNo;

    private String usageId;

    private String reporterCredentialId;

    private String bizCode;

    private String env;

    private String appCode;

    @Schema(allowableValues = {"SUCCESS", "FAILURE", "UNKNOWN"})
    private String outcome;

    private Integer explicitScore;

    private Integer judgeScore;

    private Integer effectiveScore;

    @Schema(allowableValues = {"EXPLICIT", "JUDGE", "NONE"})
    private String scoreSource;

    @Schema(allowableValues = {"NOT_REQUIRED", "PENDING", "JUDGING", "DONE", "FAILED"})
    private String judgeState;

    private Integer judgeAttempts;

    private String judgeError;

    private String inputExcerpt;

    private String outputExcerpt;

    private Integer latencyMs;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", format = "date-time")
    private Instant occurredAt;

    private String traceId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", format = "date-time", nullable = true)
    private Instant judgedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", format = "date-time")
    private Instant createdAt;
}
